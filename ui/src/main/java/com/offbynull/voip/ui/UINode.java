package com.offbynull.voip.ui;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public final class UINode extends Region {
    private final WebView browser = new WebView();
    private final WebEngine webEngine = browser.getEngine();

    public UINode() {
        String mainPageLink = Main.class.getResource("/index.html").toExternalForm();
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
