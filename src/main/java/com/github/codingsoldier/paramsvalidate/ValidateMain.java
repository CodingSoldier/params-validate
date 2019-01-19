package com.github.codingsoldier.paramsvalidate;

import com.github.codingsoldier.paramsvalidate.bean.PvConst;
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
        ruleKeySet.add(PvConst.REQUEST);
        ruleKeySet.add(PvConst.MIN_VALUE);
        ruleKeySet.add(PvConst.MAX_VALUE);
        ruleKeySet.add(PvConst.MIN_LENGTH);
        ruleKeySet.add(PvConst.MAX_LENGTH);
        ruleKeySet.add(PvConst.REGEX);
        ruleKeySet.add(PvConst.MESSAGE);
    }

    private ThreadLocal<List<Map<String, String>>> msgListThreadLocal = new ThreadLocal<>();  //错误提示信息
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
            msgListThreadLocal.set(new ArrayList<>());
            //获取请求参数
            Map<String, Object> allParam = requestParam.mergeParams(joinPoint);
            //获取校验规则
            Map<String, Object> json = ruleFile.fileJsonToMap(validateConfig, allParam.keySet());
            //执行校验
            validateJsonParamHandler(json, allParam);
            if (msgListThreadLocal.get().size() > 0){
                resultValidate.setPass(false);
                resultValidate.setMsgList(msgListThreadLocal.get());
            }
        }
        return resultValidate;
    }

    /**
     * 校验规则与请求参数
     * @param json
     * @param paramMap 不可以为null
     */
    private void validateJsonParamHandler(Map<String, Object> json, Map<String, Object> paramMap){
        if (json == null || json.size() == 0)  //没有校验规则，退出
            return ;

        //循环校验json
        for (Map.Entry<String, Object> jsonEntry:json.entrySet()){
            if (!(jsonEntry.getValue() instanceof Map)){  //对象校验有request
                continue;
            }
            Map<String, Object> jsonValue = (Map<String, Object>)jsonEntry.getValue();
            String key = jsonEntry.getKey();
            Object paramValue = paramMap.get(key);
            ruleKeyThreadLocal.set(key);

            if (ruleKeySet.containsAll(jsonValue.keySet())){   //jsonValue为校验规则rules
                checkRuleParamValue(jsonValue, paramValue, key);
                /**
                 * request:false   paramValue   Empty   不再校验
                 * request:false   paramValue  noEmpty  校验
                 * request:null    paramValue   Empty   校验
                 * request:null    paramValue  noEmpty  校验
                 */
            }else if (!(PvUtil.isFalse(jsonValue.get(PvConst.REQUEST)) && PvUtil.isBlankObj(paramValue))){
                if (PvUtil.isBlankObj(paramValue)){  //request:null  paramValue  Empty  校验
                    checkNoRequestFalseButEmpty(jsonValue);
                }else if (paramValue instanceof Map){  //paramValue是一个key-value
                    validateJsonParamHandler(jsonValue, (Map<String, Object>)paramValue);
                }else if (paramValue instanceof Collection){  //paramValue是一个List
                    checkCollection(jsonValue, (Collection)paramValue, key);
                }else {
                    throw new ParamsValidateException(String.format("传参或者校验规则错误，校验规则：%s，请求参数：%s", jsonValue, paramValue));
                }
            }
        }
    }

    //参数值是Collection
    private void checkCollection(Map<String, Object> jsonValue, Collection paramValue, String key){

        Integer min = PvUtil.objToInteger(jsonValue.get(PvConst.MIN_LENGTH));
        Integer max = PvUtil.objToInteger(jsonValue.get(PvConst.MAX_LENGTH));
        boolean noPass = min != null && paramValue.size() < min ? true : false;
        noPass = max != null && paramValue.size() > max ? true : noPass;

        if (noPass){
            addFailMsg(jsonValue);
        }

        if (jsonValue.get(PvConst.ELEM) instanceof Map){
            Map jsonElemValue = (Map) jsonValue.get(PvConst.ELEM);
            Map jsonEntry = new HashMap();
            Map paramEntry = new HashMap();
            jsonEntry.put(String.format("%s.%s",key,PvConst.ELEM), jsonElemValue);
            if (paramValue.size() != 0){
                for (Object paramElem:paramValue){
                    paramEntry.put(String.format("%s.%s",key,PvConst.ELEM), paramElem);
                    validateJsonParamHandler(jsonEntry, paramEntry);
                }
            }else if (!(PvUtil.isFalse(jsonElemValue.get(PvConst.REQUEST)))){
                checkNoRequestFalseButEmpty(jsonElemValue);
            }

        }
    }

    //request:null  paramValue is empty  校验
    private void checkNoRequestFalseButEmpty(Map<String, Object> json){
        Set<String> jsonKeySet = json.keySet();
        if (ruleKeySet.containsAll(jsonKeySet) && PvUtil.isTrue(json.get(PvConst.REQUEST))){
            addFailMsg(json);
        }else {
            for (String key:jsonKeySet){
                if (json.get(key) instanceof Map){
                    ruleKeyThreadLocal.set(key);
                    //json对象的rule是否仍然有request:true
                    checkNoRequestFalseButEmpty((Map<String, Object>) json.get(key));
                }
            }
        }
    }

    /**
     * value为最后一层参数值，rules为校验规则
     * 只校验请求参数是否可为空
     */
    private void checkRuleParamValue(Map<String, Object> rules, Object value, String key){
        if (PvUtil.isTrue(rules.get(PvConst.REQUEST)) && PvUtil.isBlankObj(value)){
            addFailMsg(rules);  //必填&&无值
        }else if (PvUtil.isNotBlankObj(value)){  //有值&&有校验规则
            checkRuleValueDetail(rules, value);
        }
    }

    /**
     * 最后一层参数值，详细规则校验
     * valueObj属于Collection<基本类型>，valueObj可为empty，也需要校验
     * valueObj属于Map的value，valueObj为Empty，不进入此方法，校验
     */
    private void checkRuleValueDetail(Map<String, Object> jsonRule, Object valueObj) {
        Object minValue = jsonRule.get(PvConst.MIN_VALUE);
        Object maxValue = jsonRule.get(PvConst.MAX_VALUE);
        Object minLength = jsonRule.get(PvConst.MIN_LENGTH);
        Object maxLength = jsonRule.get(PvConst.MAX_LENGTH);
        String regex = PvUtil.objToStr(jsonRule.get(PvConst.REGEX));
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
            addFailMsg(jsonRule);
        }
    }

    //返回错误提示
    private void addFailMsg(Map<String, Object> json){
        Map<String, String> msgMap = new HashMap<>();
        msgMap.put(PvConst.NAME, ruleKeyThreadLocal.get());
        for (Map.Entry<String, Object> entry:json.entrySet()){
            String key = entry.getKey();
            String value = PvUtil.objToStr(entry.getValue());
            if (PvUtil.isNotBlank(key) && ruleKeySet.contains(key) && PvUtil.isNotBlank(value)){
                msgMap.put(key, value);
            }
        }
        msgListThreadLocal.get().add(msgMap);
        ruleKeyThreadLocal.remove();
    }

}
