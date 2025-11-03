package lk.ijes.groupchat.Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    public ImageView pfp;
    public ScrollPane scrollPane;
    public TextField msgInput;
    public Button sendBtn;
    public VBox chatDisplay;
    public Button imageUploader;
    public TextField usernameField;
    public Button connectBtn;
    public Button disconnectBtn;

    private Socket socket;
    private DataOutputStream dOS;
    private DataInputStream dIS;
    private String username = "";
    private boolean isConnected = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        setChatControlsEnabled(false);
        disconnectBtn.setDisable(true);
    }

    public void connectToServer(ActionEvent actionEvent) {
        if (isConnected) {
            showAlert("Already connected to server!");
            return;
        }

        username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showAlert("Please enter a username!");
            return;
        }

        new Thread(() -> {
            try {
                socket = new Socket("localhost", 3000);
                dOS = new DataOutputStream(socket.getOutputStream());
                dIS = new DataInputStream(socket.getInputStream());

                dOS.writeUTF(username);
                dOS.flush();

                isConnected = true;

                Platform.runLater(() -> {
                    setChatControlsEnabled(true);
                    connectBtn.setDisable(true);
                    disconnectBtn.setDisable(false);
                    usernameField.setDisable(true);
                    displaySystemMessage("Connected to server as: " + username);
                });

                while (isConnected && !socket.isClosed()) {
                    try {
                        String type = dIS.readUTF();

                        if (type.equals("TEXT")) {
                            String message = dIS.readUTF();
                            Platform.runLater(() -> displayMsg(message, getSenderType(message)));
                        } else if (type.equals("IMAGE")) {
                            int size = dIS.readInt();
                            byte[] imageBytes = new byte[size];
                            dIS.readFully(imageBytes);

                            String imageDescription = dIS.readUTF();
                            InputStream in = new ByteArrayInputStream(imageBytes);
                            Image image = new Image(in);
                            Platform.runLater(() -> displayImage(image, getSenderType(imageDescription)));
                        }
                    } catch (IOException e) {
                        if (isConnected) {
                            Platform.runLater(() -> displaySystemMessage("Disconnected from server"));
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showAlert("Cannot connect to server. Make sure server is running.");
                    displaySystemMessage("Connection failed");
                });
            }
        }).start();
    }

    public void disconnectFromServer(ActionEvent actionEvent) {
        if (!isConnected) {
            showAlert("Not connected to server!");
            return;
        }

        try {
            isConnected = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            Platform.runLater(() -> {
                setChatControlsEnabled(false);
                connectBtn.setDisable(false);
                disconnectBtn.setDisable(true);
                usernameField.setDisable(false);
                displaySystemMessage("Disconnected from server");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setChatControlsEnabled(boolean enabled) {
        msgInput.setDisable(!enabled);
        sendBtn.setDisable(!enabled);
        imageUploader.setDisable(!enabled);
    }

    private String getSenderType(String message) {
        if (message.startsWith(username + ":")) {
            return "client";
        } else if (message.startsWith("Server:")) {
            return "server";
        } else {
            return "other";
        }
    }

    public void sendMsg(ActionEvent actionEvent) {
        if (!isConnected) {
            showAlert("Not connected to server!");
            return;
        }

        try {
            String message = msgInput.getText().trim();
            if (!message.isEmpty()) {
                dOS.writeUTF("TEXT");
                dOS.writeUTF(message);
                dOS.flush();

                displayMsg(message, "client");
                msgInput.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showConnectionError();
        }
    }

    private void displayMsg(String inputMsg, String sender) {
        HBox bubble = new HBox();
        bubble.setSpacing(10);
        bubble.setMaxWidth(Double.MAX_VALUE);

        Label msg = new Label(inputMsg);
        msg.setWrapText(true);
        msg.setMaxWidth(280);

        switch (sender) {
            case "client":
                msg.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15;");
                bubble.setAlignment(Pos.CENTER_RIGHT);

                HBox.setMargin(msg, new Insets(5, 20, 5, 300));
                bubble.getChildren().add(msg);
                break;

            case "server":
                msg.setStyle("-fx-background-color: #34c759; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15;");
                bubble.setAlignment(Pos.CENTER_LEFT);
                HBox.setMargin(msg, new Insets(5, 200, 5, 20));
                bubble.getChildren().add(msg);
                break;

            case "other":
                String[] parts = inputMsg.split(":", 2);
                if (parts.length == 2) {
                    VBox messageContainer = new VBox(2);
                    messageContainer.setMaxWidth(280);

                    Label senderLabel = new Label(parts[0]);
                    senderLabel.setStyle("-fx-text-fill: #666; -fx-font-weight: bold; -fx-font-size: 12px;");

                    Label messageLabel = new Label(parts[1].trim());
                    messageLabel.setWrapText(true);
                    messageLabel.setStyle("-fx-background-color: #e5e5ea; -fx-text-fill: black; -fx-padding: 8 12; -fx-background-radius: 15;");

                    messageContainer.getChildren().addAll(senderLabel, messageLabel);
                    bubble.getChildren().add(messageContainer);
                    HBox.setMargin(messageContainer, new javafx.geometry.Insets(5, 200, 5, 20));
                } else {
                    msg.setStyle("-fx-background-color: #e5e5ea; -fx-text-fill: black; -fx-padding: 8 12; -fx-background-radius: 15;");
                    bubble.getChildren().add(msg);
                    HBox.setMargin(msg, new javafx.geometry.Insets(5, 200, 5, 20));
                }
                bubble.setAlignment(Pos.CENTER_LEFT);
                break;
        }

        chatDisplay.getChildren().add(bubble);
        scrollPane.setVvalue(1.0);
    }

    private void displayImage(Image image, String sender) {
        HBox bubble = new HBox();
        bubble.setSpacing(10);

        VBox imageContainer = new VBox();
        imageContainer.setSpacing(5);

        ImageView imgView = new ImageView(image);
        imgView.setFitWidth(200);
        imgView.setPreserveRatio(true);

        String senderName = getImageSenderName(sender);
        Label senderLabel = new Label(senderName);
        senderLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px; -fx-font-weight: bold;");
        imageContainer.getChildren().addAll(senderLabel, imgView);

        bubble.getChildren().add(imageContainer);

        if (sender.equals("client")) {
            bubble.setAlignment(Pos.BASELINE_RIGHT);
        } else {
            bubble.setAlignment(Pos.BASELINE_LEFT);
        }

        chatDisplay.getChildren().add(bubble);
        scrollPane.setVvalue(1.0);
    }

    private String getImageSenderName(String senderType) {
        switch (senderType) {
            case "client": return "You";
            case "server": return "Server";
            case "other": return "Other User";
            default: return "Unknown";
        }
    }

    private void displaySystemMessage(String message) {
        HBox bubble = new HBox();
        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15; -fx-font-style: italic;");
        bubble.getChildren().add(msg);
        bubble.setAlignment(Pos.CENTER);
        chatDisplay.getChildren().add(bubble);
        scrollPane.setVvalue(1.0);
    }

    public void imageUploadOnAction(javafx.event.ActionEvent actionEvent) {
        if (!isConnected) {
            showAlert("Not connected to server!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image to Send");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            try {
                BufferedImage bufferedImage = ImageIO.read(selectedFile);
                if (bufferedImage == null) {
                    showAlert("Invalid image file");
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                String format = getImageFormat(selectedFile.getName());
                ImageIO.write(bufferedImage, format, baos);
                byte[] imageBytes = baos.toByteArray();

                dOS.writeUTF("IMAGE");
                dOS.writeInt(imageBytes.length);
                dOS.write(imageBytes);
                dOS.flush();

                Image fxImage = new Image(new ByteArrayInputStream(imageBytes));
                displayImage(fxImage, "client");

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error uploading image: " + e.getMessage());
            }
        } else {
            showAlert("No file selected.");
        }
    }

    private String getImageFormat(String fileName) {
        if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return "jpg";
        } else if (fileName.toLowerCase().endsWith(".gif")) {
            return "gif";
        } else {
            return "png";
        }
    }

    private void showConnectionError() {
        showAlert("Not connected to server. Please check connection.");
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
            alert.show();
        });
    }

    public void shutdown() {
        if (isConnected) {
            try {
                isConnected = false;
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}