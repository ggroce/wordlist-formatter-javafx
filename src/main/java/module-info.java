module org.wordlistformatter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens org.wordlistformatter to javafx.fxml;
    exports org.wordlistformatter;
}