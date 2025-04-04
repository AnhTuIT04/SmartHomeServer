package hcmut.smart_home.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import hcmut.smart_home.handler.WebSocketNotificationHandler;
import hcmut.smart_home.handler.WebSocketRealtimeHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketRealtimeHandler webSocketRealtimeHandler;
    private final WebSocketNotificationHandler webSocketNotificationHandler;

    public WebSocketConfig(WebSocketRealtimeHandler webSocketRealtimeHandler, WebSocketNotificationHandler webSocketNotificationHandler) {
        this.webSocketRealtimeHandler = webSocketRealtimeHandler;
        this.webSocketNotificationHandler = webSocketNotificationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketRealtimeHandler, "/ws/realtime")
            .addInterceptors(new HttpSessionHandshakeInterceptor())
            .setAllowedOrigins("*");

        registry.addHandler(webSocketNotificationHandler, "/ws/notification")
            .addInterceptors(new HttpSessionHandshakeInterceptor())
            .setAllowedOrigins("*");
    }
}
