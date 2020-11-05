package org.wordlistformatter;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class PrimaryController implements Initializable {
    @FXML private TextField textFieldFileOutput = new TextField();
    @FXML private TextField textFieldMaxLineLength = new TextField();
    @FXML private CheckBox checkBoxSortLines;
    @FXML private CheckBox checkBoxRemoveNonAscii;
    @FXML private CheckBox checkBoxSetMaxLineLength;
    @FXML private TableView<WordListFile> tableViewWordListFiles;
    @FXML private TableColumn<WordListFile, String> columnWordListFilePath;
    @FXML private TableColumn<WordListFile, String> columnWordListFileSize;
    @FXML private Label labelStatus;
    @FXML private ProgressBar progressBar;
    private ObservableList<WordListFile> wordListFiles = FXCollections.observableArrayList();
    private Set<String> filePathSet = new HashSet<>();
    private File lastUsedDirectory;
    FileFormatter fileFormatter = new FileFormatter();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        columnWordListFilePath.setCellValueFactory(new PropertyValueFactory<WordListFile, String>("filePath"));
        columnWordListFileSize.setCellValueFactory(new PropertyValueFactory<WordListFile, String>("fileSizeStr"));
        textFieldMaxLineLength.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textFieldMaxLineLength.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        //TODO: remove these as implemented
        checkBoxRemoveNonAscii.setDisable(true);
        progressBar.setVisible(false);
    }

    @FXML
    public void addWordlist() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add file");
        File file = null;
        if (lastUsedDirectory != null) {
            fileChooser.setInitialDirectory(lastUsedDirectory);
            file = fileChooser.showOpenDialog(null);
        } else {
            file = fileChooser.showOpenDialog(null);
        }
        if (file != null) {
            lastUsedDirectory = file.getParentFile();
            String filePath = file.getAbsolutePath();
            WordListFile wordListFile = new WordListFile(file);

            int indexOfDot = file.getName().lastIndexOf('.');
            if (indexOfDot > 0) {
                String fileExtension = file.getName().substring(indexOfDot + 1);
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
        fileChooser.setInitialFileName("OptimizedWordlist.txt");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("TXT files (*.txt)",
                "*.txt"));
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
            showErrorDialog("Select output path before continuing.");
            return;
        } else if (wordListFiles.isEmpty()) {
            showErrorDialog("Add files to be processed.");
            return;
        } else {
            try {
                Path outputFile = Paths.get(textFieldFileOutput.getText());
                Files.createFile(outputFile);
            } catch (IOException e) {
                showErrorDialog("Output file already exists.  Please choose a " +
                        "different file name or save location.");
                e.printStackTrace();
                return;
            }
        }

        saveOutFiles();

//        if (checkBoxRemoveNonAscii.isSelected()) {
//            labelStatus.setText("Removing non-Ascii lines");
//            //NEW THREAD
//            fileFormatter.removeNonAscii(new File(textFieldFileOutput.getText()));
//        }
//        labelStatus.setText("");
    }

    private void saveOutFiles() {
        if (wordListFiles.size() > 1) {
            labelStatus.setText("Combining files...");
        }else {
            labelStatus.setText("Preparing file...");
        }
        Task<Void> combineFiles = new Task<Void>() {
            @Override
            public Void call() {
                fileFormatter.combineFiles(wordListFiles, new File(textFieldFileOutput.getText()));
                return null;
            }
        };
        combineFiles.setOnSucceeded(e -> {
            if (wordListFiles.size() > 1) {
                labelStatus.setText("Combining files completed");
            }else {
                labelStatus.setText("Preparing file completed");
            }
            checkSortLines();
        });
        new Thread(combineFiles).start();
    }

    private void checkSortLines() {
        if (checkBoxSortLines.isSelected()) {
            labelStatus.setText("Sorting lines by string length");
            Task<Void> sortLines = new Task<Void>() {
                @Override
                public Void call() {
                    fileFormatter.sortByStringSize(new File(textFieldFileOutput.getText()));
                    return null;
                }
            };
            sortLines.setOnSucceeded(e -> {
                labelStatus.setText("Line sort completed");
                checkMaxLineLength();
            });
            new Thread(sortLines).start();
        }else {
            checkMaxLineLength();
        }
    }

    private void checkMaxLineLength() {
        //TODO: conditional logic needs help in order to place else onto end and continue
        if (checkBoxSetMaxLineLength.isSelected()) {
            if (textFieldMaxLineLength.getText().isEmpty()
                    || Integer.parseInt(textFieldMaxLineLength.getText()) < 1) {
                showErrorDialog("If selecting maximum line length attribute, input desired " +
                        "maximum line length.");
            } else {
                labelStatus.setText("Removing lines greater than " +
                        Integer.parseInt(textFieldMaxLineLength.getText()));
                Task<Void> setMaxLineLength = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        fileFormatter.setMaxLineLength(Integer.parseInt(textFieldMaxLineLength.getText()),
                                new File(textFieldFileOutput.getText()));
                        return null;
                    }
                };
                setMaxLineLength.setOnSucceeded(e -> {
                    labelStatus.setText("Lines removed");
                    //TODO: next method here
                });
                new Thread(setMaxLineLength).start();
            }
        }
    }

    private void showErrorDialog(String errorText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Error");
        alert.setHeaderText("Error on Input");
        alert.setContentText(errorText);
        alert.showAndWait();
    }
}
