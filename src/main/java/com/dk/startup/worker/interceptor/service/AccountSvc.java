package com.dk.startup.worker.interceptor.service;

import com.dk.startup.worker.interceptor.entity.TokenExtractResult;
import com.dk.startup.worker.interceptor.req.OperationLogReq;

public interface AccountSvc {
    /**
     * 验证并解码Token
     * @param token
     * @return
     */
    TokenExtractResult tokenExtract(String token);

    /**
     * 是否有相应角色
     * @param userId
     * @param roleName
     * @return
     */
    Boolean ifUserHasRole(Long userId,String roleName);

    /**
     * 是否有权限
     * @param userId
     * @param permissionKey
     * @return
     */
    Boolean ifUserHasPermission(Long userId, String permissionKey);

    /**
     * 创建操作日志
     * @param req
     * @return
     */
    Long createOperationLog(OperationLogReq req);

    /**
     * 签名校验
     * @param appid
     * @param content
     * @param signature
     * @return
     */
    Boolean signatureVerify(String appid, String content, String signature);
}
