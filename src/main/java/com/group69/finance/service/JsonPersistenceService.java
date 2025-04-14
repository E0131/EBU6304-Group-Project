package com.group69.finance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group69.finance.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class JsonPersistenceService implements PersistenceService {

    private static final Logger log = LoggerFactory.getLogger(JsonPersistenceService.class);
    private final ObjectMapper objectMapper; // Use Jackson

    @Autowired // Inject the configured ObjectMapper from JacksonConfig
    public JsonPersistenceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveTransactions(List<Transaction> transactions, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        log.debug("Saving {} transactions to {}", transactions.size(), path.toAbsolutePath());
        try {
            Files.createDirectories(path.getParent()); // Ensure directory exists
            // Write list as JSON using injected ObjectMapper (handles pretty printing via config)
            objectMapper.writeValue(path.toFile(), transactions);
            log.info("Data successfully written to {}", filePath);
        } catch (IOException e) {
            log.error("IOException during save to {}: {}", filePath, e.getMessage());
            throw e; // Re-throw for the caller (e.g., Repository) to handle
        }
    }

    @Override
    public List<Transaction> loadTransactions(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        log.debug("Loading transactions from {}", path.toAbsolutePath());
        if (!Files.exists(path) || Files.size(path) == 0) {
            log.warn("Data file not found or empty: {}. Returning empty list.", filePath);
            return new ArrayList<>();
        }

        try {
            // Use ObjectMapper to read the JSON file into a List<Transaction>
            List<Transaction> loaded = objectMapper.readValue(path.toFile(), new TypeReference<List<Transaction>>() {});
            log.info("Data successfully loaded {} transactions from {}", loaded.size(), filePath);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {
            log.error("IOException during load from {}: {}", filePath, e.getMessage());
            throw e; // Re-throw
        } catch (Exception e) { // Catch other potential parsing errors
            log.error("Failed to parse data from {}: {}", filePath, e.getMessage(), e);
            throw new IOException("Failed to parse data from file: " + e.getMessage(), e);
        }
    }
}