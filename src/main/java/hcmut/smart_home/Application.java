package hcmut.smart_home;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hcmut.smart_home.config.PublicEndpoint;

@SpringBootApplication
@RestController
@RequestMapping("/")
public class Application {

	@Value("${server.port}")
    private String serverPort;

	@Value("${springdoc.swagger-ui.path}")
	private String swaggerPath;

	@Value("${spring.application.name}")
	private String appName;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@PublicEndpoint
	@GetMapping
    public Map<String, Object> getServerInfo() {
        Map<String, Object> serverInfo = new HashMap<>();

        serverInfo.put("message", String.format("Welcome to %s API server", appName));
        serverInfo.put("serverPort", serverPort);
        serverInfo.put("osName", System.getProperty("os.name"));
        serverInfo.put("javaVersion", System.getProperty("java.version"));
        serverInfo.put("apiDoc", swaggerPath);

        return serverInfo;
    }

}
