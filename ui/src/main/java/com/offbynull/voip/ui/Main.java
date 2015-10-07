package com.offbynull.voip.ui;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class Main extends Application {

    private Scene scene;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Web View");
        scene = new Scene(new Browser(), 900, 600, Color.web("#666970"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class Browser extends Region {

    private final WebView browser = new WebView();
    private final WebEngine webEngine = browser.getEngine();

    public Browser() {
        String mainPageLink = Main.class.getResource("/index.html").toExternalForm();
        System.out.println(mainPageLink);
        webEngine.load(mainPageLink);
        getChildren().add(browser);

    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(browser, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
    }
}
