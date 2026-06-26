package com.github.lybgeek.upload.concurrent;


import com.github.lybgeek.upload.dto.FileUploadDTO;
import com.github.lybgeek.upload.dto.FileUploadRequestDTO;
import com.github.lybgeek.upload.strategy.context.UploadContext;
import com.github.lybgeek.upload.strategy.enu.UploadModeEnum;
import java.util.concurrent.Callable;

public class FileCallable implements Callable<FileUploadDTO> {

    private final UploadModeEnum mode;

    private final FileUploadRequestDTO param;

    public FileCallable(UploadModeEnum mode,
                        FileUploadRequestDTO param) {
        this.mode = mode;
        this.param = param;
    }

    @Override
    public FileUploadDTO call() throws Exception {
        return UploadContext.INSTANCE.getInstance(mode).sliceUpload(param);
    }
}
