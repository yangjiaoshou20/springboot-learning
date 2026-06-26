package com.github.lybgeek.exception.handler;

import com.github.lybgeek.exception.BizException;
import com.github.lybgeek.exception.model.ValidMsg;
import com.github.lybgeek.exception.util.ExceptionUtil;
import com.github.lybgeek.resp.model.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionBaseHandler {


    /**
     * 开发环境：处理未知异常，返回完整堆栈信息（方便排查问题）
     */
    @Profile("dev")
    @ExceptionHandler(Exception.class)
    public AjaxResult<?> handleExceptionDev(Exception e) {
        log.error("发生未知异常：", e);
        // 拼接完整的堆栈信息
        String stackTrace = ExceptionUtil.getStackTraceAsString(e);
        // 返回错误码和堆栈信息（只在开发环境返回）
        return AjaxResult.error("开发环境-未知异常：" + stackTrace, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    /**
     * 生产环境：处理未知异常，只返回模糊提示（避免泄露敏感信息）
     */
    @Profile("prod")
    @ExceptionHandler(Exception.class)
    public AjaxResult<?> handleExceptionProd(Exception e) {
        log.error("发生未知异常：", e);
        // 只返回模糊提示
        return AjaxResult.error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

//    // 运行期异常
//    @ExceptionHandler(Exception.class)
//    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//    public AjaxResult<?> handleException(Exception e) {
//        String msg = e.getMessage();
//        if (StringUtils.isEmpty(msg)) {
//            msg = "服务端异常";
//        }
//        log.error(msg, e);
//        return AjaxResult.error(msg, HttpStatus.INTERNAL_SERVER_ERROR.value());
//    }


    @ExceptionHandler(BizException.class)
    public AjaxResult<?> handleException(BizException e) {
        return AjaxResult.error(e.getMessage(), e.getErrorCode());
    }


    /**
     * 参数验证失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult<?> handleException(ConstraintViolationException e) {
        log.error("参数验证失败", e);
        return AjaxResult.error("参数验证失败", HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 请求对象属性不满足校验规则
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult<?> handleException(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        return getValidateExceptionAjaxResult(e, result);
    }

    /**
     * 请求对象属性不满足校验规则
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult<?> handleException(BindException e) {
        BindingResult result = e.getBindingResult();
        return getValidateExceptionAjaxResult(e, result);
    }

    /**
     * 参数校验异常
     */
    private AjaxResult<List<ValidMsg>> getValidateExceptionAjaxResult(Exception e, BindingResult result) {
        final List<FieldError> fieldErrors = result.getFieldErrors();
        List<ValidMsg> errorList = new ArrayList<>();
        for (FieldError error : fieldErrors) {
            errorList.add(new ValidMsg(null, error.getField(), error.getDefaultMessage()));
        }
        log.error("请求对象属性不满足校验规则", e);
        return AjaxResult.error("请求对象属性不满足校验规则", HttpStatus.BAD_REQUEST.value(), null, errorList);
    }

    /**
     * 参数类型数据异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult<List<ValidMsg>> handleException(MethodArgumentTypeMismatchException e) {
        List<ValidMsg> errorList = new ArrayList<>();
        errorList.add(new ValidMsg("", e.getName(), e.getMessage()));
        log.error("参数类型数据异常", e);
        return AjaxResult.error("参数类型数据异常", HttpStatus.BAD_REQUEST.value(), null, errorList);
    }

    /**
     * 参数丢失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult<List<ValidMsg>> handleException(MissingServletRequestParameterException e) {
        List<ValidMsg> errorList = new ArrayList<>();
        errorList.add(new ValidMsg("", e.getParameterName(), e.getMessage()));
        log.error("参数丢失异常", e);
        return AjaxResult.error("参数丢失异常",
                HttpStatus.BAD_REQUEST.value(), null, errorList);
    }

    /**
     * 消息不可读异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult<?> handleException(HttpMessageNotReadableException e) {
        String msg = ExceptionUtil.getExceptionMessage(e);
        log.error(msg, e);
        return AjaxResult.error(msg,
                HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 处理404异常（接口不存在）
     * 注意：需要在application.yml中配置spring.mvc.throw-exception-if-no-handler-found=true
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public AjaxResult<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.error("访问的接口不存在：{} {}", e.getHttpMethod(), e.getRequestURL(), e);
        return AjaxResult.error("您访问的接口不存在，请检查URL是否正确", HttpStatus.NOT_FOUND.value());
    }

    /**
     * 处理405异常（请求方法不支持）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        // 获取支持的请求方法
        String supportedMethods = Arrays.toString(e.getSupportedMethods());
        log.error("请求方法不支持：当前方法={}，支持的方法={}", e.getMethod(), supportedMethods, e);
        return AjaxResult.error("请求方法不支持，支持的方法：" + supportedMethods, HttpStatus.METHOD_NOT_ALLOWED.value());
    }

}
