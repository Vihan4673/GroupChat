module lk.ijes.groupchat {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens lk.ijes.groupchat to javafx.fxml;
    exports lk.ijes.groupchat;
}