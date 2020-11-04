package org.wordlistformatter;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    @FXML private Label labelStatus;
    @FXML private ProgressBar progressBar;
    private ObservableList<WordListFile> wordListFiles = FXCollections.observableArrayList();
    private Set<String> filePathSet = new HashSet<>();
    private File lastUsedDirectory;

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
            System.out.println(lastUsedDirectory.getAbsolutePath());
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

        combineFiles();

        if (checkBoxSortLines.isSelected()) {
            sortByStringSize();
        }
        if (checkBoxRemoveNonAscii.isSelected()) {
            removeNonAscii();
        }
        if (checkBoxSetMaxLineLength.isSelected()) {
            if (textFieldMaxLineLength.getText().isEmpty()
                    || Integer.parseInt(textFieldMaxLineLength.getText()) < 1) {
                showErrorDialog("If selecting maximum line length attribute, input desired " +
                        "maximum line length.");
                return;
            } else {
                setMaxLineLength(Integer.parseInt(textFieldMaxLineLength.getText()));
            }
        }
        labelStatus.setText("");
    }

    private void combineFiles() {

        labelStatus.setText("Combining files...");

        try (FileOutputStream fileOutputStream = new FileOutputStream(textFieldFileOutput.getText());
             FileChannel fileChannelOut = fileOutputStream.getChannel();) {

            for (WordListFile wordListFile : wordListFiles) {
                try (FileInputStream fileInputStream = new FileInputStream(wordListFile.getFile());
                     FileChannel fileChannelIn = fileInputStream.getChannel();) {

                    fileChannelIn.transferTo(0, fileChannelIn.size(), fileChannelOut);
                    ByteBuffer newLine = ByteBuffer.wrap("\n".getBytes());
                    fileChannelOut.write(newLine);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setMaxLineLength(int maxLength) {
        labelStatus.setText("Removing lines greater than " + maxLength);
        ArrayList<String> wordList = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(textFieldFileOutput.getText()));)
        {
            String string = reader.readLine();

            while (string != null) {
                if (string.length() <= maxLength) {
                    wordList.add(string);
                }
                string = reader.readLine();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(textFieldFileOutput.getText()));) {
                for (String line : wordList) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) { e.printStackTrace(); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void removeNonAscii() {
        labelStatus.setText("Removing non-Ascii lines");
        //TODO: implement removal of non-ascii characters
    }

    private void sortByStringSize() {
        labelStatus.setText("Sorting lines by string length");
        ArrayList<String> wordList = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(textFieldFileOutput.getText()));)
        {
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

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(textFieldFileOutput.getText()));) {
                for (String line : wordList) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) { e.printStackTrace(); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showErrorDialog(String errorText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Error");
        alert.setHeaderText("Error on Input");
        alert.setContentText(errorText);
        alert.showAndWait();
    }
}
