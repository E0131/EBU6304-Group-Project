package com.group69.finance;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // Enables component scanning for beans (Services, Controllers, etc.)
public class App {

    public static void main(String[] args) {
        // Launch the separate JavaFX Application class
        // FxApplication will handle Spring context initialization and UI startup
        Application.launch(FxApplication.class, args);
    }
}