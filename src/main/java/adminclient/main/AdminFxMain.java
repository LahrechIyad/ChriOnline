package adminclient.main;

import adminclient.network.AdminClientHandler;
import adminclient.ui.AdminAuthView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class AdminFxMain extends Application {
    private AdminClientHandler clientHandler;

    @Override
    public void start(Stage stage) {
        clientHandler = new AdminClientHandler("127.0.0.1", 8081);
        try {
            clientHandler.connect();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText("Server unavailable");
            alert.setContentText("Could not connect to the server on 127.0.0.1:8081");
            alert.showAndWait();
            return;
        }

        AdminAuthView authView = new AdminAuthView(clientHandler, stage);
        Scene scene = new Scene(authView.getView(), 1220, 820);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        stage.setTitle("ChriOnline Admin");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (clientHandler != null) {
            clientHandler.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
