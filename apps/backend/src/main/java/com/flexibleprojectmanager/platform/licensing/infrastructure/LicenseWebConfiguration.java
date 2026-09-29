package com.flexibleprojectmanager.platform.licensing.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LicenseWebConfiguration implements WebMvcConfigurer {
    private final LicenseEnforcementInterceptor interceptor;

    public LicenseWebConfiguration(LicenseEnforcementInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/v1/**");
    }
}
