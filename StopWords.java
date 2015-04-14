/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author hafner
 */
public class StopWords {
    
    private static StopWords instance;
    private final File f = new File("stopwords2.txt");
    ArrayList<String> stopWords;
    
    private StopWords() throws FileNotFoundException, IOException {
        System.out.println("### reading the stopwords file");
        stopWords = new ArrayList<>();
        BufferedReader readStopWords = new BufferedReader(new FileReader(f));
        String stopLine = readStopWords.readLine();
        Scanner sc = new Scanner(stopLine);
        sc.useDelimiter(",");
        while (sc.hasNext()) {
            stopWords.add(sc.next());
        }
    }
    
    public static StopWords getInstance() throws FileNotFoundException, IOException {
        if (instance == null) {
            instance = new StopWords();
        }
        return instance;
    }
    
    public ArrayList<String> get() {
        return stopWords;
    }
    
}
