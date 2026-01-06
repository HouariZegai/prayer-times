module com.houarizegai.prayertimes {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires javafx.base;
    
    requires java.desktop;
    requires java.logging;
    
    // Automatic modules (non-modular dependencies)
    // Module names are derived from JAR filenames
    requires com.jfoenix;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.ikonli.icomoon;
    requires unirest.java;  // JAR: unirest-java-*.jar
    requires json;  // JAR: json-*.jar
    requires lombok;
    
    opens com.houarizegai.prayertimes to javafx.fxml;
    opens com.houarizegai.prayertimes.controller to javafx.fxml;
    
    exports com.houarizegai.prayertimes;
}

