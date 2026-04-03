package com.Unify.controller;

import com.Unify.Navigator;
import javafx.fxml.FXML;

public class UtilityController {

    @FXML
    public void openLibraryMenu() {
        // Here we will navigate to a group selection screen specifically for the Library.
        // Or, if you want to reuse an existing modal, we can open it here.
        Navigator.goTo("/com/Unify/fxml/library.fxml");
    }
    @FXML
    public void openCanteenMenu() {
        Navigator.goTo("/com/Unify/fxml/canteen_view.fxml");
    }
    @FXML
    public void openTransportMenu() {
        Navigator.goTo("/com/Unify/fxml/transport.fxml");
    }
}