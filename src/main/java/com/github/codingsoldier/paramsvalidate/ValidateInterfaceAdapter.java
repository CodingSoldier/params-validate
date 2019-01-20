package com.github.codingsoldier.paramsvalidate;


import com.github.codingsoldier.paramsvalidate.bean.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author chenpiqian 2018-05-25
 */
public abstract class ValidateInterfaceAdapter implements ValidateInterface{

    /**
     * 默认根目录是resources/validate，json文件都放在此目录中
     * 可在ValidateInterfaceAdapter子类覆盖此方法，改变默认根目录
     */
    @Override
    public String basePath() {
        return "validate/";
    }

    /**
     * 校验级别
     * PvConst.LEVEL_STRICT  严格模式，发生异常，校验不通过，默认
     * PvConst.LEVEL_LOOSE   宽松模式，发生异常，不校验
     *
     * params-validate读取json文件或者son文件编写不合法等导致params-validate发生异常，默认是会拦截请求，不执行controller方法
     * 若在ValidateInterfaceAdapter子类覆盖此方法，返回PvConst.LEVEL_LOOSE，当params-validate校验发生异常，就不校验了，放行请求，执行controller方法。
     * 注意：当level设置为PvConst.LEVEL_LOOSE，在params-validate代码未发生异常的前提下，请求参数不符合校验规则，不会放行请求，params-validate返回校验信息给前端，controller方法不会执行。
     */
    @Override
    public String getLevel(){
        return PvConst.LEVEL_STRICT;
    }

    /**
     * json解析器
     * 1、使用默认解析器jackson，返回null
     * 2、使用gson，请返回 new Parser(Gson.class)。需要引入gson依赖
     * 3、使用fastjson，请返回new Parser(JSON.class, Feature[].class)。需要引入fastjson依赖
     * 提供对gson、fastjson的支持是因为jackson不支持在json文件中写注释。为了支持fastjson，搞得好坑爹。
     */
    @Override
    public Parser getParser() {
        return null;
    }

    @Override
    public Object validateNotPass(ResultValidate resultValidate){
        List<Map<String, String>> msgList = resultValidate.getMsgList();
        Map<String, String> data = new HashMap<>();  //错误信息集合
        for (Map<String, String> elemMap:msgList){
            if (elemMap != null){
                Boolean requestVal = Boolean.parseBoolean(elemMap.get(PvConst.REQUEST));
                String minVal = elemMap.get(PvConst.MIN_VALUE);
                String maxVal = elemMap.get(PvConst.MAX_VALUE);
                String minLen = elemMap.get(PvConst.MIN_LENGTH);
                String maxLen = elemMap.get(PvConst.MAX_LENGTH);
                String jsonMsg = elemMap.get(PvConst.MESSAGE);

                String message = "";
                message = PvUtil.isNotBlankObj(jsonMsg) ? (message+jsonMsg+"，") : message;
                message = Boolean.TRUE.equals(requestVal) ? (message+"必填，") : message;
                message = PvUtil.isNotBlankObj(minVal) ? (message+"最小值"+minVal+"，") : message;
                message = PvUtil.isNotBlankObj(maxVal) ? (message+"最大值"+maxVal+"，") : message;
                message = PvUtil.isNotBlankObj(minLen) ? (message+"最小长度"+Float.valueOf(minLen).intValue()+"，") : message;
                message = PvUtil.isNotBlankObj(maxLen) ? (message+"最大长度"+Float.valueOf(maxLen).intValue()+"，") : message;
                message = "".equals(message) ? "未通过校验，" : message;
                message = message.substring(0, message.length()-1);

                String name = elemMap.get(PvConst.NAME);
                data.put(name, message);
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("code", resultValidate.isPass() ? 0 : 101);
        r.put("data", data);
        return r;
    }

    @Override
    public Map<String, Object> getKeyCache(ValidateConfig validateConfig) {
        return new HashMap<>();
    }

    @Override
    public void setFileCache(ValidateConfig validateConfig, Map<String, Map<String, Object>> json) {

    }

}
