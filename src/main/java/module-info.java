module org.example.scenebuilder_test {
    requires javafx.controls;
    requires javafx.fxml;
    requires common;


    opens GUI to javafx.fxml;
    exports GUI;
}