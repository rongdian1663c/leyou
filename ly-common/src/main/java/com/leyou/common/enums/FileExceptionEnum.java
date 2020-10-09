package com.leyou.common.enums;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-17 10:49
 **/
public enum FileExceptionEnum {
    FILE_TYPE_ERROR(400, "文件类型错误"),
    FILE_CONTENT_ERROR(400, "文件内容受损"),
    FILE_UPLOAD_FAIL(500, "文件上传失败");

    private int code; // 异常状态码
    private String msg; // 异常说明

    FileExceptionEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
