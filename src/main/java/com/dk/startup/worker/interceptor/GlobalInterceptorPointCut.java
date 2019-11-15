package com.dk.startup.worker.interceptor;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class GlobalInterceptorPointCut extends AbstractPointcutAdvisor {
    @Resource
    private GlobalInterceptor globalInterceptor;

    private final Pointcut pointcut = new AspectJExpressionPointcut() {
        @Override
        public String getExpression() {
            return "execution(public * com..controller..*.*(..))";
        }
    };

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.globalInterceptor;
    }
}
