/*
 * Read in 10 root pages and gather all links on each page
 For each page, create a HashTable of word, frequency pairs
 If the word doesn't exist in the Tree, insert it, otherwise
 add the word, frequency pair for that page to the appropriate word
 */
package datamining;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Loader {

    public static BTree tree;
    public static Cache cache = new Cache(20);
    public static HashTable[] cacheTables = new HashTable[20];
    public static ArrayList<String> urls;

    public static void main(String[] args) throws MalformedURLException, IOException {
        File f = new File("nodes.data");
        if (f.exists()) { f.delete(); }
        f = new File("values.data");
        if (f.exists()) { f.delete(); }
        tree = new BTree();
        urls = loadUrls();
        for (int i = urls.size()-1; i >= 0; i--) {
            System.out.println("PAGE:" + urls.get(i));
            URL u = new URL(urls.get(i));
            grabLinks(u);
        }
        saveUrls(urls);
        int totalUrls = urls.size();
        for (int i = 0; i < urls.size(); i++) {
            System.out.println("PAGE:" + urls.get(i) + " (#" + i + " of " + totalUrls + ")");
            HashTable table = new HashTable(urls.get(i));
            ArrayList<String> allWords = table.getWords();
            int[] frequencies = table.getFrequencies(allWords);
            for (int j = 0; j < allWords.size(); j++) {
                tree.add(allWords.get(j), frequencies[j], i);
            }
            System.out.println("<page added to the tree>");
        }
        tree.getPool().closeBuffers();
        tree.closeRAF();
    }

    private static boolean verifyURL(String link) {
        try {
            URL u = new URL(link);
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            huc.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD"); 
            huc.connect();
            int code = huc.getResponseCode();
            huc.disconnect();
            if (code == 200) {
                return true;
            } else {
                return false;
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private static ArrayList<String> loadUrls() throws FileNotFoundException, IOException {
        BufferedReader readUrls = new BufferedReader(new FileReader("root_urls.txt"));
        String line, file = "";
        while ((line = readUrls.readLine()) != null) {
            file += line;
        }
        Scanner sc = new Scanner(file);
        ArrayList<String> arr = new ArrayList<>();
        while (sc.hasNext()) {
            arr.add(sc.next());
        }
        return arr;
    }

    private static void saveUrls(ArrayList<String> urls) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter("urls.txt"));
        for (String u : urls) {
            out.write(u + " ");
        }
        out.close();
    }

    private static void grabLinks(URL u) throws IOException {
        String domain = u.getHost();
        String inputLine;
        BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
        do {
            if ((inputLine = in.readLine()) == null) {
                break;
            }
        } while (!inputLine.matches("[\\s.]*</head>"));
        int i;
        while ((inputLine = in.readLine()) != null) {
            while ((i = inputLine.indexOf("<a")) > -1) {
                while (inputLine.indexOf("</a") == -1) {
                    inputLine += in.readLine();
                }
                String theLink = inputLine.substring(i, inputLine.indexOf("</a"));
                for (String property : theLink.split("[\\s|>]")) {
                    if (property.length() > 4 && property.substring(0, 5).equals("href=")) {
                        String link = property.substring(6, property.length() - 1);
                        int id = link.indexOf("#");
                        if (id >= 0) {
                            link = link.substring(0, id);
                        }
                        if (link.length() > 7 && !(link.substring(5).contains("?") | link.substring(5).contains(":"))) {
                            if (link.substring(0, 1).equals("/") && !link.substring(1, 2).equals("/")) {
                                link = "http://" + domain + link;
                            } else if (!link.substring(0, 7).equals("http://") && !link.substring(1, 2).equals("/")) {
                                link = "http://" + link;
                            } else {
                                break;
                            }
                            if (verifyURL(link)) {
                                if (!urls.contains(link)) {
                                    urls.add(link);
                                }
                            }
                        }
                        break;
                    }
                }
                inputLine = inputLine.substring(i + theLink.length() + 2);
            }
        }
    }

}
