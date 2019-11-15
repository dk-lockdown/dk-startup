package com.dk.startup.worker.util;

import com.dk.foundation.engine.baseentity.StandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
public class ExceptionHandler {
    final static Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);


    @org.springframework.web.bind.annotation.ExceptionHandler({MethodArgumentNotValidException.class})
    public @ResponseBody
    StandResponse exception(MethodArgumentNotValidException e, HttpServletRequest request, HttpServletResponse response) {
        e.printStackTrace();
        logger.error("#######ERROR#######", e);
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "OPTIONS,GET,POST");
        return StandResponseBuilder.result(StandResponse.BUSINESS_EXCEPTION,e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }
}
