package com.sub2.monitor.common.api;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private long total;
    private long pageNo;
    private long pageSize;
    private List<T> records;
}
