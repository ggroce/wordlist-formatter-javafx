package org.wordlistformatter;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class PrimaryController implements Initializable {
    @FXML private TextField textFieldFileOutput = new TextField();;
    @FXML private TextField textFieldMaxLineLength = new TextField();
    @FXML private CheckBox checkBoxSortLines;
    @FXML private CheckBox checkBoxRemoveNonAscii;
    @FXML private CheckBox checkBoxSetMaxLineLength;
    @FXML private TableView<WordListFile> tableViewWordListFiles;
    @FXML private TableColumn<WordListFile, String> columnWordListFilePath;
    @FXML private TableColumn<WordListFile, String> columnWordListFileSize;
    private Desktop desktop = Desktop.getDesktop();
    private ObservableList<WordListFile> wordListFiles = FXCollections.observableArrayList();
    private Set<String> filePathSet = new HashSet<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        columnWordListFilePath.setCellValueFactory(new PropertyValueFactory<WordListFile, String>("filePath"));
        columnWordListFileSize.setCellValueFactory(new PropertyValueFactory<WordListFile, String>("fileSizeStr"));
        textFieldMaxLineLength.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textFieldMaxLineLength.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    @FXML
    public void addWordlist() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add file");
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            String filePath = file.getAbsolutePath();
            Float fileSize = (float) (file.length() / 1024.0);
            WordListFile wordListFile = new WordListFile(filePath, fileSize);

            int indexOfDot = file.getName().lastIndexOf('.');
            if (indexOfDot > 0) {
                String fileExtension = file.getName().substring(indexOfDot + 1);
                System.out.println(fileExtension);
                if (!fileExtension.equals("txt")) {
                    showErrorDialog("File does not have a .txt extension and may not contain a word list.");
                }
            }

            if (filePathSet.contains(filePath)) {
                showErrorDialog("File already added to list.");
            } else {
                wordListFiles.add(wordListFile);
                filePathSet.add(filePath);
                tableViewWordListFiles.setItems(wordListFiles);
            }
        }
    }

    @FXML
    public void removeWordlist() {
        WordListFile wordListFile = tableViewWordListFiles.getSelectionModel().getSelectedItem();
        if (wordListFile != null) {
            wordListFiles.remove(wordListFile);
        }
    }

    @FXML
    public void selectOutputLocation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save to");
        fileChooser.setInitialFileName("Wordlist.txt");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt"));
        File file = fileChooser.showSaveDialog(new Stage());
        if (file != null) {
            String outputPath = file.getAbsolutePath();
            textFieldFileOutput.setText(outputPath);
        }
    }

    @FXML
    public void selectMaxLineLength() {
        if (checkBoxSetMaxLineLength.isSelected()) {
            textFieldMaxLineLength.setDisable(false);
        } else {
            textFieldMaxLineLength.clear();
            textFieldMaxLineLength.setDisable(true);
        }
    }

    @FXML
    public void onStartSelected() {
        if (textFieldFileOutput.getText().isEmpty()) {
            //TODO: do some sort of filepath check here.  Just tryto save file?
            showErrorDialog("Select output path before continuing.");
            return;
        } else if (wordListFiles.isEmpty()) {
            showErrorDialog("Add files to be processed.");
            return;
        }

        //combine files before running any selected routines
        combineFiles();

        if (checkBoxSortLines.isSelected()) {
            sortByStringSize();
        }
        if (checkBoxRemoveNonAscii.isSelected()) {
            removeNonAscii();
        }
        if (checkBoxSetMaxLineLength.isSelected()) {
            if (textFieldMaxLineLength.getText().isEmpty() || Integer.parseInt(textFieldMaxLineLength.getText()) < 1) {
                showErrorDialog("If selecting maximum line length attribute, input desired " +
                        "maximum line length.");
                return;
            } else {
                setMaxLineLength();
            }
        }
    }

    private void combineFiles() {
        //TODO: implement combine (append) files
        for (WordListFile wordListFile : wordListFiles) {

        }
    }

    private void setMaxLineLength() {
        //TODO: implement removal of lines longer than N
    }

    private void removeNonAscii() {
        //TODO: implement removal of non-ascii characters
    }

    private void sortByStringSize() {
        ArrayList<String> wordList = new ArrayList<>();
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            //TODO: gets file from output location after files have been appended

            reader = new BufferedReader(new FileReader(textFieldFileOutput.getText()));
            String string = reader.readLine();

            Long startTime = System.nanoTime();
            while (string != null) {
                wordList.add(string);
                string = reader.readLine();
            }
            Collections.sort(wordList, Comparator.comparingInt(String::length));
//            Collections.sort(wordList, (a, b)->Integer.compare(a.length(), b.length()));

            Long endTime = System.nanoTime();
            System.out.println("Execution time: " + (endTime - startTime) + " ns. ");

            //TODO: need to properly setup output file/location
            writer = new BufferedWriter(new FileWriter(textFieldFileOutput.getText()));
            for (String line : wordList) {
                writer.write(line);
                writer.newLine();
            }
        }
        catch (IOException e) { e.printStackTrace(); }
        finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if(writer != null) {
                    writer.close();
                }
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void showErrorDialog(String errorText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Error");
        alert.setHeaderText("Error on Input");
        alert.setContentText(errorText);
        alert.showAndWait();
    }

    @FXML
    private void switchToSecondary() throws IOException {
//        App.setRoot("secondary");
    }
}
