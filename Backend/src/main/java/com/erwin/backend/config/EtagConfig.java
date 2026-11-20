package com.erwin.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class EtagConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etagDocumentos() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registro =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registro.addUrlPatterns("/api/titulacion2/documento/*");
        registro.setName("etagDocumentos");
        return registro;
    }
}
