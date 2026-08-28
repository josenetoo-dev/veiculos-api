package com.josenetoo_dev.veiculos_api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class UploadConfig implements WebMvcConfigurer {

    @Value("${upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Caminho absoluto garante que o Spring encontra os arquivos independente de onde o app roda
        String caminhoAbsoluto = Paths.get(uploadDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/fotos/**")
                .addResourceLocations("file:" + caminhoAbsoluto + "/");
    }
}