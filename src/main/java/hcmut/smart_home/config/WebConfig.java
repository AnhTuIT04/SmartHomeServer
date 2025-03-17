package hcmut.smart_home.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import hcmut.smart_home.interceptor.PublicEndpointInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PublicEndpointInterceptor publicEndpointInterceptor;

    public WebConfig(PublicEndpointInterceptor publicEndpointInterceptor) {
        this.publicEndpointInterceptor = publicEndpointInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(publicEndpointInterceptor);
    }
}
