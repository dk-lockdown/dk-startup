package com.dk.startup.worker.interceptor;

import com.alibaba.fastjson.JSON;
import com.dk.foundation.engine.baseentity.StandResponse;
import com.dk.foundation.engine.springcontext.SpringContextHolder;
import com.dk.startup.worker.constant.JwtTokenConstant;
import com.dk.startup.worker.interceptor.annotation.*;
import com.dk.startup.worker.interceptor.entity.TokenExtractResult;
import com.dk.startup.worker.interceptor.req.OperationLogReq;
import com.dk.startup.worker.interceptor.req.UserHasPermissionReq;
import com.dk.startup.worker.interceptor.req.UserHasRoleReq;
import com.dk.startup.worker.interceptor.remote.AccountSvcClient;
import com.dk.startup.worker.interceptor.service.AccountSvc;
import com.dk.startup.worker.util.StandResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author scott lewis
 */
@Slf4j
@Component
public class GlobalInterceptor implements MethodInterceptor, ApplicationListener<ApplicationStartedEvent> {
    private static final String NOT_ALLOWED="not allowed";
    private static final String ALLOWED="allowed";
    private static final String VERIFY_PERMISSION_FAIL="verify permission fail";

    @Value("${dk.permissionCheck.enable:false}")
    private Boolean permissionCheck;

    @Value("${dk.signatureVerify.enable:false}")
    private Boolean signatureVerify;

    @Resource
    AccountSvcClient accountSvcClient;

    private AccountSvc accountSvc;

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        if (mi.getMethod().isAnnotationPresent(Login.class)
                || mi.getMethod().isAnnotationPresent(HasRole.class)
                || mi.getMethod().isAnnotationPresent(Permission.class)) {
            return processLogin(request,mi);
        }

        if (mi.getMethod().isAnnotationPresent(SignatureVerify.class)){
            if(signatureVerify) {
                Object res = processSignatureVerify(request,mi);
                if (res!=null) {
                    return res;
                }
            }
        }

