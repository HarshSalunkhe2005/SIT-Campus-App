package com.sit.campusbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final Path uploadRoot;
    private final Path frontendRoot;

    public AppConfig(@Value("${app.upload-dir}") String uploadDir, @Value("${app.frontend-dir:}") String frontendDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.frontendRoot = frontendDir.isBlank() ? null : Path.of(frontendDir).toAbsolutePath().normalize();
    }

    /** Injectable clock so expiry and rate-limit logic can be tested without sleeping. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Uploaded complaint photos are served from disk. File names are random UUIDs, so they are unguessable. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadRoot.toUri().toString())
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());

        // Single-deployment mode (FRONTEND_DIR set): this app also serves the pages, so the browser and API share an origin.
        if (frontendRoot != null) {
            registry.addResourceHandler("/templates/**").addResourceLocations(dirUri(frontendRoot.resolve("templates")));
            registry.addResourceHandler("/static/**").addResourceLocations(dirUri(frontendRoot.resolve("static")));
        }
    }

    private static String dirUri(Path dir) {
        String uri = dir.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }
}
