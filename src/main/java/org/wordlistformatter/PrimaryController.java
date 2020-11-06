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

public class PrimaryController implements Initializable {
    @FXML private TextField textFieldFileOutput = new TextField();
    @FXML private TextField textFieldMaxLineLength = new TextField();
    @FXML private CheckBox checkBoxSortLines;
    @FXML private CheckBox checkBoxRemoveNonAscii;
    @FXML private CheckBox checkBoxSetMaxLineLength;
    @FXML private TableView<WordListFile> tableViewWordListFiles;
    @FXML private TableColumn<WordListFile, String> columnWordListFilePath;
    @FXML private TableColumn<WordListFile, String> columnWordListFileSize;
    @FXML private Button buttonOuputLocation;
    @FXML private Button buttonAddWordlist;
    @FXML private Button buttonRemoveWordlist;
    @FXML private Button buttonStart;
    @FXML private Label labelStatus;
    @FXML private Label labelResult;
    @FXML private ProgressBar progressBar;
    private ObservableList<WordListFile> wordListFiles = FXCollections.observableArrayList();
    private Set<String> filePathSet = new HashSet<>();
    private File lastUsedDirectory;
    private File outputDirectory;
    private FileFormatter fileFormatter = new FileFormatter();

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
    private void addWordlist() {
        File file = null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add file");

        if (lastUsedDirectory != null) {
            fileChooser.setInitialDirectory(lastUsedDirectory);
        }
        file = fileChooser.showOpenDialog(null);

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
    private void removeWordlist() {
        WordListFile wordListFile = tableViewWordListFiles.getSelectionModel().getSelectedItem();
        if (wordListFile != null) {
            wordListFiles.remove(wordListFile);
            filePathSet.remove(wordListFile.getFilePath());
        }
    }

    @FXML
    private void selectOutputLocation() {
        File file = null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save to");
        fileChooser.setInitialFileName("OptimizedWordlist.txt");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("TXT files (*.txt)",
                "*.txt"));

        if (outputDirectory != null) {
            fileChooser.setInitialDirectory(outputDirectory);
        }
        file = fileChooser.showSaveDialog(null);

        if (file != null) {
            outputDirectory = file.getParentFile();
            String outputPath = file.getAbsolutePath();
            textFieldFileOutput.setText(outputPath);
        }
    }

    @FXML
    private void selectMaxLineLength() {
        if (checkBoxSetMaxLineLength.isSelected()) {
            textFieldMaxLineLength.setDisable(false);
        } else {
            textFieldMaxLineLength.clear();
            textFieldMaxLineLength.setDisable(true);
        }
    }

    @FXML
    private void checkInputAndStartProcess() {
        Path outputFile;
        if (checkBoxSetMaxLineLength.isSelected() && (textFieldMaxLineLength.getText().isEmpty()
                || Integer.parseInt(textFieldMaxLineLength.getText()) < 1)) {
            showErrorDialog("If selecting maximum line length attribute, input desired " +
                    "maximum line length.");
            return;
        }

        if (wordListFiles.isEmpty()) {
            showErrorDialog("Add files to be processed.");
            return;
        }

        // Check field for valid path.  Check if file exists, if it does, confirm overwrite.
        // Don't create file until execution proceeds.

        if (textFieldFileOutput.getText().isEmpty()) {
            showErrorDialog("Select output path before continuing.");
            return;
        } else {
            try {
                outputFile = Paths.get(textFieldFileOutput.getText());
                Files.createFile(outputFile);
            } catch (IOException e) {
                showErrorDialog("Output file already exists.  Please choose a " +
                        "different file name or save location.");
                e.printStackTrace();
                return;
            }
        }

        labelResult.setText("");
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        disableControls();
        combineFilesAndSave();

//        if (checkBoxRemoveNonAscii.isSelected()) {
//            labelStatus.setText("Removing non-Ascii lines");
//            //NEW THREAD
//            fileFormatter.removeNonAscii(new File(textFieldFileOutput.getText()));
//        }
//        labelStatus.setText("");
    }

    private void combineFilesAndSave() {
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
        } else {
            checkMaxLineLength();
        }
    }

    private void checkMaxLineLength() {
        if (checkBoxSetMaxLineLength.isSelected()) {
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
                showOutputAndResetControls();
            });
            new Thread(setMaxLineLength).start();
        } else {
            showOutputAndResetControls();
        }
    }

    private void disableControls() {
        buttonStart.setDisable(true);
        buttonOuputLocation.setDisable(true);
        buttonAddWordlist.setDisable(true);
        buttonRemoveWordlist.setDisable(true);
        textFieldFileOutput.setDisable(true);
        textFieldMaxLineLength.setDisable(true);
        checkBoxSortLines.setDisable(true);
        checkBoxSetMaxLineLength.setDisable(true);
//        checkBoxRemoveNonAscii.setDisable(true);
    }

    private void showOutputAndResetControls() {
        labelResult.setText("Output file size: " +
                (new File(textFieldFileOutput.getText()).length() /1024) + "kb");
        progressBar.setProgress(0);
        progressBar.setVisible(false);
        labelStatus.setText("Processing complete");
        buttonStart.setDisable(false);
        buttonOuputLocation.setDisable(false);
        buttonAddWordlist.setDisable(false);
        buttonRemoveWordlist.setDisable(false);
        textFieldFileOutput.setDisable(false);
        textFieldMaxLineLength.setDisable(false);
        checkBoxSortLines.setDisable(false);
//        checkBoxRemoveNonAscii.setDisable(false);
        checkBoxSetMaxLineLength.setDisable(false);

    }

    private void showErrorDialog(String errorText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Error");
        alert.setHeaderText("Error on Input");
        alert.setContentText(errorText);
        alert.showAndWait();
    }
}
