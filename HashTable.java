package datamining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import utilities.StopWords;

public class HashTable {

    private Bucket[] hashtable = new Bucket[1070];
    private int count = 0;
    private final String url;
    private static StopWords stopWords;

    public HashTable(String url) throws IOException {
        this.url = url;
        stopWords = StopWords.getInstance();
        URL u = new URL(url.toString());
        BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.matches("[\\s.]*<head>")) {
                while (!inputLine.matches("[\\s.]*</head>")) {
                    if ((inputLine = in.readLine()) == null) {
                        break;
                    }
                }
                continue;
            }
            inputLine = inputLine.replaceAll("\\<.*?>", "");
            for (String word : inputLine.split("\\s+")) {
                word = word.replaceAll("\\d|\\W", "").toLowerCase();
                if (filter(word)) {
                    add(word);
                }
            }
        }
    }

    private static boolean filter(String word) {
        word = word.replaceAll("\\d", "");
        int length = word.length();
        if (length > 1 && length < 45) {
            return !stopWords.get().contains(word);
        } else {
            return false;
        }
    }

    public int hash(String s) {
        int hash = 1315423911;
        for (int i = 0; i < s.length(); i++) {
            hash ^= ((hash << 5) + s.charAt(i) + (hash >> 2));
        }
        return hash;
    }

    public String getUrl() {
        return url;
    }

    private void add(String s) {
        if (count > ((int) (.75 * hashtable.length))) {
            resize();
        }
        int hash = hash(s);
        int h = hash & (hashtable.length - 1);
        if (hashtable[h] == null) {
            hashtable[h] = new Bucket(s);
        } else {
            hashtable[h].addValue(s);
        }
        count++;
    }

    public void add(String s, int f) {
        int hash = hash(s);
        int h = hash & (hashtable.length - 1);
        if (hashtable[h] == null) {
            hashtable[h] = new Bucket(s);
        }
        hashtable[h].setValue(s, f);
    }

    public void resize() {
        Bucket[] temp = hashtable;
        hashtable = new Bucket[temp.length * 3];
        count = 0;
        for (Bucket n : temp) {
            if (n != null) {
                for (String word : n.getContents().split(" ")) {
                    add(word);
                }
            }
        }
    }

    public int[] getFrequencies(ArrayList<String> words) {
        int[] frequencies = new int[words.size()];
        int hash, h, i = 0;
        for (String word : words) {
            hash = hash(word);
            h = hash & (hashtable.length - 1);
            frequencies[i++] = hashtable[h].getFrequency(word);
        }
        return frequencies;
    }

    public ArrayList<String> getWords() {
        ArrayList<String> words = new ArrayList<>();
        for (Bucket b : hashtable) {
            if (b != null) {
                for (String word : b.getContents().split(" ")) {
                    words.add(word);
                }
            }
        }
        return words;
    }

    public boolean contains(String s) {
        int hash = hash(s);
        int h = hash & (hashtable.length - 1);
        Bucket b = hashtable[h];
        if (b != null) {
            return b.find(s);
        } else {
            return false;
        }
    }

    public Bucket getBucket(int h) {
        return hashtable[h];
    }

    public void display() {
        for (Bucket b : hashtable) {
            if (b != null) {
                System.out.println(b.toString());
            }
        }
    }

    private static class Bucket {

        private final String key;
        private Integer value;
        private Bucket next;

        public Bucket(String key) {
            this.key = key;
            value = 1;
            next = null;
        }

        public void addValue(String s) {
            if (key.equals(s)) {
                value++;
            } else if (next != null) {
                next.addValue(s);
            } else {
                next = new Bucket(s);
            }
        }

        public void setValue(String s, int f) {
            if (key.equals(s)) {
                value = f;
            } else if (next != null) {
                next.setValue(s, f);
            } else {
                next = new Bucket(s);
                next.value = f;
            }
        }

        public boolean find(String s) {
            if (key.equals(s)) {
                return true;
            } else if (next != null) {
                return next.find(s);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            String elements = "[" + key + ":" + value + "]";
            if (next != null) {
                elements += next.toString();
            }
            return elements;
        }

        private String getContents() {
            if (next != null) {
                return key + " " + next.getContents();
            } else {
                return key;
            }
        }

        private int getFrequency(String s) {
            if (key.equals(s)) {
                return value;
            } else if (next != null) {
                return next.getFrequency(s);
            } else {
                return 0;
            }
        }
    }
}
