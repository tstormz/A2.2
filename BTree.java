/*
 * BTree with minimum degree 2t and top-down insertion
    Nodes contain words as keys, and pointers to (url, freq) pairs as values
   Insertions are implemented through a ByteBuffer
 */
package datamining;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

class BTree {

    private static Node root;
    private final static int t = 400;
    public static BufferPool pool = new BufferPool(t);
    public final static File nodeFile = new File("nodes.data");
    private final static File valueFile = new File("values.data");
    public static RandomAccessFile file, valueRAF;

    BTree() {
        if (nodeFile.exists()) {
            long startPosition = 0;
            try {
                file = new RandomAccessFile(nodeFile, "rw");
                valueRAF = new RandomAccessFile(valueFile, "rw");
                file.seek(0);
                startPosition = file.readLong();
            } catch (IOException e) {
                System.out.println("IOException:" + e);
            }
            root = load(startPosition);
            System.out.println("root=" + root.head);
        } else {
            try {
                file = new RandomAccessFile(nodeFile, "rw");
                valueRAF = new RandomAccessFile(valueFile, "rw");
                file.seek(0);
                file.writeLong(0);
            } catch (IOException e) {
                System.out.println("IOException:" + e);
            }
            root = allocateNode(true);
        }
    }
    
    public final Node load(long position) {
        Node n = new Node(false);
        return n.diskRead(position, pool.createBuffer());
    }

    // determine whether the word needs to be inserted to the tree
    // or just add a (url, freq) pair to the already existent word
    void add(String x, int frequency, int url) {
        Node n = root.search(x);
        if (n != null) {
            n.addValue(x, frequency, url);
        } else {
            insert(x, frequency, url);
        }
    }

    void insert(String x, int frequency, int url) {
        if (root.size == (2 * t) - 1) {
            Node s = allocateNode(false);
            s.children[0] = root.head;
            s.split(root, 0, pool.getBuffer(root.head), pool.createBuffer());
            root = s;
            updateRoot(root.head);
            root.insert(x, frequency, url);
        } else {
            root.insert(x, frequency, url);
        }
    }

    static Node allocateNode(boolean leaf) {
        Node n = new Node(leaf);
        try {
            file.seek(file.length());
            n.head = file.getFilePointer();
            file.writeInt(0); // size
            file.writeLong(-1); // first child
            for (int i = 0; i < 2 * t - 1; i++) {
                file.write(new byte[65]);
            }
        } catch (IOException e) {
            System.out.println("IOException:" + e);
        }
        return n;
    }

    void updateRoot(long l) {
        try {
            file.seek(0);
            file.writeLong(l);
        } catch (IOException e) {
            System.out.println("IOException:" + e);
        }
    }

    public static Node getRoot() {
        return root;
    }

    public static BufferPool getPool() {
        return pool;
    }

    ByteBuffer getMatches(String word) {
        Node n = root.search(word);
        if (n != null) {
            return n.getMatches(word);
        } else {
            return null;
        }
    }

    public void inorder() {
        root.inorder();
    }

    void closeRAF() {
        try {
            file.close();
            valueRAF.close();
        } catch (IOException e) {}
    }
    
}
