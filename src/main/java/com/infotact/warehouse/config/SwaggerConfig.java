package com.infotact.warehouse.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    static {
        // This solves the 500 error when returning Spring Data Page objects
        SpringDocUtils.getConfig().replaceWithClass(
                org.springframework.data.domain.Pageable.class,
                org.springdoc.core.converters.models.PageableAsQueryParam.class
        );
    }
}