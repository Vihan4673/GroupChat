package lk.ijes.groupchat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GroupChat extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primarystage) throws Exception {
        startServer();
        startClient(1);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                javafx.application.Platform.runLater(() -> {
                    try {
                        startClient(2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startServer() throws Exception {
        javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/groupchat/interfaces/ServerForm.fxml"));
        Stage stage = new Stage();
        stage.setTitle("Group Chat Server");
        stage.setScene(new javafx.scene.Scene(root));
        stage.setWidth(420);
        stage.setHeight(650);
        stage.setX(50);
        stage.setY(50);
        stage.setResizable(false);
        stage.show();
    }

    private void startClient(int clientNumber) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/groupchat/interfaces/ClientForm.fxml"));
        Stage stage = new Stage();
        stage.setTitle("Group Chat Client " + clientNumber);
        stage.setScene(new Scene(root));
        stage.setWidth(420);
        stage.setHeight(650);
        stage.setX(500 + ((clientNumber - 1) * 450));
        stage.setY(50);
        stage.setResizable(false);
        stage.show();
    }
}
