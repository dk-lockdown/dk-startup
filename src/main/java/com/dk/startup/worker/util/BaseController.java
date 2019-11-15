package com.dk.startup.worker.util;

import com.dk.foundation.engine.baseentity.StandResponse;
import com.dk.startup.worker.constant.JwtTokenConstant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * @author scott lewis
 */
public class BaseController implements InitializingBean {

    final static Logger logger = LoggerFactory.getLogger(BaseController.class);

    public BaseController() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    public <E> StandResponse<E> success() {
        return StandResponseBuilder.ok();
    }


    public <E> StandResponse<E> success(E data) {
        return StandResponseBuilder.ok(data);
    }


    public <E> StandResponse<E> fail() {
        return StandResponseBuilder.result(StandResponse.INTERNAL_SERVER_ERROR,"系统错误");
    }

    public <E> StandResponse<E> fail(Integer code,String message) {
        return StandResponseBuilder.result(code,message);
    }

    public Long getUserId(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        Object userIdObj = request.getAttribute(JwtTokenConstant.CLAIM_USER_ID);
        String userId = "";
        if(userIdObj!=null) {
            userId = userIdObj.toString();
        }
        if(StringUtils.isNotEmpty(userId)){
            return Long.parseLong(userId);
        }
        return null;
    }

    public String getUserName(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        Object userNameObj = request.getAttribute(JwtTokenConstant.CLAIM_USER_NAME).toString();
        String userName = "";
        if(userNameObj!=null) {
            userName = userNameObj.toString();
        }
        return userName;
    }


    @org.springframework.web.bind.annotation.ExceptionHandler({ConstraintViolationException.class})
    public @ResponseBody
    StandResponse exception(ConstraintViolationException e, HttpServletRequest request, HttpServletResponse response) {
        e.printStackTrace();
        logger.error("#######ERROR#######", e);
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "OPTIONS,GET,POST");
        return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,((ConstraintViolation)(((ConstraintViolationException) e).getConstraintViolations().toArray()[0])).getMessage());
    }
}
