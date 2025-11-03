package lk.ijes.groupchat.Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class ServerController implements Initializable {
    public ImageView pfp;
    public ScrollPane scrollPane;
    public TextField msgInput;
    public Button sendBtn;
    public VBox chatDisplay;
    public Button imageUpload;
    public Label clientCountLabel;
    public Button stopServerBtn;

    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private boolean isServerRunning = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startServer();
    }

    private void startServer() {
        if (isServerRunning) {
            showAlert("Server is already running!");
            return;
        }

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(3000);
                isServerRunning = true;
                System.out.println("Group Chat Server started. Waiting for clients...");
                Platform.runLater(() -> displaySystemMessage("Server started on port 3000"));

                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();

                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    String username = dis.readUTF();

                    ClientHandler clientHandler = new ClientHandler(clientSocket, username);
                    clients.put(username, clientHandler);

                    Platform.runLater(() -> {
                        clientCountLabel.setText("Connected Clients: " + clients.size());
                        displaySystemMessage(username + " joined the chat");
                    });

                    new Thread(clientHandler).start();
                }
            } catch (IOException e) {
                if (!isServerRunning) {
                    return;
                }
                e.printStackTrace();
                Platform.runLater(() -> displaySystemMessage("Server error: " + e.getMessage()));
            }
        }).start();
    }

    public void sendMsg(ActionEvent actionEvent) {
        try {
            String message = msgInput.getText().trim();
            if (!message.isEmpty()) {
                String serverMessage = "Server: " + message;
                broadcastMessage("TEXT", serverMessage, null, "server");
                displayMsg(message, "server");
                msgInput.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error sending message: " + e.getMessage());
        }
    }

    private void displayMsg(String inputMsg, String sender) {
        HBox bubble = new HBox();
        Label msg = new Label(inputMsg);
        msg.setWrapText(true);

        if (sender.equals("server")) {
            msg.setStyle("-fx-background-color: #34c759; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15;");
            bubble.setAlignment(Pos.BASELINE_RIGHT);
            bubble.getChildren().add(msg);
        } else {
            // For client messages on server, show username and message
            HBox messageWithSender = new HBox();
            messageWithSender.setSpacing(5);
            messageWithSender.setAlignment(Pos.CENTER_LEFT);

            Label senderLabel = new Label(sender + ":");
            senderLabel.setStyle("-fx-text-fill: #666; -fx-font-weight: bold; -fx-padding: 0 5 0 0;");

            Label messageLabel = new Label(inputMsg);
            messageLabel.setWrapText(true);
            messageLabel.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15;");

            messageWithSender.getChildren().addAll(senderLabel, messageLabel);
            bubble.getChildren().add(messageWithSender);
            bubble.setAlignment(Pos.BASELINE_LEFT);
        }

        chatDisplay.getChildren().add(bubble);
        scrollPane.setVvalue(1.0);
    }

    private void displayImage(Image image, String sender) {
        HBox bubble = new HBox();
        VBox imageContainer = new VBox();
        imageContainer.setSpacing(5);

        ImageView imgView = new ImageView(image);
        imgView.setFitWidth(200);
        imgView.setPreserveRatio(true);

        Label senderLabel = new Label(sender.equals("server") ? "Server" : sender);
        senderLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px; -fx-font-weight: bold;");
        imageContainer.getChildren().addAll(senderLabel, imgView);

        bubble.getChildren().add(imageContainer);
        bubble.setAlignment(sender.equals("server") ? Pos.BASELINE_RIGHT : Pos.BASELINE_LEFT);

        chatDisplay.getChildren().add(bubble);
        scrollPane.setVvalue(1.0);
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image to Send");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            try {
                BufferedImage bufferedImage = ImageIO.read(selectedFile);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();

                broadcastMessage("IMAGE", "Server sent an image", imageBytes, "server");

                Image fxImage = new Image(new ByteArrayInputStream(imageBytes));
                displayImage(fxImage, "server");

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error uploading image: " + e.getMessage());
            }
        } else {
            showAlert("No file selected.");
        }
    }

    private void broadcastMessage(String type, String message, byte[] imageBytes, String sender) throws IOException {
        for (ClientHandler client : clients.values()) {
            if (client.isConnected()) {
                client.sendMessage(type, message, imageBytes);
            }
        }
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
            alert.show();
        });
    }

    public void stopServer() {
        try {
            isServerRunning = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler client : clients.values()) {
                client.disconnect();
            }
            clients.clear();
            Platform.runLater(() -> {
                clientCountLabel.setText("Connected Clients: 0");
                displaySystemMessage("Server stopped");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataOutputStream dOS;
        private DataInputStream dIS;
        private String username;

        public ClientHandler(Socket socket, String username) {
            this.socket = socket;
            this.username = username;

            try {
                this.dOS = new DataOutputStream(socket.getOutputStream());
                this.dIS = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (!socket.isClosed() && isConnected()) {
                    try {
                        String type = dIS.readUTF();

                        if (type.equals("TEXT")) {
                            String message = dIS.readUTF();
                            String formattedMessage = username + ": " + message;

                            for (ClientHandler client : clients.values()) {
                                if (!client.username.equals(this.username) && client.isConnected()) {
                                    client.sendMessage("TEXT", formattedMessage, null);
                                }
                            }

                            Platform.runLater(() -> displayMsg(message, username));

                        } else if (type.equals("IMAGE")) {
                            int size = dIS.readInt();
                            byte[] imageBytes = new byte[size];
                            dIS.readFully(imageBytes);

                            for (ClientHandler client : clients.values()) {
                                if (!client.username.equals(this.username) && client.isConnected()) {
                                    client.sendMessage("IMAGE", username + " sent an image", imageBytes);
                                }
                            }

                            InputStream in = new ByteArrayInputStream(imageBytes);
                            Image image = new Image(in);
                            Platform.runLater(() -> displayImage(image, username));
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        public void sendMessage(String type, String message, byte[] imageBytes) throws IOException {
            if (isConnected()) {
                dOS.writeUTF(type);
                if (type.equals("TEXT")) {
                    dOS.writeUTF(message);
                } else if (type.equals("IMAGE")) {
                    dOS.writeInt(imageBytes.length);
                    dOS.write(imageBytes);
                }
                dOS.flush();
            }
        }

        public boolean isConnected() {
            return socket != null && !socket.isClosed() && socket.isConnected();
        }

        private void disconnect() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                clients.remove(username);
                Platform.runLater(() -> {
                    clientCountLabel.setText("Connected Clients: " + clients.size());
                    displaySystemMessage(username + " left the chat");
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        stopServer();
    }
}