package com.github.lybgeek.exception.util;

import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 错误信息处理类。
 *
 */
public class ExceptionUtil {
    /**
     * 获取exception的详细错误信息。
     */
    public static String getExceptionMessage(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    public static String getRootErrorMessage(Exception e) {
        Throwable root = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
        root = (root == null ? e : root);
        if (root == null) {
            return "";
        }
        String msg = root.getMessage();
        if (msg == null) {
            return "null";
        }
        return StringUtils.defaultString(msg);
    }

    public static String getStackTraceAsString(Throwable e) {
        if (e == null) {
            return"";
        }
        // 用StringWriter和PrintWriter来获取堆栈信息
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            return sw.toString();
        } catch (IOException ex) {
            return"获取堆栈信息失败";
        }
    }
}
