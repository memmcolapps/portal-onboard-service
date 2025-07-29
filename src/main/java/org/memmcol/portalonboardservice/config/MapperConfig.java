package org.memmcol.portalonboardservice.config;

import org.memmcol.portalonboardservice.util.UUIDTypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@MapperScan("org.memmcol.portalonboardservice.mapper")
public class MapperConfig {

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> configuration.getTypeHandlerRegistry()
                .register(UUID.class, new UUIDTypeHandler());
    }
}
