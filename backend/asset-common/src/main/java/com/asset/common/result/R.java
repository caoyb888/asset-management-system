package com.asset.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一API返回结果
 */
@Data
public class R<T> implements Serializable {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok() { return restResult(null, 200, "success"); }
    public static <T> R<T> ok(T data) { return restResult(data, 200, "success"); }
    public static <T> R<T> fail(String msg) { return restResult(null, 500, msg); }
    public static <T> R<T> fail(int code, String msg) { return restResult(null, code, msg); }

    private static <T> R<T> restResult(T data, int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
