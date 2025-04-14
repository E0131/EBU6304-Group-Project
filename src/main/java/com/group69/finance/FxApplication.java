package com.group69.finance;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.URL;

public class FxApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(FxApplication.class);
    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        log.info("Initializing JavaFX Application and Spring Context...");
        try {
            // Initialize Spring Boot context, using App class for component scanning
            springContext = new SpringApplicationBuilder(App.class)
                    .run(getParameters().getRaw().toArray(new String[0])); // Pass command line args if needed
            log.info("Spring Context Initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Spring Context!", e);
            throw e; // Prevent start if Spring fails
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting JavaFX Application UI...");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL fxmlUrl = getClass().getResource("/com/group69/finance/view/fxml/main-window.fxml"); // Path within resources

            if (fxmlUrl == null) {
                log.error("Cannot find FXML file: /com/group69/finance/view/fxml/main-window.fxml");
                throw new IOException("Cannot find FXML file.");
            }
            log.debug("Loading FXML from: {}", fxmlUrl);
            fxmlLoader.setLocation(fxmlUrl);

            // IMPORTANT: Set the controller factory BEFORE loading FXML
            fxmlLoader.setControllerFactory(springContext::getBean);

            Parent root = fxmlLoader.load();

            Scene scene = new Scene(root, 850, 650); // Adjusted size slightly
            // Example: Add CSS
            // URL cssUrl = getClass().getResource("/com/group69/finance/view/css/styles.css");
            // if (cssUrl != null) {
            //     scene.getStylesheets().add(cssUrl.toExternalForm());
            // } else {
            //     log.warn("CSS file not found.");
            // }

            primaryStage.setScene(scene);
            // Retrieve title from Spring context/properties if desired, or set directly
            // String title = springContext.getEnvironment().getProperty("app.ui.title", "Finance Tracker");
            primaryStage.setTitle("Personal Finance Tracker (Group69 FX)");
            primaryStage.show();
            log.info("JavaFX Application UI Started.");

        } catch (Exception e) { // Catch broader exceptions during FXML loading/init
            log.error("Failed to load FXML or start JavaFX UI!", e);
            Platform.exit(); // Ensure exit if UI fails
        }
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping JavaFX Application and closing Spring Context...");
        if (springContext != null) {
            springContext.close();
        }
        log.info("Application Stopped.");
        // Platform.exit() is implicitly called after stop() returns
    }
}