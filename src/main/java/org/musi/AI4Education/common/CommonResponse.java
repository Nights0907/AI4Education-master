package org.musi.AI4Education.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.io.Serializable;

@Getter
//JSON序列化时空的数据不被包含
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> implements Serializable
{

    private int status;
    private String msg;
    private T data;

    private CommonResponse(int status)
    {
        this.status = status;
    }

    private CommonResponse(int status, String msg)
    {
        this.status = status;
        this.msg = msg;
    }

    private CommonResponse(int status, String msg, T data)
    {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    private CommonResponse(int status, T data, T commentList)
    {
        this.status = status;
        this.data = data;
    }

    private CommonResponse(int status, T data)
    {
        this.status = status;
        this.data = data;
    }

    @JsonIgnore
    public boolean isSuccess()
    {
        return this.status == ResponseCode.SUCCESS.getCode();
    }

    public static <T> CommonResponse <T> creatForSuccess()
    {
        return new CommonResponse<T>(ResponseCode.SUCCESS.getCode());
    }

    public static <T> CommonResponse <T> creatForSuccess(T data)
    {
        return new CommonResponse<T>(ResponseCode.SUCCESS.getCode(), data);
    }

    public static <T> CommonResponse <T> creatForSuccessMessage(String msg)
    {
        return new CommonResponse<T>(ResponseCode.SUCCESS.getCode(), msg);
    }

    public static <T> CommonResponse <T> creatForSuccess(String msg, T data)
    {
        return new CommonResponse<T>(ResponseCode.SUCCESS.getCode(), msg, data);
    }

    public static <T> CommonResponse <T> creatForError()
    {
        return new CommonResponse<T>(ResponseCode.ERROR.getCode(), ResponseCode.ERROR.getDescription());
    }

    public static <T> CommonResponse <T> creatForError(String msg)
    {
        return new CommonResponse<T>(ResponseCode.ERROR.getCode(), msg);
    }

    public static <T> CommonResponse <T> creatForError(int code, String msg)
    {
        return new CommonResponse<T>(code, msg);
    }
}
