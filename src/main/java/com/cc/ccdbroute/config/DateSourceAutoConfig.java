package com.cc.ccdbroute.config;

import com.cc.ccdbroute.aspect.DBRouterAspect;
import com.cc.ccdbroute.dbroute.DBRouterConfig;
import com.cc.ccdbroute.dynamic.DynamicDataSource;
import com.cc.ccdbroute.dynamic.DynamicMybatisPlugin;
import com.cc.ccdbroute.strategy.IDBRouterStrategy;
import com.cc.ccdbroute.strategy.impl.IDBRouterStrategyImpl;
import com.cc.ccdbroute.util.PropertyUtil;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DateSourceAutoConfig implements EnvironmentAware{
    private Map<String, Map<String, Object>> dataSourceMap = new HashMap<>();

    private Map<String, Object> defaultDataSourceConfig;

    private int dbCount;

    private int tbCount;

    private String routerKey;

    @Bean
    @ConditionalOnMissingBean
    public DBRouterAspect aspect(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
        return  new DBRouterAspect(dbRouterConfig, dbRouterStrategy);
    }

    @Bean
    public DBRouterConfig dbRouterConfig() {
        return new DBRouterConfig(dbCount, tbCount, routerKey);
    }

    @Bean
    public IDBRouterStrategy dbRouterStrategy(DBRouterConfig dbRouterConfig) {
        return new IDBRouterStrategyImpl(dbRouterConfig);
    }


    @Bean
    public Interceptor plugin() {
        return new DynamicMybatisPlugin();
    }

    @Bean
    public DataSource dataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        for(String dbInfo : dataSourceMap.keySet()) {
            Map<String, Object> objMap = dataSourceMap.get((dbInfo));
            targetDataSources.put(dbInfo, new DriverManagerDataSource(objMap.get("url").toString(), objMap.get("username").toString(), objMap.get("password").toString()));
        }

        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(new DriverManagerDataSource(defaultDataSourceConfig.get("url").toString(), defaultDataSourceConfig.get("username").toString(), defaultDataSourceConfig.get("password").toString()));
        return dynamicDataSource;
    }

    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(dataSourceTransactionManager);
        transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return transactionTemplate;
    }



    /*
    * Environment 是一个接口，用于表示当前应用程序的运行环境和配置属性。它提供了获取和操作配置属性的方法。
    * */
    @Override
    public void setEnvironment(Environment environment) {
        String prefix = "cc-db-router.jdbc.datasource.";

        dbCount = Integer.valueOf(environment.getProperty(prefix + "dbCount"));
        tbCount = Integer.valueOf(environment.getProperty(prefix + "tbCount"));
        routerKey = environment.getProperty(prefix + "routerKey");

        String dataSources = environment.getProperty(prefix + "list");
        assert dataSources != null;

        for(String dbInfo : dataSources.split(",")) {
            Map<String, Object> dataSourceProps = PropertyUtil.handle(environment, prefix + dbInfo, Map.class);
            dataSourceMap.put(dbInfo, dataSourceProps);
        }

        String defaultData = environment.getProperty(prefix + "default");
        defaultDataSourceConfig = PropertyUtil.handle(environment, prefix + defaultData, Map.class);
    }
}
