module com.Unify {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires java.desktop;
    requires kotlin.stdlib;


    opens com.Unify.controller to javafx.fxml;
    opens com.Unify.model to javafx.base;
    exports com.Unify;
}