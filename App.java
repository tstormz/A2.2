/*
 * Compare a web page given by the user to those in the tree.
 Find the top three page matches based on word frequency
 */
package datamining;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Scanner;
import static utilities.Quicksort.quicksort;

public class App {

    public static BTree tree = new BTree();
    public static Cache cache = new Cache(20);
    public static ArrayList<String> urls;

    public static void main(String[] args) throws MalformedURLException, IOException {
        urls = loadUrls();
        cache.load();
        Scanner input = new Scanner(System.in);
        String cmd;
        System.out.print("Enter a URL:");
        while (!(cmd = input.nextLine()).equals("quit")) {
            doComparison(cmd);
            System.out.println("Enter a URL:");
        }
//        tree.inorder();
    }

    static private void doComparison(String requestedPage) throws MalformedURLException, IOException {
        int[] matches = new int[urls.size() + 1];
        String[] urlsCopy = new String[urls.size()];
        urlsCopy = urls.toArray(urlsCopy);
        ByteBuffer b = cache.get(requestedPage);
        if (b == null) {
            getTheMatches(requestedPage, matches);
            int sizeOfBuffer = (4 + requestedPage.length()) + 4 + (matches.length * 8);
            b = ByteBuffer.allocate(sizeOfBuffer);
            b.putInt(matches.length - 1).putInt(requestedPage.length()).put(requestedPage.getBytes());
            for (int i = 0; i < matches.length; i++) {
                b.putInt(i).putInt(matches[i]);
            }
            cache.add(requestedPage, b);
            System.out.println("added to cache...");
        } else {
            b.getInt();
            int length = b.getInt();
            b.position(length + 8);
            while (b.hasRemaining()) {
                matches[b.getInt()] = b.getInt();
            }
            System.out.println("found in cache...");
        }
        matches[matches.length - 1] = (int) Math.pow(2, 61);
        quicksort(0, matches.length - 1, matches, urlsCopy);
        for (int i = matches.length - 2; i > matches.length - 5; i--) {
            System.out.println(urlsCopy[i] + " has " + matches[i] + " matches.");
        }
    }

    private static void getTheMatches(String requestedPage, int[] matches) throws IOException {
        HashTable table = new HashTable(requestedPage);
        for (String word : table.getWords()) {
            ByteBuffer b = tree.getMatches(word);
            if (b != null) {
                while (b.hasRemaining()) {
                    matches[b.getInt()] += b.getInt();
                }
            }
        }
    }

    private static ArrayList<String> loadUrls() throws FileNotFoundException, IOException {
        String line, file = "";
        File branches = new File("urls.txt");
        if (branches.exists()) {
            BufferedReader readBranchedUrls = new BufferedReader(new FileReader("urls.txt"));
            while ((line = readBranchedUrls.readLine()) != null) {
                file += line;
            }
        }
        Scanner sc = new Scanner(file);
        ArrayList<String> arr = new ArrayList<>();
        while (sc.hasNext()) {
            arr.add(sc.next());
        }
        return arr;
    }

}
