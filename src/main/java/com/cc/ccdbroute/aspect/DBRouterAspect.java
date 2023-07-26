package com.cc.ccdbroute.aspect;

import com.cc.ccdbroute.annotation.DBRouter;
import com.cc.ccdbroute.annotation.DBRouterStrategy;
import com.cc.ccdbroute.dbroute.DBRouterConfig;
import com.cc.ccdbroute.strategy.IDBRouterStrategy;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.lang.reflect.Method;

@Aspect
public class DBRouterAspect {
    private Logger logger = LoggerFactory.getLogger(DBRouterAspect.class);

    private DBRouterConfig dbRouterConfig;

    private IDBRouterStrategy dbRouterStrategy;

    public DBRouterAspect( DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
        this.dbRouterConfig = dbRouterConfig;
        this.dbRouterStrategy = dbRouterStrategy;
    }

    @Pointcut("@annotation(com.cc.ccdbroute.annotation.DBRouter)")
    public void aopPoint() {

    }
    /*
    * ProceedingJoinPoint 是 Spring AOP 提供的一个切点对象，用于表示正在执行的连接点（即被拦截的方法）。
    * */

    @Around("aopPoint() && @annotation(dbRouter)")
    public Object doRouter(ProceedingJoinPoint joinPoint, DBRouter dbRouter) throws Throwable{
        String dbKey = dbRouter.key();

       if(StringUtils.isBlank(dbKey) && StringUtils.isBlank(dbRouterConfig.getRouterKey())) {
            throw new RuntimeException("annotation DBRouter key is null");
       }

       dbKey = StringUtils.isNotBlank(dbKey) ? dbKey :dbRouterConfig.getRouterKey();

        String dbKeyAttr = getAttrValue(dbKey, joinPoint.getArgs());


        dbRouterStrategy.doRouter(dbKeyAttr);

        try {
            return joinPoint.proceed();
        } finally {
            dbRouterStrategy.clear();
        }
    }


    public Method getMethod(JoinPoint joinPoint) throws NoSuchMethodException{
        Signature sig = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) sig;
        return joinPoint.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
    }

    public String getAttrValue(String attr, Object[] args) {
        if(1 == args.length) {
            Object arg = args[0];
            if(arg instanceof String) {
                return arg.toString();
            }
        }

        String filedValue = null;
        for(Object arg : args) {
            try {
                if(StringUtils.isNotBlank(filedValue)) {
                    break;
                }
                filedValue = BeanUtils.getProperty(arg, attr);
            } catch (Exception e) {
                logger.error("获取路由属性值失败 attr：{}", attr, e);
            }
        }
        return filedValue;
    }
}
