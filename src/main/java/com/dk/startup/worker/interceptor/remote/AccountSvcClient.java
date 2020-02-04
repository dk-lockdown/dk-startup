package com.dk.startup.worker.interceptor.remote;

import com.dk.foundation.engine.baseentity.StandResponse;
import com.dk.startup.worker.interceptor.entity.TokenExtractResult;
import com.dk.startup.worker.interceptor.req.OperationLogReq;
import com.dk.startup.worker.interceptor.req.UserHasPermissionReq;
import com.dk.startup.worker.interceptor.req.UserHasRoleReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * @author scott lewis
 */
@FeignClient(name = "per-ope-svc", url = "${dk.account.svc.host}")
public interface AccountSvcClient {
    @RequestMapping(value = "/v1/account/tokenExtract", method = RequestMethod.GET)
    @ResponseBody StandResponse<TokenExtractResult> tokenExtract();

    @RequestMapping(value = "/v1/role/ifUserHasRole", method = RequestMethod.POST)
    @ResponseBody
    StandResponse<Boolean> ifUserHasRole(@RequestBody UserHasRoleReq req);

    @RequestMapping(value = "/v1/permission/ifUserHasPermission", method = RequestMethod.POST)
    @ResponseBody
    StandResponse<Boolean> ifUserHasPermission(@RequestBody UserHasPermissionReq req);

    @RequestMapping(value = "/v1/operationlog/create", method = RequestMethod.POST)
    @ResponseBody
    StandResponse<Long> createOperationLog(@RequestBody OperationLogReq req);

    @RequestMapping(value = "/v1/app/signatureVerify", method = RequestMethod.POST)
    @ResponseBody
    StandResponse<Boolean> signatureVerify(@RequestBody String content);
}
