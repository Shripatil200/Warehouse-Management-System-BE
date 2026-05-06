package com.infotact.warehouse.config;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.context.annotation.Configuration;

@Configuration
@FilterDef(
        name = "warehouseFilter",
        parameters = @ParamDef(name = "warehouseId", type = String.class)
)
public class HibernateFilterConfig {
}