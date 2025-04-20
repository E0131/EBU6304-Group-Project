package com.group69.finance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary // Make this the default ObjectMapper
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Configure for Java 8+ Date/Time types (LocalDate, etc.)
        objectMapper.registerModule(new JavaTimeModule());
        // Configure pretty printing for saved JSON file
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Configure date format if needed (ISO_LOCAL_DATE is default for LocalDate)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Optional: Don't fail on unknown properties during deserialization
        // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}