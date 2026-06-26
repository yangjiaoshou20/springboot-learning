package com.github.lybgeek.upload.service.impl;

import com.github.lybgeek.common.exception.BizException;
import com.github.lybgeek.common.util.DateUtil;
import com.github.lybgeek.common.util.RedisUtil;
import com.github.lybgeek.common.util.YmlUtil;
import com.github.lybgeek.upload.concurrent.FileCallable;
import com.github.lybgeek.upload.constant.FileConstant;
import com.github.lybgeek.upload.dto.FileUploadDTO;
import com.github.lybgeek.upload.dto.FileUploadRequestDTO;
import com.github.lybgeek.upload.enu.FileCheckMd5Status;
import com.github.lybgeek.upload.service.FileService;
import com.github.lybgeek.upload.strategy.enu.UploadModeEnum;
import com.github.lybgeek.upload.util.FileMD5Util;
import com.github.lybgeek.upload.util.FilePathUtil;
import com.github.lybgeek.upload.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private FilePathUtil filePathUtil;

    @Value("${upload.thread.maxSize}")
    private Integer uploadThreadMaxSize;

    @Value("${upload.queue.maxSize}")
    private Integer uploadQueueMaxSize;


    private final AtomicInteger atomicInteger = new AtomicInteger(0);


    private final ExecutorService executorService = Executors.newFixedThreadPool(
            uploadThreadMaxSize, (r) -> {
                String threadName = "uploadPool-" + atomicInteger.getAndIncrement();
                Thread thread = new Thread(r);
                thread.setName(threadName);
                return thread;
            });

    private final CompletionService<FileUploadDTO> completionService =
            new ExecutorCompletionService<>(executorService, new LinkedBlockingDeque<>(uploadQueueMaxSize));


    @Override
    public FileUploadDTO upload(FileUploadRequestDTO param) throws IOException {
        if (Objects.isNull(param.getFile())) {
            throw new BizException("file can not be empty", 404);
        }
        param.setPath(FileUtil.withoutHeadAndTailDiagonal(param.getPath()));
        String md5 = FileMD5Util.getFileMD5(param.getFile());
        param.setMd5(md5);

        String filePath = filePathUtil.getPath(param);
        File targetFile = new File(filePath);
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }
        String path = filePath + FileConstant.FILE_SEPARATORCHAR + param.getFile().getOriginalFilename();
        FileOutputStream out = new FileOutputStream(path);
        out.write(param.getFile().getBytes());
        out.flush();
        FileUtil.close(out);

        redisUtil.hset(FileConstant.FILE_UPLOAD_STATUS, md5, "true");

        return FileUploadDTO.builder().path(path).mtime(DateUtil.getCurrentTimeStamp()).uploadComplete(true).build();
    }

    @Override
    public FileUploadDTO sliceUpload(FileUploadRequestDTO fileUploadRequestDTO) {
        try {
            completionService.submit(new FileCallable(UploadModeEnum.RANDOM_ACCESS, fileUploadRequestDTO));
            return completionService.take().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new BizException(e.getMessage(), 406);
        }
    }

    @Override
    public FileUploadDTO checkFileMd5(FileUploadRequestDTO param) throws IOException {
        Object uploadProgressObj = redisUtil.hget(FileConstant.FILE_UPLOAD_STATUS, param.getMd5());
        if (uploadProgressObj == null) {
            return FileUploadDTO.builder()
                    .code(FileCheckMd5Status.FILE_NO_UPLOAD.getValue()).build();
        }
        String processingStr = uploadProgressObj.toString();
        boolean processing = Boolean.parseBoolean(processingStr);
        String value = String.valueOf(redisUtil.get(FileConstant.FILE_MD5_KEY + param.getMd5()));
        return fillFileUploadDTO(param, processing, value);
    }

    /**
     * 填充返回文件内容信息
     */
    private FileUploadDTO fillFileUploadDTO(FileUploadRequestDTO param, boolean processing, String value) throws IOException {
        if (processing) {
            // 1. 规范化路径：去掉路径头尾的斜杠（例如 "/upload/path/" 变成 "upload/path"）
            param.setPath(FileUtil.withoutHeadAndTailDiagonal(param.getPath()));
            // 2. 构建最终的文件存储路径
            String path = filePathUtil.getPath(param);
            // 3. 返回“文件已上传”的状态，并带上文件路径
            // 前端收到后就知道不需要再传文件了，直接使用这个路径即可
            return FileUploadDTO.builder().code(FileCheckMd5Status.FILE_UPLOADED.getValue())
                    .path(path).build();
        } else {
            // 1. 获取记录进度的配置文件
            // value 是这个 conf 文件的路径，里面记录了哪些分片已经上传了
            java.io.File confFile = new java.io.File(value);
            // 2. 读取配置文件的内容到一个字节数组 (byte数组)
            byte[] completeList = FileUtils.readFileToByteArray(confFile);
            // 3. 遍历字节数组，找出哪些分片还没上传
            List<Integer> missChunkList = new LinkedList<>();
            for (int i = 0; i < completeList.length; i++) {
                // 核心判断：如果该位置的值不等于 Byte.MAX_VALUE (即127)，说明该分片未上传或未完成
                if (completeList[i] != Byte.MAX_VALUE) {
                    // 记录缺失的分片索引
                    missChunkList.add(i);
                }
            }
            // 4. 返回“部分上传”的状态，并把缺失的分片索引列表带回
            // 前端收到后，会只上传 missChunkList 里的这些分片
            return FileUploadDTO.builder().code(FileCheckMd5Status.FILE_UPLOAD_SOME.getValue())
                    .missChunks(missChunkList).build();
        }
    }
}
