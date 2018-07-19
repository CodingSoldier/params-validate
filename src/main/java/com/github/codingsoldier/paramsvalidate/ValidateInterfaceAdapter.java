package com.github.codingsoldier.paramsvalidate;

import com.github.codingsoldier.paramsvalidate.bean.Parser;
import com.github.codingsoldier.paramsvalidate.bean.ValidateConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * author chenpiqian 2018-05-25
 */
public abstract class ValidateInterfaceAdapter implements ValidateInterface{

    public Parser getParser() {
        return null;
    }

    @Override
    public Map<String, Object> getCache(ValidateConfig validateConfig) {
        return new HashMap<>();
    }

    @Override
    public void setCache(ValidateConfig validateConfig, Map<String, Object> json) {

    }

}
