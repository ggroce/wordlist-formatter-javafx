package org.wordlistformatter;

import java.text.DecimalFormat;

public class WordListFile {
    private String filePath;
    private String fileSizeStr;
    private Float fileSize;

    public WordListFile(String filePath, Float fileSize) {
        this.filePath = filePath;
        this.fileSize = fileSize;
        DecimalFormat df = new DecimalFormat("0.00");
        this.fileSizeStr = df.format(fileSize);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Float getFileSize() {
        return fileSize;
    }

    public void setFileSize(Float fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileSizeStr() { return fileSizeStr; }

    public void setFileSizeStr(String fileSizeStr) { this.fileSizeStr = fileSizeStr; }
}
