package com.app;

import com.socket.Client;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ControllerLogin implements Initializable {

    @FXML
    public TextField text_username;
    @FXML
    public TextField text_password;
    @FXML
    public Label lbl_status;

    public static Client client = new Client();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }


    public void login(ActionEvent event)throws Exception{
        //Client client = new Client();
        //String username = "matsledz1";
        //String password = "Mateusz123";

        String username = text_username.getText();
        String password = text_password.getText();

        boolean result = client.authorize(username, password);

        if(result){
            lbl_status.setText("Login Succesful");

            URL url = new File("src/main/resources/fxml/scene.fxml").toURI().toURL();
            Parent root1 = FXMLLoader.load(url);

            Stage stage = new Stage();
            stage.onCloseRequestProperty().setValue(e -> Platform.exit());

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("CloudX");
            stage.setScene(new Scene(root1));
            stage.show();

        }
        else{
            lbl_status.setText("Login Failed");
        }

    }
}
