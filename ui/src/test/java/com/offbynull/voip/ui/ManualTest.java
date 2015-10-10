package com.offbynull.voip.ui;

import java.util.function.Supplier;
import javafx.scene.Parent;



public class ManualTest {

    public static void main(String[] args) {
        UIGateway uiGateway = new UIGateway("ui");
        Supplier<Parent> componentSupplier = uiGateway.getJavaFXComponent();
        PlaceholderApplication.start(componentSupplier);
    }

}
