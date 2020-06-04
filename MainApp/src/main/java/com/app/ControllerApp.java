package com.app;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.ResourceBundle;

public class ControllerApp implements Initializable {

    private String current_path = "/";
    private String selected = "";

    @FXML
    public ListView<String> dir_list;
    @FXML
    public Button button_back;
    @FXML
    public Button button_upload;
    @FXML
    public TextArea result_out;
    @FXML
    public AnchorPane anchor_pane;
    @FXML
    public Button button_create_folder;
    @FXML
    public Button button_delete;
    @FXML
    public Button button_download;


    @FXML public void handleMouseClick(MouseEvent arg0) {
        System.out.println("clicked on " + dir_list.getSelectionModel().getSelectedItem());
    }


    ObservableList<String> dirs = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {



        String[] dirs_from_server = ControllerLogin.client.getDirs("/");

        Collections.addAll(dirs, dirs_from_server);

        dir_list.setItems(dirs);

        button_back.setOnAction(e ->{
            selected = "";
            if(current_path.equals("/")){
                System.out.println("curr path" + current_path);
                return;
            }else{

                current_path = current_path.substring(0, current_path.length() - 1);
                int index = current_path.lastIndexOf("/");
                current_path = current_path.substring(0, index +1);
                update_list();
            }
        });


        button_upload.setOnAction(e ->{
            selected = "";
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Pick file to upload");

            Stage stage = (Stage) anchor_pane.getScene().getWindow();

            File file = fileChooser.showOpenDialog(stage);

            if(file != null) {

                ControllerLogin.client.upload_range(file.getPath(), current_path + file.getName());
                System.out.println("path to server " + current_path+file.getName());
                update_list();
            }
        });

        button_create_folder.setOnAction(e -> {
            selected = "";
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.setTitle("Make folder");
            textInputDialog.getDialogPane().setContentText("Folder name:");
            Optional<String> result = textInputDialog.showAndWait();
            TextField input = textInputDialog.getEditor();

            if(input.getText() != null && input.getText().length() != 0){
                String name = input.getText();
                if(name.contains(".")){
                    Alert a = new Alert(Alert.AlertType.WARNING);
                    a.setContentText("Cannot create folder with \'.\' in name");
                    a.show();
                    return;
                }
                ControllerLogin.client.mkdir(current_path + name);
                update_list();
            }else {
                System.out.println("No text entered");
            }
        });

        button_download.setOnAction(e ->{
            if(!selected.equals("")){
                final DirectoryChooser directoryChooser = new DirectoryChooser();

                Stage stage = (Stage) anchor_pane.getScene().getWindow();
                File file = directoryChooser.showDialog(stage);

                if(file != null){
                    System.out.println("curr_path " + current_path);
                    System.out.println("localpath " + file.getPath());

                    ControllerLogin.client.send_range(current_path + selected,
                            file.getAbsolutePath() + "/" + selected);

                    selected = "";
                }
            }else{
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setContentText("No file selected!");
                a.show();
            }
        });

        button_delete.setOnAction(e -> {
            if(selected.equals("")){
                Alert alert = new Alert(Alert.AlertType.WARNING,
                                "Delete current folder with all components?",
                                ButtonType.OK,
                                ButtonType.CANCEL);
                alert.setTitle("Delete warning");
                Optional<ButtonType> result = alert.showAndWait();

                if (result.get() == ButtonType.OK) {
                    ControllerLogin.client.delete(current_path, "");

                    if(current_path.equals("/")){
                        update_list();
                        return;
                    }else{

                        current_path = current_path.substring(0, current_path.length() - 1);
                        int index = current_path.lastIndexOf("/");
                        current_path = current_path.substring(0, index +1);
                        update_list();
                    }
                }
            }
            else{
                ControllerLogin.client.delete(current_path, selected);
                update_list();
                selected = "";

            }

        });

        dir_list.setOnMouseClicked(new EventHandler<MouseEvent>(){

            @Override
            public void handle(MouseEvent event) {
                String temp = dir_list.getSelectionModel().getSelectedItem();
                if(temp!= null ) {
                    if (temp.contains(".")) {
                        selected = temp;
                        System.out.println("clicked on " + temp);

                    } else {
                        selected = "";
                        current_path += temp + "/";
                        System.out.println(current_path);

                        update_list();
                    }
                }
            }
        });

    }

    private void update_list(){
        System.out.println("get dirs curr path: " + current_path);
        String[] dirs_from_server = ControllerLogin.client.getDirs(current_path);
        dirs.setAll(dirs_from_server);
        dir_list.setItems(dirs);
        result_out.setText(current_path);
    }

}
