package com.github.codingsoldier.paramsvalidate.bean;

public class PvConst {

    private PvConst() {}

    public static final String NAME = "name";  //请求参数名

    public static final String REQUEST = "request";
    public static final String MIN_VALUE = "minValue";
    public static final String MAX_VALUE = "maxValue";
    public static final String MIN_LENGTH = "minLength";
    public static final String MAX_LENGTH = "maxLength";
    public static final String REGEX = "regex";
    public static final String MESSAGE = "message";

    public static final String ELEM = "elem";

    public static final String LEVEL_STRICT = "STRICT";  //严格模式，发生异常，校验不通过，默认
    public static final String LEVEL_LOOSE = "LOOSE";    //宽松模式，发生异常，不校验

}
