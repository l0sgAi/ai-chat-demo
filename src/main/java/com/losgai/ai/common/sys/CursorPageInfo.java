package com.losgai.ai.common.sys;

import lombok.Data;

import java.util.List;
/**
 * 游标分页信息封装
 * */
@Data
public class CursorPageInfo<T> {
    // 数据列表
    private List<T> list;
    // 记录总数
    private long total;

    public CursorPageInfo(List<T> list, long total) {
        this.list = list;
        this.total = total;
    }
}
