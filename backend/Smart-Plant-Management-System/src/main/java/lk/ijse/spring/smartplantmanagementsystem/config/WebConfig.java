package lk.ijse.spring.smartplantmanagementsystem.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer config) {
        // 10 minutes
        config.setDefaultTimeout(600_000L);
    }
}
