module org.example.scenebuilder_test {
    requires javafx.controls;
    requires javafx.fxml;
    requires common;
    requires java.sql;


    opens GUI to javafx.fxml;
    exports GUI;
    opens Sketchify to javafx.graphics;
}