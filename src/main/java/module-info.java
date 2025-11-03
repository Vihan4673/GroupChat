module lk.ijes.groupchat {
    requires javafx.controls;
    requires javafx.fxml;


    opens lk.ijes.groupchat to javafx.fxml;
    exports lk.ijes.groupchat;
}