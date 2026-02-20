module com.example.unify {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires kotlin.stdlib;
    requires java.rmi;
    requires java.desktop;

    opens com.example.unify.calendar.controller to javafx.fxml;
    //opens com.example.unify.calendar.view to javafx.fxml;;
    opens com.example.unify to javafx.fxml;
    exports com.example.unify;
}