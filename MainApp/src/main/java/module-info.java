module MainApp{
    requires javafx.controls;
    requires javafx.fxml;
    requires Sockets;
    opens com.app to javafx.fxml;
    exports com.app;
}