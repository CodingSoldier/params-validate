# params-validate
基于Spring MVC的请求参数校验库

**注意：只能在ssm、spring-boot等使用了Spring MVC框架的项目中使用**
# 使用方式：

## 1、pom.xml中导入jar包
	<dependency>
		<groupId>com.github.codingsoldier</groupId>
		<artifactId>params-validate</artifactId>
		<version>1.2-RELEASE</version>
		<!--最新版本请查看：
		http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.codingsoldier%22-->
	</dependency>
## 2、扫描com.github.codingsoldier.paramsvalidate目录。
  2.1 spring-boot项目，在启动类上加上注解，如下：  
  @ComponentScan("你自己项目的扫描路径, com.github.codingsoldier.paramsvalidate")  
  2.2 ssm项目在spring的xml配置中加于：  
  <context:component-scan base-package="你自己项目的扫描路径, com.github.codingsoldier.paramsvalidate"/>
## 3、开启aop。
  3.1 spring-boot项目，请在application.properties中配置：    
      spring.aop.proxy-target-class=true    
  3.2 ssm项目在Spring MVC的xml配置加入（注意，在spring的xml中加无效，一定要在Spring MVC的xml中加）：    
      <aop:aspectj-autoproxy proxy-target-class="true" />
## 4、编写校验文件。例如在resources目录下新建目录和文件，如下：
  config/validate/test.json  
  test.json文件中编写校验规则，如下：
  ```
  {
	  "num": {
	      "request": true,
	      "minValue": 1,
	      "maxValue": 1000000000,
	      "regex": "^\\d+$",
	      "message": "num:必填、正整数"
	  }  
  }
```
### 5、实现ValidateInterface接口
```java
@Service
public class ValidateInterfaceImpl implements ValidateInterface{

    //返回json校验文件的基础路径
    @Override
    public String basePath() {
    	return "config/validate/";    
    }

    //参数未通过校验, 返回自定义数据给客户端
    @Override
    public Object validateNotPass(ResultValidate resultValidate) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", resultValidate.isPass());
        r.put("msg", resultValidate.getMsgSet());
        return r;
    }

    /**
     * json解析器
     * 1、使用默认解析器jackson，直接返回null即可，使用此解析器，json校验文件必须以严格模式编写
     * 2、使用gson，请返回 new Parser(Gson.class);
     * 3、使用fastjson，请返回new Parser(JSON.class, Feature[].class)
     * 为了支持fastjson，搞得好坑爹
     */
    public Parser getParser(){
        return null;
        //return new Parser(Gson.class);
        //return new Parser(JSON.class, Feature[].class);
    }

    //不使用缓存，返回空map即可
    @Override
    public Map<String, Object> getCache(ValidateConfig validateConfig) {
        return new HashMap<>();
    }

    //不使用缓存，本方法可不处理
    @Override
    public void setCache(ValidateConfig validateConfig, Map<String, Object> json) {

    }
} 
```
## 6、controller方法上（注意：是方法上，比如：functionValidate()）添加注解：
   @ParamsValidate(file = "test.json")
## 7、前台ajax发送请求到functionValidate()，则ajax中的参数num必须符合校验规则：  
```
"request": true,
"minValue": 1,
"maxValue": 1000000000,
"regex": "^\\d+$"
```
  
<br><br>
更多使用方法请查看我的博客：  
@paramsValidate 一 简单使用  
https://blog.csdn.net/u010606397/article/details/80449600  
@paramsValidate 二 进阶  
https://blog.csdn.net/u010606397/article/details/80451603
<br><br>
我还写了一个测试项目来测试params-validate   
https://github.com/CodingSoldier/test-params-validate  