        Object result = mi.proceed();
        processOperationLog(request,mi,result);
        return result;
    }

    private Object processLogin(HttpServletRequest request, MethodInvocation mi) throws Throwable {
        String token = request.getHeader("Authorization");
        if(StringUtils.isEmpty(token)){
            return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,"请登录后再进行相应操作");
        }

        /**校验并提取 Token 中的用户信息**/
        Object res = processTokenExtract(token, request);
        if (res!=null) {
            return res;
        }

        Object result = null;

        String checkResult = ALLOWED;
        if(permissionCheck){
            if (mi.getMethod().isAnnotationPresent(HasRole.class)) {
                checkResult = processHasRole(request, mi);
            }

            String verifyResult = ALLOWED;
            switch (checkResult){
                case ALLOWED:
                    if (mi.getMethod().isAnnotationPresent(HasRole.class)) {
                        /**具有相关角色则直接具有操作权限**/
                        result = mi.proceed();
                    }
                    else {
                        /**没有验证角色，验证是否具有权限**/
                        if (mi.getMethod().isAnnotationPresent(Permission.class)) {
                            verifyResult = processPermission(request, mi);
                        }
                    }
                    break;
                case NOT_ALLOWED:
                    /**没有相关角色，继续验证是否具有权限**/
                    if (mi.getMethod().isAnnotationPresent(Permission.class)) {
                        verifyResult = processPermission(request, mi);
                    }
                    break;
                case VERIFY_PERMISSION_FAIL:
                    return StandResponseBuilder.result(StandResponse.INTERNAL_SERVER_ERROR, "校验用户权限失败");
                default:
                    break;
            }

            switch (verifyResult){
                case ALLOWED:
                    result = mi.proceed();
                    break;
                case NOT_ALLOWED:
                    return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,"该用户不具有相应的权限");
                case VERIFY_PERMISSION_FAIL:
                    return StandResponseBuilder.result(StandResponse.INTERNAL_SERVER_ERROR, "校验用户权限失败");
                default:
                    break;
            }
        } else {
            result = mi.proceed();
        }

        processOperationLog(request,mi,result);
        return result;
    }

    private Object processTokenExtract(String token, HttpServletRequest request) {
       if (accountSvc != null) {
            TokenExtractResult result = accountSvc.tokenExtract(token);
            if(result.getCode()==1){
                request.setAttribute(JwtTokenConstant.CLAIM_USER_ID, result.getUserId());
                request.setAttribute(JwtTokenConstant.CLAIM_USER_NAME, result.getUserName());
                log.info("设置操作用户的id为{},用户名为{}",result.getUserId(),result.getUserName());
            } else if(result.getCode()==0){
                return StandResponseBuilder.result(HttpStatus.UNAUTHORIZED.value(), "您的用户凭证已过期，请重新登录");
            } else if(result.getCode()==-1){
                return StandResponseBuilder.result(HttpStatus.INTERNAL_SERVER_ERROR.value(), "校验用户token失败");
            }
            return null;
        } else {
            try {
                StandResponse<TokenExtractResult> result = accountSvcClient.tokenExtract();
                if (!result.getSuccess()) {
                    return StandResponseBuilder.result(HttpStatus.INTERNAL_SERVER_ERROR.value(), "校验用户token失败");
                } else if (result.getData().getCode()==1) {
                    request.setAttribute(JwtTokenConstant.CLAIM_USER_ID, result.getData().getUserId());
                    request.setAttribute(JwtTokenConstant.CLAIM_USER_NAME, result.getData().getUserName());
                    log.info("设置操作用户的id为{},用户名为{}",result.getData().getUserId(),result.getData().getUserName());
                } else if (result.getData().getCode()==0) {
                    return StandResponseBuilder.result(HttpStatus.UNAUTHORIZED.value(), "您的用户凭证已过期，请重新登录");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error(ex.getMessage());
                return StandResponseBuilder.result(HttpStatus.INTERNAL_SERVER_ERROR.value(), "校验用户token失败");
            }
            return null;
        }
    }

    private String processHasRole(HttpServletRequest request, MethodInvocation mi) throws Throwable {
        HasRole hasRole = mi.getMethod().getAnnotation(HasRole.class);
        if(StringUtils.isNotEmpty(hasRole.roleName())){
            UserHasRoleReq req = new UserHasRoleReq();
            Long userId = Long.parseLong(request.getAttribute(JwtTokenConstant.CLAIM_USER_ID).toString());
            req.setUserId(userId);
            req.setRoleName(hasRole.roleName());

            if( accountSvc!=null ){
                Boolean result = accountSvc.ifUserHasRole(userId,hasRole.roleName());
                return result ? ALLOWED : NOT_ALLOWED;
            } else {
                try {
                    StandResponse<Boolean> result = accountSvcClient.ifUserHasRole(req);
                    return result.getSuccess() && result.getData() ? ALLOWED : NOT_ALLOWED;
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    log.error("校验用户是否具有相应角色失败{}", req.toString());
                    return VERIFY_PERMISSION_FAIL;
                }
            }
        }
        return ALLOWED;
    }

    private String processPermission(HttpServletRequest request, MethodInvocation mi) throws Throwable {
        Permission permission = mi.getMethod().getAnnotation(Permission.class);
        if(StringUtils.isNotEmpty(permission.permissionKey())){
            UserHasPermissionReq req = new UserHasPermissionReq();
            Long userId = Long.parseLong(request.getAttribute(JwtTokenConstant.CLAIM_USER_ID).toString());
            req.setUserId(userId);
            req.setPermissionKey(permission.permissionKey());
            if( accountSvc!=null ){
                Boolean result = accountSvc.ifUserHasPermission(userId,permission.permissionKey());
                return result ? ALLOWED : NOT_ALLOWED;
            } else {
                try {
                    StandResponse<Boolean> result = accountSvcClient.ifUserHasPermission(req);
                    return result.getSuccess() && result.getData() ? ALLOWED : NOT_ALLOWED;
                }
                catch (Throwable throwable){
                    throwable.printStackTrace();
                    log.error("校验用户是否具有相应角色失败{}",req.toString());
                    return VERIFY_PERMISSION_FAIL;
                }
            }
        }
        return ALLOWED;
    }

    private void processOperationLog(HttpServletRequest request, MethodInvocation mi, Object result) {
        /* 插入操作日志 */
        if(mi.getMethod().isAnnotationPresent(OperationLog.class)) {

            OperationLog anno = mi.getMethod().getAnnotation(OperationLog.class);
            OperationLogReq req = new OperationLogReq();
            req.setOperationName(anno.name());
            req.setActionName(mi.getMethod().getDeclaringClass().getName()+"."+ mi.getMethod().getName());

            Object userIdObj = request.getAttribute(JwtTokenConstant.CLAIM_USER_ID);
            if(userIdObj==null) {
                userIdObj = 0L;
            }
            req.setOperatorId(Long.parseLong(userIdObj.toString()));

            Object userNameObj = request.getAttribute(JwtTokenConstant.CLAIM_USER_ID);
            if(userNameObj==null) {
                userNameObj = "system";
            }
            req.setOperatorName(userNameObj.toString());

            Object[] params = mi.getArguments();
            if(params!=null) {
                req.setParameters(JSON.toJSONString(params));
            }
            if(result!=null) {
                req.setResult(result.toString());
            }


            if( accountSvc!=null ){
                accountSvc.createOperationLog(req);
            } else {
                try {
                    accountSvcClient.createOperationLog(req);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    log.error("操作日志记录失败");
                }
            }
        }
    }

    private Object processSignatureVerify(HttpServletRequest request, MethodInvocation mi) throws Throwable {
        String appid = request.getHeader("dk-appid");
        String signature = request.getHeader("dk-sign");
        if(StringUtils.isEmpty(appid)){
            return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,"appid不能为空");
        }
        if(StringUtils.isEmpty(signature)){
            return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,"签名不能为空");
        }

        /* 获取参数 */
        Object[] args = mi.getMethod().getParameters();
        if (args == null || args.length == 0 || args[0] == null) {
            log.error("{}: 方法声明不存在参数", mi.getMethod().toString());
            return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,"参数不能为空");
        }
        String content = (String)args[0];

        if( accountSvc!=null ){
            Boolean result = accountSvc.signatureVerify(appid,content,signature);
            if(!result){
                return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,"签名校验失败");
            }
        } else {
            try {
                StandResponse<Boolean> response = accountSvcClient.signatureVerify(content);
                if (!response.getSuccess() || !response.getData()) {
                    return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,"签名校验失败");
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                log.error("签名校验失败");
                return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,"签名校验失败");
            }
        }
        return null;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        try {
            accountSvc = SpringContextHolder.getBean(AccountSvc.class);
        }
        catch (Exception ex) {
            log.warn("there is no bean named accountSvc");
        }
    }
}
