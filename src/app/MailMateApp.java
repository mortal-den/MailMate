package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.MailMateController;

public class MailMateApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/main.fxml"
                        )
                );

        Scene scene =
                new Scene(
                        loader.load(),
                        1250,
                        760
                );

        MailMateController controller =
                loader.getController();

        controller.startApplication();

        stage.setTitle("MailMate");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        stage.show();
    }

    @Override
    public void stop() {

        // The controller handles MailService cleanup.
    }

    public static void main(String[] args) {
        launch(args);
    }
}