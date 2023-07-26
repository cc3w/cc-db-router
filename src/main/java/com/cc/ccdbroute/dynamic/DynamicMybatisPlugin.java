package com.cc.ccdbroute.dynamic;

import com.cc.ccdbroute.annotation.DBRouterStrategy;

import com.cc.ccdbroute.dbroute.DBContextHolder;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class DynamicMybatisPlugin implements Interceptor {

    /*
    * 当我们逐个解析这个正则表达式时，它由以下几个部分组成：
    1. `(from|into|update)`: 这是一个括号内的表达式，使用竖线 `|` 分隔了三个不同的子表达式。这表示正则表达式将匹配字符串中的"from"、"into"或"update"中的任意一个单词。
    2. `[\\s]{1,}`: 这是一个字符类表达式，用于匹配一个或多个空白字符。`[\\s]`表示空白字符，`{1,}`表示匹配前面的字符类表达式一次或多次。
    3. `(\\w{1,})`: 这是一个括号内的表达式，表示由一个或多个字母数字字符组成的单词。`\w`匹配任何字母、数字或下划线字符。
    * Pattern.CASE_INSENSITIVE是一个标志，用于指示正则表达式在匹配时应该忽略大小写。
    因此，整个正则表达式用来匹配字符串中以大小写不敏感的方式包含"from"、"into"或"update"之后至少一个空白字符，
    * 然后跟随一个或多个字母数字字符的模式。这种正则表达式可用于从文本中提取包含特定关键词的部分或进行过滤操作。
    * */
    private Pattern pattern = Pattern.compile("(from|into|update)[\\s]{1,}(\\w{1,})", Pattern.CASE_INSENSITIVE);


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //从拦截的对象中获取StatementHandler实例，StatementHandler就是一个封装了jdbc的各种操作
        //将他强转为StatementHandler类型，就是因为这个类里面有对sql语句的各种操作，如获取执行的sql语句，修改sql语句
        /*
        * invocation是一个代理对象，用于调用目标方法
        * getTarget()用于获取代理对象的目标对象
        * */
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        //创建StatementHandler实例的元对象，用于获取和设置属性列
        /*
        * MetaObjectd是mybatis提供的一个工具类，用于操作java对象的属性和方法，支持对对象的反射操作
        * forObjec()方法用于创建MetaObject对象
        * 第一个参数表示要操作的对象
        * SystemMetaObject.DEFAULT_OBJECT_FACTORY表示创建对象时使用默认的对象工厂。
        * SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY表示创建对象时使用默认的对象包装器工厂。
        * new DefaultReflectorFactory()表示创建对象时使用默认的反射工厂。
        * 这行代码的作用是创建一个MetaObject对象，用于操作statementHandler对象，以实现对其属性和方法的访问和修改。
        * */
        MetaObject metaObject = MetaObject.forObject(statementHandler,
                SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());

        //从元对象中获取包含sql语句元信息的MappedStatement实例
        /*
        * 为什么要获取MappedStatement对象？
        * 在MyBatis中，MappedStatement是一个用于描述SQL映射配置的对象，它包含了SQL语句的各种元数据信息，如命名空间、语句ID、参数映射、结果映射等。
        * */
        MappedStatement mappedStatement =(MappedStatement) metaObject.getValue("delegate.mappedStatement");

        //通过类的全限定名，并根据类名后去Class实例，id就是那个namespace，如com.cc.dbroutertest.mapper.UserMapper
        String id = mappedStatement.getId();
        String className = id.substring(0, id.lastIndexOf("."));
        //获取一个Class对象，Class对象是java中对类的抽象表示，通过这个Class对象，可以获取类的结构、注解、方法等
        Class<?> clazz = Class.forName(className);

        //从获取的Class实例中获取DBRouterStrategy注解的实例
        DBRouterStrategy dbRouterStrategy = clazz.getAnnotation(DBRouterStrategy.class);
        //判断DBRouterStrategy注解是否存在或分表属性为false，如果判断为真，则执行原始的sql语句
        /*
        * invocation.proceed()方法的调用会继续执行原始的查询方法，
        * 等待查询方法执行完毕后，再执行后置处理逻辑，并将执行结果返回。
        * 这样就能够在拦截器中对原始方法进行增强、修改或者记录日志等操作
        * */
        if(null == dbRouterStrategy || !dbRouterStrategy.splitTable()) {
            return invocation.proceed();
        }

        //获取sql语句对象BoundSql
        /*
        * 通过statementHandler.getBoundSql()方法可以获取到包含原始SQL语句及其参数的BoundSql对象
        * 比如占位符“？”和参数列表
        * */
        BoundSql boundSql = statementHandler.getBoundSql();
        //从BoundSql对象中获取原始的sql语句
        String sql = boundSql.getSql();

        //使用正则表达式匹配出sql语句中的表名
        Matcher matcher = pattern.matcher(sql);
        String tableName = null;

        if(matcher.find()) {
            tableName = matcher.group().trim();
        }

        assert null != tableName;
        //根据表名和分表键生成替换后的sql语句
        String replaceSql = matcher.replaceAll(tableName + "_" + DBContextHolder.getTBKey());

        //使用反射修改BoundSql对象的sql属性，将原始sql语句替换为分表后的sql语句
        Field field = boundSql.getClass().getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, replaceSql);
        field.setAccessible(false);

        //执行替换后的sql语句
        return invocation.proceed();
    }
}
