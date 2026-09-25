package com.moriba.skultem.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.moriba.skultem.infrastructure.security.ModuleAccessInterceptor;
import com.moriba.skultem.infrastructure.security.SectionScopeInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ModuleAccessConfig implements WebMvcConfigurer {

    private final ModuleAccessInterceptor moduleAccessInterceptor;
    private final SectionScopeInterceptor sectionScopeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(moduleAccessInterceptor).addPathPatterns("/api/**");
        // Management-section scope for section-limited staff - see SectionScopeInterceptor.
        registry.addInterceptor(sectionScopeInterceptor).addPathPatterns("/api/**");
    }
}
