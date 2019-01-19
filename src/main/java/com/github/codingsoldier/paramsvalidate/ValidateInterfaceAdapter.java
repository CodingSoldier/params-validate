package com.github.codingsoldier.paramsvalidate;


import com.github.codingsoldier.paramsvalidate.bean.*;

import java.util.HashMap;
import java.util.List;
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
    public String getLevel(){
        return PvConst.LEVEL_STRICT;
    }

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
                message = PvUtil.isNotBlankObj(minLen) ? (message+"最小长度"+minLen+"，") : message;
                message = PvUtil.isNotBlankObj(maxLen) ? (message+"最大长度"+maxLen+"，") : message;
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
