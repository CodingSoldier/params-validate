package com.github.codingsoldier.paramsvalidate;


import com.github.codingsoldier.paramsvalidate.bean.Parser;
import com.github.codingsoldier.paramsvalidate.bean.ResultValidate;
import com.github.codingsoldier.paramsvalidate.bean.ValidateConfig;

import java.util.Map;

/**
 * author chenpiqian 2018-05-25
 */
public interface ValidateInterface {

    /**
     * 返回json文件基础路径。init.json文件必须放在此目录下
     * @return json文件基础路径
     */
    String basePath();

    /**
     * 校验级别
     * PvConst.LEVEL_STRICT  严格模式，发生异常，校验不通过，默认
     * PvConst.LEVEL_LOOSE   宽松模式，发生异常，不校验
     */
    String getLevel();

    /**
     * json解析器
     * 1、使用默认解析器jackson，可不覆盖此方法。
     * 2、使用gson，请返回 new Parser(Gson.class)。
     * 3、使用fastjson，请返回new Parser(JSON.class, Feature[].class)。
     * 为了支持fastjson，搞得好坑爹。
     */
    Parser getParser();

    /**
     * 参数校验未通过
     * @param resultValidate
     * @return 返回给客户端的数据
     */
    Object validateNotPass(ResultValidate resultValidate);

    /**
     * 获取缓存中某个hashKey的校验规则
     * @param validateConfig
     * @return  hashKey的值
     */
    Map<String, Object> getKeyCache(ValidateConfig validateConfig);

    /**
     * json文件的校验规则储存到缓存中
     * @param validateConfig
     * @param json json文件的校验规则
     */
    void setFileCache(ValidateConfig validateConfig, Map<String, Map<String, Object>> json);

}
