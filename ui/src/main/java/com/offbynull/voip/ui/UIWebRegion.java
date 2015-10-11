package com.offbynull.voip.ui;

import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.voip.ui.internalmessages.ChooseDevicesAction;
import com.offbynull.voip.ui.internalmessages.DisconnectAction;
import com.offbynull.voip.ui.internalmessages.GoToIdle;
import com.offbynull.voip.ui.internalmessages.ConnectAction;
import com.offbynull.voip.ui.internalmessages.ShowDeviceSelection;
import com.offbynull.voip.ui.internalmessages.GoToLogin;
import com.offbynull.voip.ui.internalmessages.GoToWorking;
import com.offbynull.voip.ui.internalmessages.ReadyAction;
import java.net.URL;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Platform;
import javafx.concurrent.Worker.State;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UIWebRegion extends Region {

    private static final Logger LOG = LoggerFactory.getLogger(UIWebRegion.class);

    private final WebView webView;
    private final WebEngine webEngine;

    private final Bus busFromGateway;
    private final Bus busToGateway;

    private final Lock lock;
    private Thread incomingMessagePumpThread;

    public static UIWebRegion create(Bus busFromGateway, Bus busToGateway) {
        UIWebRegion ret = new UIWebRegion(busFromGateway, busToGateway);

        URL resource = ret.getClass().getResource("/index.html");
        Validate.validState(resource != null); // should never happen, sanity check

        String mainPageLink = resource.toExternalForm();

        ret.webEngine.getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    if (newState == State.SUCCEEDED) {
                        JSObject win = (JSObject) ret.webEngine.executeScript("window");
                        win.setMember("messageSender", ret.new JavascriptToGatewayBridge());
                        
                        busToGateway.add(new UIAction(new ReadyAction(false)));
                    } else if (newState == State.CANCELLED || newState == State.FAILED) {
                        busToGateway.add(new UIAction(new ReadyAction(true)));
                    }
                });
        ret.webEngine.load(mainPageLink);

        // This block makes sure to kill the incoming message pump if this webregion is removed from its parent, and (re)starts it if its
        // added
        //
        // NOTE: CANNOT USE PARENTPROPERTY -- PARENTPROPERTY ALWAYS RETURNS NULL FOR WHATEVER REASON
        ret.sceneProperty().addListener((observable, oldValue, newValue) -> {
            ret.lock.lock();
            try {
                if (oldValue != null) {
                    ret.incomingMessagePumpThread.interrupt();
                    ret.incomingMessagePumpThread.join();
                    ret.incomingMessagePumpThread = null;
                }

                if (newValue != null) {
                    Thread thread = new Thread(ret.new GatewayToJavascriptPump());
                    thread.setDaemon(true);
                    thread.setName(UIWebRegion.class.getSimpleName() + "-GatewayToJavascriptPump");
                    thread.start();
                    ret.incomingMessagePumpThread = thread;
                }
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            } finally {
                ret.lock.unlock();
            }
        });

        return ret;
    }

    private UIWebRegion(Bus busFromGateway, Bus busToGateway) {
        Validate.notNull(busFromGateway);
        Validate.notNull(busToGateway);

        this.busFromGateway = busFromGateway;
        this.busToGateway = busToGateway;

        webView = new WebView();
        webEngine = webView.getEngine();
        
        webView.setContextMenuEnabled(false);

        getChildren().add(webView);

        lock = new ReentrantLock();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(webView, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
    }

    // This is an external thread that reads in messages from the gateway (via a bus) and invokes the proper javascript calls
    public final class GatewayToJavascriptPump implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    List<Object> incomingObjects = busFromGateway.pull();

                    Validate.notNull(incomingObjects);
                    Validate.noNullElements(incomingObjects);

                    for (Object incomingObj : incomingObjects) {
                        if (incomingObj instanceof GoToLogin) {
                            GoToLogin goToLogin = (GoToLogin) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToLogin", goToLogin.getMessage(), goToLogin.isReset());
                            });
                        } else if (incomingObj instanceof GoToWorking) {
                            GoToWorking goToWorking = (GoToWorking) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToWorking", goToWorking.getMessage());
                            });
                        } else if (incomingObj instanceof GoToIdle) {
                            GoToIdle goToLoggedIn = (GoToIdle) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToIdle");
                            });
                        } else if (incomingObj instanceof ShowDeviceSelection) {
                            ShowDeviceSelection showDeviceSelection = (ShowDeviceSelection) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("showDeviceSelection", showDeviceSelection.getInputDevices(), showDeviceSelection.getOutputDevices());
                            });
                        } else {
                            LOG.error("Unrecognized message: {}", incomingObj);
                        }
                    }
                }
            } catch (InterruptedException ie) {
                LOG.debug("Gateway to javascript message pump interrupted");
                Thread.interrupted();
            } catch (Exception e) {
                LOG.error("Internal error encountered", e);
            } finally {
                busFromGateway.close();
            }
        }
    }

    // This is a class that the javascript calls methods on to funnels messages to the gateway (via a bus)
    public final class JavascriptToGatewayBridge {

        public void loginAction(String username, String bootstrap) {
            busToGateway.add(new UIAction(new ConnectAction(username, bootstrap)));
        }

        public void logoutAction() {
            busToGateway.add(new UIAction(new DisconnectAction()));
        }

        public void chooseDevicesAction() {
            busToGateway.add(new UIAction(new ChooseDevicesAction()));
        }
    }
}
