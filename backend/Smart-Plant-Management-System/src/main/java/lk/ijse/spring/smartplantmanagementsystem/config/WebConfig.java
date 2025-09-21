package lk.ijse.spring.smartplantmanagementsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class WebConfig implements WebMvcConfigurer {
    @Value("${diagnosis.upload-dir}")
    private String uploadDir;

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDir);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        String location = path.toUri().toString();
        registry.addResourceHandler("/uploads/diagnoses/**")
                .addResourceLocations(location);
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer config) {
        // 10 minutes
        config.setDefaultTimeout(600_000L);
    }
}
