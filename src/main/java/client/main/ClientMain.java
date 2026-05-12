package client.main;

import client.network.ClientTCP;
import client.ui.AuthView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class ClientMain extends Application {
    private ClientTCP clientTCP;

    @Override
    public void start(Stage primaryStage) {
        String serverAddress = "127.0.0.1";
        int port = 8081;

        clientTCP = new ClientTCP(serverAddress, port);

        System.out.println("Connecting to server " + serverAddress + ":" + port + "...");
        if (!clientTCP.connect()) {
            System.err.println("Could not connect to the server.");

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText("Server unavailable");
            alert.setContentText("Could not connect to the server on " + serverAddress + ":" + port
                    + "\n\nStart ServerMain first, then run the client.");
            alert.showAndWait();

            return; // stop here, do NOT open AuthView
        }

        primaryStage.setTitle("ChriOnline Tech");

        AuthView authView = new AuthView(clientTCP, primaryStage);
        Scene scene = new Scene(authView.getView(), 1180, 780);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (clientTCP != null) {
            clientTCP.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
