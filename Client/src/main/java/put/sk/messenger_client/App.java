package put.sk.messenger_client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import put.sk.messenger_client.controllers.MainController;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class App extends Application {
    public static Stage stg;
    public static Parameters params;
    public static List<String> list;
    @Override
    public void start(Stage stage) throws IOException {
        this.stg = stage;
        this.params = getParameters();
        this.list = params.getRaw();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        AnchorPane anchorPane = loader.load();
        MainController controller = loader.getController();
        System.out.println(list.get(0));
        System.out.println(list.get(1));
        controller.setAddress(list.get(0));
        controller.setPort(Integer.parseInt(list.get(1)));
        stage.setScene(new Scene(anchorPane, 800, 600));
        stage.show();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {

                Platform.exit();
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}