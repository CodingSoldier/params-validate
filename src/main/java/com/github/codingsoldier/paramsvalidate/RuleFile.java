package com.github.codingsoldier.paramsvalidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.codingsoldier.paramsvalidate.bean.Parser;
import com.github.codingsoldier.paramsvalidate.bean.ValidateConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * author chenpiqian 2018-05-25
 */
@Component
public class RuleFile {

    private static volatile Map<String, String> regexCommon;
    public static final String REGEX_COMMON_JSON = "init.json";

    @Autowired
    private ValidateInterface validateInterface;

    //获取需要校验的json
    public  Map<String, Object> fileJsonToMap(ValidateConfig validateConfig, Set<String> paramKeySet) throws Exception{
        String key = validateConfig.getKey();  //获取key
        String basePath = validateInterface.basePath();
        String filePath = String.format("%s/%s",PvUtil.trimBeginEndChar(basePath, '/'),
                PvUtil.trimBeginChar(validateConfig.getFile(), '/'));
        Map<String, Object>  json = validateInterface.getKeyCache(validateConfig);
        if (json == null || json.size() == 0) {
            Map<String, Map<String, Object>> fileJson = ruleFileRead(filePath);
            if (fileJson == null || fileJson.size() == 0) {
                throw new ParamsValidateException(String.format("读取%s,结果是null或者空json", filePath));
            }
            validateInterface.setFileCache(validateConfig, fileJson);

            json = PvUtil.isNotBlank(key) ? fileJson.get(key) : (Map)fileJson;
            if (json == null){
                throw new ParamsValidateException(String.format("%s文件中无key: %s", filePath, key));
            }
        }

        Map<String, Object> result = new HashMap<>();
        //请求参数的key包含了@ParamsValidate的key，只校验此key
        if (paramKeySet.contains(key) || paramKeySet.size() == 0){
            result.put(key, json);
        }else {
            result = json;
        }

        return result;
    }

    //读取json文件到Map<String,Map<String, Object>>
    private Map<String,Map<String, Object>> ruleFileRead(String filePath) throws Exception{
        Map<String,Map<String, Object>> fileJson = null;
        Parser parser = validateInterface.getParser();
        try (InputStream is = PvUtil.class.getClassLoader().getResourceAsStream(filePath)){
            if (is != null){
                if (parser != null && parser.getParserClass() != null){
                    Class parserClazz = parser.getParserClass();
                    Class featureArrClass = parser.getFeatureArrClass();
                    if ("com.google.gson.Gson".equals(parserClazz.getName())){
                        //使用gson解析
                        try (Reader reader = new InputStreamReader(new BufferedInputStream(is))){
                            Object gson = parserClazz.newInstance();
                            Method method = parserClazz.getMethod("fromJson", Reader.class, Class.class);
                            fileJson = (Map<String,Map<String, Object>>)method.invoke(gson, reader, Map.class );
                        }
                    }else if ("com.alibaba.fastjson.JSON".equals(parserClazz.getName())
                        && featureArrClass != null && "Feature[]".equals(featureArrClass.getSimpleName())){
                        //使用fastjson解析
                        Method method = parserClazz.getMethod("parseObject", InputStream.class, Type.class, featureArrClass);
                        fileJson = (Map<String,Map<String, Object>>)method.invoke(null, is, Map.class, null);
                    }else {
                        throw new ParamsValidateException("json解析器不符合规范，请修改getParser()");
                    }
                }else{
                    //使用Jackson解析
                    ObjectMapper mapper = new ObjectMapper();
                    fileJson = mapper.readValue(is, Map.class);
                }
            }
        }
        return fileJson;
    }

    //读取init.json文件到regexCommon
    public Map<String, String> getRegexCommon(){
        if (regexCommon == null){
            synchronized (this){
                if (regexCommon == null){
                    String filePath = PvUtil.trimBeginEndChar(validateInterface.basePath(), '/') + "/"+ REGEX_COMMON_JSON;
                    try {
                        regexCommon = (Map)ruleFileRead(filePath);
                    }catch (Exception ioe){
                        ParamsValidateException pve = new ParamsValidateException(String.format("读取、解析%s异常，%s",
                                filePath, ioe.getMessage()));
                        pve.initCause(ioe);
                        throw pve;
                    }
                }
            }
        }
        return regexCommon;
    }
}
