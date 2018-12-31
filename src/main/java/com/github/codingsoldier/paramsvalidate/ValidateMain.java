package com.github.codingsoldier.paramsvalidate;

import com.github.codingsoldier.paramsvalidate.bean.PvMsg;
import com.github.codingsoldier.paramsvalidate.bean.ResultValidate;
import com.github.codingsoldier.paramsvalidate.bean.ValidateConfig;
import org.aspectj.lang.JoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/**
 * author chenpiqian 2018-05-25
 */
@Component
public class ValidateMain {

    public static final String REGEX_BEGIN = "REGEX_";

    private static Set<String> ruleKeySet = new HashSet<>();
    static {
        ruleKeySet.add(PvMsg.REQUEST);
        ruleKeySet.add(PvMsg.MIN_VALUE);
        ruleKeySet.add(PvMsg.MAX_VALUE);
        ruleKeySet.add(PvMsg.MIN_LENGTH);
        ruleKeySet.add(PvMsg.MAX_LENGTH);
        ruleKeySet.add(PvMsg.REGEX);
        ruleKeySet.add(PvMsg.MESSAGE);
    }

    private ThreadLocal<List<Map<String, String>>> msgThreadLocal = new ThreadLocal<>();  //错误提示信息
    private ThreadLocal<String> ruleKeyThreadLocal = new ThreadLocal<>();  //规则的key

    @Autowired
    private RequestParam requestParam;
    @Autowired
    private RuleFile ruleFile;

    //校验
    public ResultValidate validateExecute(JoinPoint joinPoint, ValidateConfig validateConfig) throws Exception{
        ResultValidate resultValidate = new ResultValidate(true);  //默认是校验通过
        if (PvUtil.isNotBlank(validateConfig.getFile())){  //需要校验
            //初始化list
            msgThreadLocal.set(new ArrayList<>());
            //获取请求参数
            Map<String, Object> allParam = requestParam.mergeParams(joinPoint);
            //获取校验规则
            Map<String, Object> json = ruleFile.ruleFileJsonToMap(validateConfig, allParam.keySet());
            //执行校验
            validateJsonParam(json, allParam);
            if (msgThreadLocal.get().size() > 0){
                resultValidate.setPass(false);
                resultValidate.setMsgList(msgThreadLocal.get());
            }
        }
        return resultValidate;
    }

    /**
     * 校验规则与请求参数
     * @param json
     * @param paramMap 不可以为null
     */
    private void validateJsonParam(Map<String, Object> json, Map<String, Object> paramMap){
        if (json == null || json.size() == 0)  //没有校验规则，退出
            return ;

        //循环校验json
        for (Map.Entry<String, Object> jsonEntry:json.entrySet()){
            if (!(jsonEntry.getValue() instanceof Map))  //对象校验有request
                continue;

            Map<String, Object> jsonValue = (Map<String, Object>)jsonEntry.getValue();
            String key = jsonEntry.getKey();
            Object paramValue = paramMap.get(key);
            ruleKeyThreadLocal.set(key);
            if (ruleKeySet.containsAll(jsonValue.keySet())){   //jsonValue为校验规则rules
                checkRuleParamValue(jsonValue, paramValue, key);

                /**
                 * request:false   paramValue is depthEmpty  不再校验
                 * request:false   paramValue no Empty       校验
                 * request:null    paramValue is depthEmpty  校验
                 * request:null    paramValue no Empty       校验
                 */
            }else if (!("false".equals(PvUtil.objToStr(jsonValue.get(PvMsg.REQUEST)).toLowerCase())
                    && PvUtil.isDepthValueEmpty(paramValue))){

                if (PvUtil.isEmptySize0(paramValue)){  //request:null  paramValue isEmptySize0  校验
                    checkNoRequestFalseButEmptySize0(jsonValue);
                }else if (paramValue instanceof Map){  //paramValue是一个key-value
                    validateJsonParam(jsonValue, (Map<String, Object>)paramValue);
                }else if (paramValue instanceof List){  //paramValue是一个List
                    List paramList = (List)paramValue;
                    for (Object elem:paramList){
                        if (!(elem instanceof Map)){
                            throw new ParamsValidateException(String.format("传参或者校验规则错误，校验规则：%s，请求参数：%s", jsonValue, elem));
                        }
                        validateJsonParam(jsonValue, (Map<String, Object>)elem);
                    }
                }else {
                    throw new ParamsValidateException(String.format("传参或者校验规则错误，校验规则：%s，请求参数：%s", jsonValue, paramValue));
                }
            }
        }

    }

