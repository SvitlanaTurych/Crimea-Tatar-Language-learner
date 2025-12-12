package qirim.app;

import java.io.IOException;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml"));
        Image icon = new Image((Objects.requireNonNull(getClass().getResource("/qirim/app/images/icon.jpg"))).toExternalForm());
        Scene scene = new Scene(fxmlLoader.load(), 400, 400);

        primaryStage.getIcons().add(icon);
        primaryStage.setTitle("QIrIm");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMaximized(true);
    }

}