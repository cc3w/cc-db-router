package com.cc.ccdbroute.strategy.impl;

import com.cc.ccdbroute.dbroute.DBContextHolder;
import com.cc.ccdbroute.dbroute.DBRouterConfig;
import com.cc.ccdbroute.strategy.IDBRouterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IDBRouterStrategyImpl implements IDBRouterStrategy {

    private Logger logger = LoggerFactory.getLogger(IDBRouterStrategyImpl.class);

    private DBRouterConfig dbRouterConfig;

    public IDBRouterStrategyImpl(DBRouterConfig dbRouterConfig) {
        this.dbRouterConfig = dbRouterConfig;
    }

    @Override
    public void doRouter(String dbKeyAttr) {
        int size = dbRouterConfig.getDbCount() * dbRouterConfig.getTbCount();

        int idx = (size - 1) & (dbKeyAttr.hashCode() ^ (dbKeyAttr.hashCode() >>> 16));

        int dbIdx = idx / dbRouterConfig.getTbCount() + 1;
        int tbIdx = idx - (dbIdx - 1) * dbRouterConfig.getTbCount();

        DBContextHolder.setDBKey(String.format("%02d", dbIdx));
        DBContextHolder.setTBKey(String.format("%03d", tbIdx));
        logger.debug("数据库路由由 dbIdx : {}, tbIdx : {}", dbIdx, tbIdx);
    }

    @Override
    public void setDBKey(int dbIdx) {
        DBContextHolder.setDBKey(String.format("%02d", dbIdx));
    }

    @Override
    public void setTBKey(int tbIdx) {
        DBContextHolder.setTBKey(String.format("%03d", tbIdx));
    }

    @Override
    public int dbCount() {
        return dbRouterConfig.getDbCount();
    }

    @Override
    public int tbCount() {
        return dbRouterConfig.getTbCount();
    }

    @Override
    public void clear() {
        DBContextHolder.clearDBKey();
        DBContextHolder.clearTBKey();
    }
}

