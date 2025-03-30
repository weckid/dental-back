import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Разрешить все эндпоинты
                        .allowedOrigins("http://localhost:5173") // Разрешить запросы с фронтенда
                        .allowedMethods("GET", "POST", "PUT", "DELETE") // Разрешить методы HTTP
                        .allowedHeaders("*") // Разрешить все заголовки
                        .allowCredentials(true); // Разрешить отправку куки
            }
        };
    }
}