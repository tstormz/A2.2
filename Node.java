/*
 * BTree node
 */
package datamining;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

class Node {

    private final static int t = 400;
    private final static int maxValueSize = 50;

    long[] children, values;
    String[] keys;
    long head;
    int size;
    boolean isLeaf;

    static BufferPool pool = BTree.getPool();
    static RandomAccessFile file = BTree.file;
    static RandomAccessFile valueRAF = BTree.valueRAF;

    Node(boolean leaf) {
        children = new long[2 * t];
        keys = new String[2 * t - 1];
        values = new long[2 * t - 1];
        size = 0;
        isLeaf = leaf;
        if (isLeaf) {
            children[0] = -1;
        }
    }

    Node(ByteBuffer b) {
        children = new long[2 * t];
        keys = new String[2 * t - 1];
        values = new long[2 * t - 1];
        head = b.getLong();
        size = b.getInt();
        for (int i = 0; i < size; i++) {
            children[i] = b.getLong();
            int wordLength = b.getInt();
            byte[] bytes = new byte[wordLength];
            b.get(bytes, 0, wordLength);
            keys[i] = new String(bytes);
            values[i] = b.getLong();
        }
        children[size] = b.getLong();
        isLeaf = (children[0] == -1);
    }

    public long getHead() {
        return head;
    }

    Node search(String x) {
        int i = 0;
        while (i < size && x.compareTo(keys[i]) > 0) {
            i++;
        }
        if (i < size && x.equals(keys[i])) {
            return this;
        } else if (isLeaf) {
            return null;
        } else {
            return getNextChild(children[i]).search(x);
        }
    }

    public void insert(String x, int frequency, int url) {
        int i = size - 1;
        if (isLeaf) {
            while (i >= 0 && x.compareTo(keys[i]) < 0) {
                keys[i + 1] = keys[i];
                values[i + 1] = values[i--];
            }
            keys[i + 1] = x;
            values[i + 1] = writeValue(frequency, url);
            size++;
            ByteBuffer buffer = pool.getBuffer(head);
            if (size == 1) {
                buffer.putLong(head).putInt(size).putLong(-1).putInt(x.length()).put(x.getBytes()).putLong(values[i + 1]);
            } else {
                updateLeafBuffer(buffer, i + 1);
            }
        } else {
            while (i >= 0 && x.compareTo(keys[i]) < 0) {
                i--;
            }
            Node next = getNextChild(children[++i]);
            if (next.size == (2 * t) - 1) {
                ByteBuffer buffer = pool.getBuffer(children[i]);
                split(next, i, buffer, pool.getBuffer(head));
                if (x.compareTo(keys[i]) > 0) {
                    buffer = pool.getBuffer(children[++i]);
                    next = diskRead(children[i], buffer);
                }
            }
            next.insert(x, frequency, url);
        }
    }

    public void split(Node y, int i, ByteBuffer b, ByteBuffer parent) {
        Node z = BTree.allocateNode(y.isLeaf);
        for (int j = 0; j < t - 1; j++) {
            z.keys[j] = y.keys[j + t];
            z.values[j] = y.values[j + t];
            z.size++;
        }
        if (!y.isLeaf) {
            for (int j = 0; j < t; j++) {
                z.children[j] = y.children[j + t];
            }
        }
        y.size = t - 1;
        for (int j = size; j > i; j--) {
            children[j + 1] = children[j];
        }
        children[i + 1] = z.head;
        for (int j = size - 1; j >= i; j--) {
            keys[j + 1] = keys[j];
            values[j + 1] = values[j];
        }
        keys[i] = y.keys[t - 1];
        values[i] = y.values[t - 1];
        size++;
        y.diskWriteSize();
        int mark = b.position();
        b.position(8);
        b.putInt(y.size);
        b.position(mark);
        diskWrite();
        if (y.head != BTree.getRoot().head) {
            updateBuffer(parent);
        }
        z.diskWrite();
    }

    private Node getNextChild(long child) {
        ByteBuffer buffer = pool.getBuffer(child);
        Node n;
        if (buffer.position() > 0) {
            int mark = buffer.position();
            buffer.flip();
            n = new Node(buffer);
            buffer.clear();
            buffer.position(mark);
        } else {
            n = diskRead(child, buffer);
        }
        return n;
    }

    public Node diskRead(long position, ByteBuffer b) {
        try {
            file.seek(position);
            b.putLong(position);
            int nodeSize = file.readInt();
            b.putInt(nodeSize);
            for (int i = 0; i < nodeSize; i++) {
                b.putLong(file.readLong()); // child
                int wordLength = file.readInt();
                b.putInt(wordLength);
                byte[] bytes = new byte[wordLength];
                file.read(bytes);
                b.put(bytes);
                b.putLong(file.readLong()); // value
            }
            b.putLong(file.readLong());
        } catch (IOException e) {
            System.out.println("IOException:" + e);
        }
        int mark = b.position();
        b.flip();
        Node n = new Node(b);
        b.clear();
        b.position(mark);
        return n;
    }

    public void diskWrite() {
        try {
            file.seek(head);
            file.writeInt(size);
            for (int i = 0; i < size; i++) {
                file.writeLong(children[i]);
                file.writeInt(keys[i].length());
                file.write(keys[i].getBytes());
                file.writeLong(values[i]);
            }
            file.writeLong(children[size]);
        } catch (IOException e) {
            System.out.println("IOException:" + e);
        }
    }

