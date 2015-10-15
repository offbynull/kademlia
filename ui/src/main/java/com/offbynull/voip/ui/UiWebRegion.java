/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.voip.ui;

import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.voip.ui.internalmessages.AcceptIncomingCallAction;
import com.offbynull.voip.ui.internalmessages.CallAction;
import com.offbynull.voip.ui.internalmessages.ChooseDevicesAction;
import com.offbynull.voip.ui.internalmessages.ResetDevicesAction;
import com.offbynull.voip.ui.internalmessages.LogoutAction;
import com.offbynull.voip.ui.internalmessages.GoToIdle;
import com.offbynull.voip.ui.internalmessages.LoginAction;
import com.offbynull.voip.ui.internalmessages.DevicesChosenAction;
import com.offbynull.voip.ui.internalmessages.ErrorAcknowledgedAction;
import com.offbynull.voip.ui.internalmessages.GoToOutgoingCall;
import com.offbynull.voip.ui.internalmessages.GoToDeviceSelection;
import com.offbynull.voip.ui.internalmessages.GoToEstablishedCall;
import com.offbynull.voip.ui.internalmessages.GoToIncomingCall;
import com.offbynull.voip.ui.internalmessages.GoToLogin;
import com.offbynull.voip.ui.internalmessages.GoToError;
import com.offbynull.voip.ui.internalmessages.GoToWorking;
import com.offbynull.voip.ui.internalmessages.HangupAction;
import com.offbynull.voip.ui.internalmessages.ReadyAction;
import com.offbynull.voip.ui.internalmessages.RejectIncomingCallAction;
import com.offbynull.voip.ui.internalmessages.UpdateMessageRate;
import java.net.URL;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

final class UiWebRegion extends Region {

    private static final Logger LOG = LoggerFactory.getLogger(UiWebRegion.class);

    private final WebView webView;
    private final WebEngine webEngine;

    private final Bus busFromGateway;
    private final Bus busToGateway;

    private final Lock lock;
    private Thread incomingMessagePumpThread;

    public static UiWebRegion create(Bus busFromGateway, Bus busToGateway) {
        UiWebRegion ret = new UiWebRegion(busFromGateway, busToGateway);

        URL resource = ret.getClass().getResource("/index.html");
        Validate.validState(resource != null); // should never happen, sanity check

        String mainPageLink = resource.toExternalForm();

        ret.webEngine.getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    if (newState == State.SUCCEEDED) {
                        JSObject win = (JSObject) ret.webEngine.executeScript("window");
                        win.setMember("messageSender", ret.new JavascriptToGatewayBridge());

                        busToGateway.add(new UiAction(new ReadyAction(false)));
                    } else if (newState == State.CANCELLED || newState == State.FAILED) {
                        busToGateway.add(new UiAction(new ReadyAction(true)));
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
                    thread.setName(UiWebRegion.class.getSimpleName() + "-GatewayToJavascriptPump");
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

    private UiWebRegion(Bus busFromGateway, Bus busToGateway) {
        Validate.notNull(busFromGateway);
        Validate.notNull(busToGateway);

        this.busFromGateway = busFromGateway;
        this.busToGateway = busToGateway;

        webView = new WebView();
        webEngine = webView.getEngine();

        webView.setContextMenuEnabled(false);
        webEngine.getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override
            public void changed(ObservableValue<? extends Throwable> ov, Throwable t, Throwable t1) {
                System.out.println("Received exception: " + t1.getMessage());
            }
        });

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
                        if (incomingObj instanceof GoToError) {
                            GoToError goToError = (GoToError) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToError", goToError.getMessage(), goToError.isUnrecoverable());
                            });
                        } else if (incomingObj instanceof GoToLogin) {
                            GoToLogin goToLogin = (GoToLogin) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToLogin", goToLogin.isReset());
                            });
                        } else if (incomingObj instanceof GoToWorking) {
                            GoToWorking goToWorking = (GoToWorking) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToWorking", goToWorking.getMessage());
                            });
                        } else if (incomingObj instanceof GoToIdle) {
                            GoToIdle goToIdle = (GoToIdle) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToIdle");
                            });
                        } else if (incomingObj instanceof GoToOutgoingCall) {
                            GoToOutgoingCall goToOutgoingCall = (GoToOutgoingCall) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToOutgoingCall", goToOutgoingCall.getUsername());
                            });
                        } else if (incomingObj instanceof GoToIncomingCall) {
                            GoToIncomingCall goToIncomingCall = (GoToIncomingCall) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToIncomingCall", goToIncomingCall.getUsername());
                            });
                        } else if (incomingObj instanceof GoToEstablishedCall) {
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("goToEstablishedCall");
                            });
                        } else if (incomingObj instanceof UpdateMessageRate) {
                            UpdateMessageRate updateMessageRate = (UpdateMessageRate) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                win.call("updateMessageRate", updateMessageRate.getIncomingMessagesPerSecond(),
                                        updateMessageRate.getOutgoingMessagesPerSecond());
                            });
                        } else if (incomingObj instanceof GoToDeviceSelection) {
                            GoToDeviceSelection showDeviceSelection = (GoToDeviceSelection) incomingObj;
                            Platform.runLater(() -> {
                                JSObject win = (JSObject) webEngine.executeScript("window");
                                
                                // Passing Java objects directly as arguments using call() causes a ton of issues. Lots of problems trying
                                // to access an iterator and elements within map... so convert these maps to JSObjects before submitting
                                JSObject jsInputDevices = InternalUtils.mapToJSObject(webEngine, showDeviceSelection.getInputDevices());
                                JSObject jsOutputDevices = InternalUtils.mapToJSObject(webEngine, showDeviceSelection.getOutputDevices());
                                
                                win.call("showDeviceSelection", jsInputDevices, jsOutputDevices);
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
            busToGateway.add(new UiAction(new LoginAction(username, bootstrap)));
        }

        public void logoutAction() {
            busToGateway.add(new UiAction(new LogoutAction()));
        }

        public void resetDevicesAction() {
            busToGateway.add(new UiAction(new ResetDevicesAction()));
        }

        public void chooseDevicesAction(int inputId, int outputId) {
            busToGateway.add(new UiAction(new ChooseDevicesAction(inputId, outputId)));
        }

        public void devicesChosenAction() {
            busToGateway.add(new UiAction(new DevicesChosenAction()));
        }

        public void callAction(String username) {
            busToGateway.add(new UiAction(new CallAction(username)));
        }

        public void acceptIncomingCallAction() {
            busToGateway.add(new UiAction(new AcceptIncomingCallAction()));
        }

        public void rejectIncomingCallAction() {
            busToGateway.add(new UiAction(new RejectIncomingCallAction()));
        }

        public void hangupCallAction() {
            busToGateway.add(new UiAction(new HangupAction()));
        }

        public void errorAcknowledgedAction() {
            busToGateway.add(new UiAction(new ErrorAcknowledgedAction()));
        }
    }
}
