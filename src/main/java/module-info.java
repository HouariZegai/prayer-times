module com.houarizegai.prayertimes {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires javafx.base;
    
    requires java.desktop;
    requires java.logging;
    
    // Automatic modules (non-modular dependencies)
    requires com.jfoenix;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.ikonli.icomoon;
    requires kong.unirest;
    requires org.json;
    requires lombok;
    
    opens com.houarizegai.prayertimes to javafx.fxml;
    opens com.houarizegai.prayertimes.controller to javafx.fxml;
    
    exports com.houarizegai.prayertimes;
}