    public void diskWriteSize() {
        try {
            file.seek(head);
            file.writeInt(size);
        } catch (IOException e) {
            System.out.println("IOException:" + e);
        }
    }

    public void updateLeafBuffer(ByteBuffer b, int i) {
        int p = 20;
        if (b.position() > 0) {
            b.position(8); // after the head
            b.putInt(size);
            b.position(p);
            for (int j = 0; j < i; j++) { // we can skip all keys up to the insertion point.
                p += b.getInt() + 20;    // this needs to be done individually because we
                b.position(p);              // don't know how many letters a word will be.
            }
        } else {
            i = 0;
            b.putLong(head).putInt(size).putLong(-1);
        }
        while (i < size) { // after the insertion key, we need to rewrite the rest
            b.putInt(keys[i].length()).put(keys[i].getBytes()).putLong(values[i++]).putLong(-1);
        }
    }

    public void updateBuffer(ByteBuffer b) {
        b.clear();
        b.putLong(head).putInt(size);
        for (int i = 0; i < size; i++) {
            b.putLong(children[i]).putInt(keys[i].length()).put(keys[i].getBytes()).putLong(values[i]);
        }
        b.putLong(children[size]);
    }

    public void inorder() {
        if (!isLeaf) {
            for (int i = 0; i < size; i++) {
                getNextChild(children[i]).inorder();
                System.out.print(keys[i] + ":");
                try {
                    valueRAF.seek(values[i]);
                    printValues(valueRAF);
                    System.out.println();
                } catch (IOException e) {
                    System.out.println("IOException:" + e);
                }
            }
            getNextChild(children[size]).inorder();
        } else {
            for (int i = 0; i < size; i++) {
                System.out.print(keys[i]);
                try {
                    valueRAF.seek(values[i]);
                    printValues(valueRAF);
                    System.out.println();
                } catch (IOException e) {
                    System.out.println("IOException:" + e);
                }
            }
        }
    }

    public void printValues(RandomAccessFile file) throws IOException {
        int valuesNum = file.readInt();
        for (int j = 0; j < valuesNum; j++) {
            System.out.print("[" + file.readInt() + "," + file.readInt() + "]");
        }
        if (valuesNum == maxValueSize) {
            long ptr = file.readLong();
            if (ptr > 0) {
                file.seek(ptr);
                printValues(file);
            }
        }
    }

    public void addValue(String x, int frequency, int url) {
        int i = 0;
        while (i < size && x.compareTo(keys[i]) > 0) {
            i++;
        }
        long ptr, l = -1;
        if (x.equals(keys[i])) {
            l = values[i];
        }
        try {
            valueRAF.seek(l);
            int cellSize = valueRAF.readInt();
            while (cellSize == maxValueSize) {
                valueRAF.skipBytes(maxValueSize * 8);
                ptr = valueRAF.readLong();
                if (ptr == -1) {
                    valueRAF.seek(valueRAF.getFilePointer() - 8);
                    valueRAF.writeLong(writeValue(frequency, url));
                    return;
                } else {
                    l = ptr;
                    valueRAF.seek(l);
                    cellSize = valueRAF.readInt();
                }
            }
            valueRAF.seek(l);
            valueRAF.writeInt(cellSize + 1);
            valueRAF.skipBytes(cellSize * 8);
            valueRAF.writeInt(url);
            valueRAF.writeInt(frequency);
        } catch (IOException e) {
            System.out.println("IOException:" + e);
        }
    }

    private long writeValue(int frequency, int url) {
        long l = 0;
        try {
            valueRAF.seek(valueRAF.length());
            l = valueRAF.getFilePointer();
            valueRAF.writeInt(1); // size
            valueRAF.writeInt(url);
            valueRAF.writeInt(frequency);
            valueRAF.write(new byte[8 * (maxValueSize - 1)]);
            valueRAF.writeLong(-1); // next pointer
        } catch (IOException e) {
            System.out.println("IOException:" + e);
        }
        return l;
    }

    public ByteBuffer getMatches(String word) {
        int i = 0;
        ByteBuffer bb = null;
        while (i < size && word.compareTo(keys[i]) > 0) {
            i++;
        }
        long l = -1; // assumed we already found the right node
        if (word.equals(keys[i])) {
            l = values[i];
        }
        try {
            valueRAF.seek(l);
            int cellSize = valueRAF.readInt();
            bb = ByteBuffer.allocate(cellSize * 8);
            while (cellSize == maxValueSize) {
                for (int j = 0; j < maxValueSize; j++) {
                    bb.putInt(valueRAF.readInt());
                    bb.putInt(valueRAF.readInt());
                }
                l = valueRAF.readLong();
                if (l == -1) {
                    bb.flip();
                    return bb;
                } else {
                    valueRAF.seek(l);
                    cellSize = valueRAF.readInt();
                    byte[] b = bb.array();
                    int pos = bb.position();
                    bb = ByteBuffer.allocate(pos + (cellSize * 8));
                    bb.put(b);
                }
            }
            for (int j = 0; j < cellSize; j++) {
                bb.putInt(valueRAF.readInt());
                bb.putInt(valueRAF.readInt());
            }
            bb.flip();
        } catch (IOException e) {
            System.out.println("IOException:" + e);
        }
        return bb;
    }
}
