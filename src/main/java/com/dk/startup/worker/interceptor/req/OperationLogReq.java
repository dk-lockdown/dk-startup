package com.dk.startup.worker.interceptor.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class OperationLogReq {

    @ApiModelProperty("操作人id")
    private Long operatorId;


    @ApiModelProperty("操作人名称")
    private String operatorName;


    @ApiModelProperty("操作名称")
    private String operationName;


    @ApiModelProperty("调用方法名称")
    private String actionName;


    @ApiModelProperty("传入参数")
    private String parameters;


    @ApiModelProperty("调用结果")
    private String result;
}
