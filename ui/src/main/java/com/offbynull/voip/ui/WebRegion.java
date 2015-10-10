package com.offbynull.voip.ui;

import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.net.URL;
import javafx.concurrent.Worker.State;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.Validate;

final class WebRegion extends Region {

    private final WebView webView;
    private final WebEngine webEngine;
    
    private final Bus busFromGateway;
    private final Bus busToGateway;

    public static WebRegion create(Bus inBus, Bus outBus) {
        WebRegion ret = new WebRegion(inBus, outBus);
        
        URL resource = ret.getClass().getResource("/index.html");
        Validate.validState(resource != null); // should never happen, sanity check
                
        String mainPageLink = resource.toExternalForm();

        ret.webEngine.getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    if (newState == State.SUCCEEDED) {
                        JSObject win = (JSObject) ret.webEngine.executeScript("window");
                        win.setMember("messageSender", ret.new JavascriptToGatewayBridge());
                    }
                });
        ret.webEngine.load(mainPageLink);
        
        return ret;
    }
    
    private WebRegion(Bus busFromGateway, Bus busToGateway) {
        Validate.notNull(busFromGateway);
        Validate.notNull(busToGateway);
        
        this.busFromGateway = busFromGateway;
        this.busToGateway = busToGateway;

        webView = new WebView();
        webEngine = webView.getEngine();
    
        getChildren().add(webView);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(webView, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
    }

    public final class JavascriptToGatewayBridge {
        public void connectTriggered(String username, String bootstrap) {
            System.out.println(username + "/" + bootstrap);
        }
    }
}
