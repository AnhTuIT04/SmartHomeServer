package hcmut.smart_home.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import hcmut.smart_home.config.PublicEndpoint;
import hcmut.smart_home.exception.UnauthorizedException;
import hcmut.smart_home.util.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PublicEndpointInterceptor implements HandlerInterceptor {

    public PublicEndpointInterceptor(hcmut.smart_home.util.Jwt jwt) {
        this.jwt = jwt;
    }

    private final Jwt jwt;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/swagger-ui") || 
            requestURI.startsWith("/v3/api-docs") || 
            requestURI.startsWith("/webjars") || 
            requestURI.startsWith("/swagger-resources")) {
            return true;
        }
        
        if (handler instanceof HandlerMethod handlerMethod) {
            if (handlerMethod.hasMethodAnnotation(PublicEndpoint.class)) {
                return true; 
            }
        }
        
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException();
        }

        String token = authorizationHeader.substring(7);
        if (jwt.validateToken(token)) {
            String userId = jwt.extractId(token);
            request.setAttribute("userId", userId);
            return true;
        }
        
        throw new UnauthorizedException();
    }
}
