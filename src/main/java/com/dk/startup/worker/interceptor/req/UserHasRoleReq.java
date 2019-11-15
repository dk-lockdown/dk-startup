package com.dk.startup.worker.interceptor.req;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class UserHasRoleReq implements Serializable {
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @NotNull(message = "角色名称不能为空")
    private String roleName;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}