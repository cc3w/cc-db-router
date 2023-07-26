package com.cc.ccdbroute.dbroute;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBRouterConfig {
    private int dbCount;

    private int tbCount;

    private String routerKey;

}
