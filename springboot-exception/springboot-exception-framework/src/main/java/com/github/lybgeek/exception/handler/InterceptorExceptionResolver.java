package com.github.lybgeek.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lybgeek.exception.BizException;
import com.github.lybgeek.resp.model.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 处理拦截器中的异常（实现HandlerExceptionResolver接口）
 */
@Slf4j
@Component
public class InterceptorExceptionResolver implements HandlerExceptionResolver {

    @Autowired
    private ObjectMapper objectMapper; // Spring默认的JSON解析器

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 设置响应格式为JSON
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        AjaxResult<Void> result;
        try {
            // 判断异常类型，和全局处理器逻辑一致
            if (ex instanceof BizException) {
                BizException businessException = (BizException) ex;
                result = AjaxResult.error(businessException.getMessage(), businessException.getErrorCode());
            } else if (ex instanceof IllegalArgumentException) {
                result = AjaxResult.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
            } else {
                result = AjaxResult.error("系统繁忙，请稍后再试", HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
            // 把Result对象转换成JSON字符串，写入响应
            String json = objectMapper.writeValueAsString(result);
            response.getWriter().write(json);
        } catch (Exception e) {
            log.error("处理拦截器异常失败：", e);
        }

        // 返回null，表示不需要跳转页面（因为我们已经返回JSON了）
        return new ModelAndView();
    }
}