    //request:null  paramValue isEmptySize0  校验
    private void checkNoRequestFalseButEmptySize0(Map<String, Object> json){
        Set<String> jsonKeySet = json.keySet();
        if (ruleKeySet.containsAll(jsonKeySet) && PvUtil.hasRequestTrue(json)){
            msgThreadLocal.get().add(createFailMsg(json));
        }else {
            for (String key:jsonKeySet){
                if (json.get(key) instanceof Map){
                    ruleKeyThreadLocal.set(key);
                    //json对象的rule是否仍然有request:true
                    checkNoRequestFalseButEmptySize0((Map<String, Object>) json.get(key));
                }
            }
        }
    }

    /**
     * value为最后一层参数值，rules为校验规则
     * 只校验请求参数是否可为空
     */
    private void checkRuleParamValue(Map<String, Object> rules, Object value, String key){
        if (PvUtil.hasRequestTrue(rules)  //必填&&无值
                && (PvUtil.isBlankObj(value) || (value instanceof List && PvUtil.collectionSize0HasEmpty((List)value)))){
            msgThreadLocal.get().add(createFailMsg(rules));
        }else if (PvUtil.isNotBlankObj(value)){  //有值（Collection<基本类型>中的元素可能是empty）&&有校验规则
            if (value instanceof Collection){
                //请求参数：Collection<基本类型>
                Collection collection = (Collection)value;
                for (Object elem:collection){
                    ruleKeyThreadLocal.set(key);
                    //elem是Collection中的元素，为空也校验
                    checkRuleValueDetail(rules, elem);
                }
            }else {
                //value是Map中的value（基本类型），value非空，才能进入详情校验方法
                checkRuleValueDetail(rules, value);
            }
        }
    }

    /**
     * 最后一层参数值，详细规则校验
     * valueObj属于Collection<基本类型>，valueObj可为empty，也需要校验
     * valueObj属于Map的value，valueObj为Empty，不进入此方法，校验
     */
    private void checkRuleValueDetail(Map<String, Object> jsonRule, Object valueObj) {
        Object minValue = jsonRule.get(PvMsg.MIN_VALUE);
        Object maxValue = jsonRule.get(PvMsg.MAX_VALUE);
        Object minLength = jsonRule.get(PvMsg.MIN_LENGTH);
        Object maxLength = jsonRule.get(PvMsg.MAX_LENGTH);
        String regex = PvUtil.objToStr(jsonRule.get(PvMsg.REGEX));
        boolean noPass = false;
        //最小值
        if (PvUtil.isNotBlankObj(minValue)){
            try {
                BigDecimal value = new BigDecimal(PvUtil.objToStr(valueObj));
                if (value.compareTo(new BigDecimal(PvUtil.objToStr(minValue))) == -1){
                    noPass = true;
                }
            }catch (NumberFormatException e){
                PvUtil.logSevere(String.format("前端传值不是数字，参数值->%s，rule->%s", valueObj, jsonRule.toString()), e);
                noPass = true;
            }
        }
        //最大值
        if (PvUtil.isNotBlankObj(maxValue)){
            try {
                BigDecimal value = new BigDecimal(PvUtil.objToStr(valueObj));
                if (value.compareTo(new BigDecimal(PvUtil.objToStr(maxValue))) == 1){
                    noPass = true;
                }
            }catch (NumberFormatException e){
                PvUtil.logSevere(String.format("前端传值不是数字，参数值->%s，rule->%s", valueObj, jsonRule.toString()), e);
                noPass = true;
            }
        }

        //长度校验
        if (PvUtil.isNotBlankObj(minLength) && PvUtil.objToStr(valueObj).length() < PvUtil.getDouble(minLength)
            || PvUtil.isNotBlankObj(maxLength) && PvUtil.objToStr(valueObj).length() > PvUtil.getDouble(maxLength)){
            noPass = true;
        }

        //正则校验
        if (PvUtil.isNotBlank(regex)){
            if ( regex.startsWith(REGEX_BEGIN)){
                Map<String, String> result = ruleFile.getRegexCommon();  //读取init.json校验规则
                if (result == null || result.size() == 0)
                    throw new ParamsValidateException(String.format("校验异常，init.json未配置，无法获取%s", REGEX_BEGIN));

                regex = result.get(regex);
            }
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            if (!pattern.matcher(PvUtil.objToStr(valueObj)).matches()){
                noPass = true;
            }
        }

        //校验未通过
        if (noPass){
            msgThreadLocal.get().add(createFailMsg(jsonRule));
        }
    }

    //返回错误提示
    private Map<String, String> createFailMsg(Map<String, Object> jsonRule){
        Map<String, String> msgMap = new HashMap<>();
        msgMap.put(PvMsg.NAME, ruleKeyThreadLocal.get());
        ruleKeyThreadLocal.remove();
        for (Map.Entry<String, Object> entry:jsonRule.entrySet()){
            String key = entry.getKey();
            String value = PvUtil.objToStr(entry.getValue());
            if (PvUtil.isNotBlank(key) && PvUtil.isNotBlank(value)){
                msgMap.put(key, value);
            }
        }
        return msgMap;
    }

}
