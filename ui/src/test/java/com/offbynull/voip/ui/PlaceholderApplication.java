package com.offbynull.voip.ui;



import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.commons.lang3.Validate;

public final class PlaceholderApplication extends Application {

    private static final Lock LOCK = new ReentrantLock();
    private static Thread runnerThread;
    private static Supplier<Parent> startNodeSupplier;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Placeholder");
        
        Parent node;
        LOCK.lock();
        try {
            node = startNodeSupplier.get();
        } finally {
            LOCK.unlock();
        }
        
        Scene scene = new Scene(node, 900, 600, Color.web("#666970"));
        stage.setScene(scene);
        stage.show();
    }

    public static void start(Supplier<Parent> nodeSupplier) throws Exception {
        LOCK.lock();
        try {
            Validate.validState(PlaceholderApplication.startNodeSupplier == null, "Application already running");
            PlaceholderApplication.startNodeSupplier = nodeSupplier;
            
            // There's a race condition where stop() may have been called by the JavaFX framework but the Application hasn't terminated yet,
            // so wait for the runnerThread to be fully shut down before attempting to launch a new application
            if (runnerThread != null) {
                runnerThread.join();
            }
            
            runnerThread = new Thread(() -> Application.launch());
            runnerThread.start();
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public void stop() throws Exception {
        LOCK.lock();
        try {
            PlaceholderApplication.startNodeSupplier = null;
        } finally {
            LOCK.unlock();
        }
    }

}
