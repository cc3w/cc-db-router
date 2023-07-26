package com.cc.ccdbroute.dynamic;

import com.cc.ccdbroute.dbroute.DBContextHolder;
import com.cc.ccdbroute.dbroute.DBRouterConfig;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return "db" + DBContextHolder.getDBKey();
    }
}
