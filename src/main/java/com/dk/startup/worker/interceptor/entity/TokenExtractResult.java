package com.dk.startup.worker.interceptor.entity;

import lombok.Data;

@Data
public class TokenExtractResult {
    /**
     * code，
     * 1 表示 Token 解码成功；
     * 0 表示 Token 已过期;
     * -1 表示Token解码失败
     */
    private Integer code;

    private Long userId;

    private String userName;
}
