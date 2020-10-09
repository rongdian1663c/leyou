package com.leyou.common.exception;

import com.leyou.common.enums.FileExceptionEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
public class LyException extends RuntimeException{

    /*响应状态对象*/
    private HttpStatus status;

    /* 响应状态码*/
    private int statusCode;

    public LyException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public LyException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public LyException(FileExceptionEnum fileEnum){
        super(fileEnum.getMsg());
        this.statusCode = fileEnum.getCode();
    }
}