module com.example.projetjavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires okhttp3;
    requires org.json;
    requires java.sql;
    requires java.mail;
    opens com.example.projetjavafx to javafx.fxml;
    exports com.example.projetjavafx;
}