package org.wordlistformatter;

import javafx.collections.ObservableList;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FileFormatter {

    public void combineFiles(ObservableList<WordListFile> wordListFiles, File outputFile) {

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsoluteFile());
             FileChannel fileChannelOut = fileOutputStream.getChannel();) {

            for (WordListFile wordListFile : wordListFiles) {
                try (FileInputStream fileInputStream = new FileInputStream(wordListFile.getFile());
                     FileChannel fileChannelIn = fileInputStream.getChannel();) {

                    fileChannelIn.transferTo(0, fileChannelIn.size(), fileChannelOut);
                    ByteBuffer newLine = ByteBuffer.wrap("\n".getBytes());
                    fileChannelOut.write(newLine);
                } catch (IOException e) { e.printStackTrace(); }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void sortByStringSize(File file) {
        ArrayList<String> wordList = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(file.getAbsoluteFile())))
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

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()))) {
                for (String line : wordList) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) { e.printStackTrace(); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void removeDuplicates(File file) {
        Set<String> wordList = new LinkedHashSet<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(file.getAbsoluteFile())))
        {
            String string = reader.readLine();

            while (string != null) {
                wordList.add(string);
                string = reader.readLine();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()))) {
                for (String line : wordList) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) { e.printStackTrace(); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void setMaxLineLength(int maxLength, File file) {
        ArrayList<String> wordList = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(file.getAbsoluteFile()));)
        {
            String string = reader.readLine();

            while (string != null) {
                if (string.length() <= maxLength) {
                    wordList.add(string);
                }
                string = reader.readLine();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()))) {
                for (String line : wordList) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) { e.printStackTrace(); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void removeNonPrintable(File file) {
        ArrayList<String> wordList = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(file.getAbsoluteFile()));)
        {
            String string = reader.readLine();

            while (string != null) {
                if (string.matches("\\A\\p{Print}+\\z")) {
                    wordList.add(string);
                }
                string = reader.readLine();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()))) {
                for (String line : wordList) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) { e.printStackTrace(); }
        } catch (IOException e) { e.printStackTrace(); }
    }
}
