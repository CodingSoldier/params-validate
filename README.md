# params-validate
基于Spring MVC的请求参数校验库

注意：只能在ssm、spring-boot等使用了Spring MVC框架的项目中使用
使用方式：
## 1、pom.xml中导入jar包
	<dependency>
		<groupId>com.github.codingsoldier</groupId>
		<artifactId>params-validate</artifactId>
		<version>1.1-RELEASE</version>
		<!--最新版本请查看：
		http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.codingsoldier%22-->
	</dependency>
## 2、扫描com.github.codingsoldier.paramsvalidate目录。
  若为spring-boot项目，在	启动类上加上下边的注解
  @ComponentScan("你自己项目的扫描路径, com.github.codingsoldier.paramsvalidate")
## 3、编写校验文件，例如在resources目录下新建如下目录和文件
  config/validate/test.json 

  test.json文件中写入校验规则，如下：
  ```
  {
	  "id": {
	      "request": true,
	      "minValue": 1,
	      "maxValue": 1000000000,
	      "regex": "^\\d+$",
	      "message": "id:必填、正整数"
	  }  
  }
```
### 4、实现ValidateInterface接口
```
public class ValidateInterfaceImpl implements ValidateInterface{

    //返回json校验文件的基础路径
    @Override
    public String basePath() {
    	return "config/validate/";    
    }

    //参数校验未通过, 返回自定义数据给客户端
    @Override
    public Object validateNotPass(ResultValidate resultValidate) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", resultValidate.isPass());
        r.put("msg", resultValidate.getMsgSet());
        return r;
    }

    /**
     * json解析器
     * 1、使用默认解析器jackson，直接返回null即可，使用此解析器，json文件必须以严格模式编写
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
## 5、controller方法上（注意：是方法上，比如：functionValidate）添加注解：
   @ParamsValidate(file = "test.json")
## 6、前台ajax发送请求到functionValidate，则ajax中的参数id必须符合校验规则：  
```
"request": true,
"minValue": 1,
"maxValue": 1000000000,
"regex": "^\\d+$"
```
