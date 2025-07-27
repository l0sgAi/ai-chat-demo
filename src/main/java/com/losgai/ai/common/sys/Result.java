package com.losgai.ai.common.sys;

import com.losgai.ai.enums.ResultCodeEnum;
import lombok.Getter;
import lombok.Setter;

/** 自定义返回体
 * */
@Getter
@Setter
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long count; // 返回总数

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCodeEnum.SUCCESS.getCode());
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(ResultCodeEnum.SERVICE_ERROR.getCode());
        result.setMessage(message);
        return result;
    }
    // 新增分页返回方法
    public static <T> Result<T> page(T data, long count) {
        Result<T> result = new Result<>();
        result.setCode(ResultCodeEnum.SUCCESS.getCode());
        result.setMessage("分页返回");
        result.setData(data);
        result.setCount(count);
        return result;
    }

}