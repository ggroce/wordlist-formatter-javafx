package org.wordlistformatter;

import java.io.File;
import java.text.DecimalFormat;

public class WordListFile {
    private File file;
    private String filePath;
    private String fileSizeStr;

    public WordListFile(File file) {
        this.file = file;
        this.filePath = file.getAbsolutePath();
        DecimalFormat df = new DecimalFormat("0.00");
        this.fileSizeStr = df.format((float) (file.length() / 1024.0));
    }

    public File getFile() { return file; }

    public void setFile(File file) { this.file = file; }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileSizeStr() { return fileSizeStr; }

    public void setFileSizeStr(String fileSizeStr) { this.fileSizeStr = fileSizeStr; }
}
