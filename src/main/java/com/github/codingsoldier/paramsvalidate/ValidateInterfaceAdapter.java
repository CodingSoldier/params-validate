package com.github.codingsoldier.paramsvalidate;


import com.github.codingsoldier.paramsvalidate.bean.Parser;
import com.github.codingsoldier.paramsvalidate.bean.ResultValidate;
import com.github.codingsoldier.paramsvalidate.bean.ValidateConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * author chenpiqian 2018-05-25
 */
public abstract class ValidateInterfaceAdapter implements ValidateInterface{

    @Override
    public String basePath() {
        return "validate/";
    }

    @Override
    public Parser getParser() {
        return null;
    }

    @Override
    public String getLevel(){
        return PvLevel.STRICT;
    }

    //必须覆盖此方法
    public abstract Object validateNotPass(ResultValidate resultValidate);

    @Override
    public Map<String, Object> getCache(ValidateConfig validateConfig) {
        return new HashMap<>();
    }

    @Override
    public void setCache(ValidateConfig validateConfig, Map<String, Object> json) {

    }

}
