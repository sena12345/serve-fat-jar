package controllers;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML
    private ListView<String> listView;
    @FXML
    private TextArea textArea;
    @FXML
    private Label machineName, currentIP,selected,status;
    Task<Void> task = null;
    private final String BASE_DIR = "host/";
    ObservableList<String> fileList;
    public void initialize(URL location, ResourceBundle resources) {
        this.init();

    }

    private void init() {
        try {
            machineName.setText("Machine name: ".concat(InetAddress.getLocalHost().getHostName()));
            currentIP.setText("IP Address: ".concat(InetAddress.getLocalHost().getHostAddress()));
            this.createHostFolder();//create host folder...
            fileList = FXCollections.observableArrayList();
            listView.setItems(fileList);
            getFiles();

            listView.setOnMouseClicked(e->{
                String selected = listView.getSelectionModel().getSelectedItem();
                this.selected.setText(selected);
            });

        } catch (Exception e) {
            textArea.setText(e.getLocalizedMessage());
        }
    }

    private void getFiles(){
        fileList.clear();
        File hostDir = new File(BASE_DIR);
        File[] list = hostDir.listFiles();
        for (File file : list) {
            if (file.getName().endsWith(".jar"))
                fileList.add(file.getName());
        }

    }

    private void createHostFolder() {
        File dir = new File("host");
        if (!dir.isDirectory()) {
            if (dir.mkdir()) {
                System.out.println("directory created...");
            }
        }
    }


    @FXML
    public void startServer(ActionEvent e) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(selected.getText().replace(".jar",".bat"));
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        if (task != null)
            task.cancel(true);
        task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                String line;
                while (true){
                    if ((line = reader.readLine()) != null)
                        updateMessage(line);
                }
            }

            @Override
            protected void done() {
                super.done();
                System.out.println("Complete");
                process.destroy();
                textArea.textProperty().unbind();
            }
        };
        task.messageProperty().addListener((observable, oldValue, newValue) -> textArea.appendText(newValue));
        new Thread(task).start();




    }

    @FXML
    public void importJar(ActionEvent e) throws IOException {

        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("jar", "*.jar");
        chooser.getExtensionFilters().add(filter);
        File jarFile = chooser.showOpenDialog(this.listView.getScene().getWindow());
        if (jarFile != null) {
            File file = new File(BASE_DIR);
            long len = file.listFiles().length + 1;
            final String rename = "_"+len+jarFile.getName();
                FileUtils.copyFile(jarFile,new File(BASE_DIR+rename));
            createBat(rename.replace(".jar",".bat"));
            System.out.println("imported jar");
            this.getFiles();//reload files
        }


    }


    private Boolean createBat(String name) throws FileNotFoundException {
        File file = new File(name);
        PrintWriter printWriter = new PrintWriter(file);
        printWriter.write("java -jar "+BASE_DIR.concat(name.replace(".bat",".jar")));
        printWriter.flush();
        printWriter.close();
        return printWriter.checkError();
    }



}


