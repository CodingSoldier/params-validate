package com.github.codingsoldier.paramsvalidate;

import com.github.codingsoldier.paramsvalidate.bean.PvConst;
import com.github.codingsoldier.paramsvalidate.bean.ResultValidate;
import com.github.codingsoldier.paramsvalidate.bean.ValidateConfig;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * author chenpiqian 2018-05-25
 */
@Aspect
@Component
public class ValidateAspect {

    @Autowired
    ValidateMain validateMain;
    @Autowired
    ValidateInterface validateInterface;

    @Pointcut("@annotation(com.github.codingsoldier.paramsvalidate.ParamsValidate)")
    public void aspect(){
        //切点
    }

    @Around("aspect()")
    public Object around(JoinPoint joinPoint) throws Throwable{
        Object obj;
        ValidateConfig validateConfig = getConfigs(joinPoint);

        //获取校验级别
        String level = PvUtil.isNotBlank(validateConfig.getLevel()) ? validateConfig.getLevel() : validateInterface.getLevel();
        level = PvConst.LEVEL_LOOSE.equals(level) ?  PvConst.LEVEL_LOOSE : PvConst.LEVEL_STRICT;

        ResultValidate resultValidate = new ResultValidate();
        if (PvConst.LEVEL_STRICT.equals(level)){
            resultValidate = validateMain.validateExecute(joinPoint, validateConfig);  //校验
        }else {
            try {
                resultValidate = validateMain.validateExecute(joinPoint, validateConfig);  //校验
            }catch (Exception e){
                resultValidate.setPass(true);  //PvLevel.LOOSE发生异常不校验
                //打印告警日志
                MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                Method method = methodSignature.getMethod();
                PvUtil.logWarning("校验发生异常，校验级别为[PvLevel.LOOSE]，不校验", method, e);
            }
        }
        if (resultValidate.isPass()){  //校验通过
            obj = ((ProceedingJoinPoint) joinPoint).proceed();
        }else {  //校验未通过
            obj = validateInterface.validateNotPass(resultValidate);
        }
        return obj;
    }

    //获取校验注解@ParamsValidate设置的值
    private static ValidateConfig getConfigs(JoinPoint joinPoint){
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();  //获取当前方法
        ValidateConfig validateConfig = new ValidateConfig();
        String file = method.getAnnotation(ParamsValidate.class).value();
        file = PvUtil.isNotBlank(file) ? file : method.getAnnotation(ParamsValidate.class).file();
        String key = method.getAnnotation(ParamsValidate.class).key();
        String level = method.getAnnotation(ParamsValidate.class).level();
        validateConfig.setFile(file);
        validateConfig.setKey(key);
        validateConfig.setLevel(level);
        return validateConfig;
    }

}
