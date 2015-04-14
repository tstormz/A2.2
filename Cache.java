/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package datamining;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 *
 * @author hafner
 */
public class Cache {

    private Page pages[] = new Page[131072];
    private Page mru, lru;
    int size, maxSize, idWidget = 0;

    Cache(int max) {
        mru = new Page();
        lru = new Page();
        mru.previous = lru;
        size = 0;
        maxSize = max;
    }

    public int hash(String s) {
        int hash = 1315423911;
        for (int i = 0; i < s.length(); i++) {
            hash ^= ((hash << 5) + s.charAt(i) + (hash >> 2));
        }
        return hash;
    }

    public Page getMRU() {
        return mru.previous;
    }

    public boolean isFull() {
        return (size >= maxSize);
    }

    public int getID() {
        if (isFull()) {
            return lru.next.id;
        } else {
            return size++;
        }
    }

    public void load() throws FileNotFoundException, IOException {
        String url = "";
        ByteBuffer b = null;
        for (int i = 0; i < maxSize; i++) {
            File f = new File("cache." + i);
            if (f.exists()) {
                RandomAccessFile raf = new RandomAccessFile(f, "rw");
                try {
                    raf.seek(0);
                    int nodeSize = raf.readInt();
                    int length = raf.readInt();
                    byte[] bytes = new byte[length];
                    raf.read(bytes);
                    url = new String(bytes);
                    b = ByteBuffer.allocate(url.length() + 8 + (nodeSize * 8));
                    b.putInt(nodeSize).putInt(url.length()).put(url.getBytes());
                    for (;;) {
                        b.putInt(raf.readInt());
                    }
                } catch (IOException ex) {
                } finally {
                    raf.close();
                }
                b.flip();
                put(url, b);
            }
        }
    }

    public void placeInFront(Page page) {
        Page tempPage = mru.previous;
        page.previous = tempPage;
        tempPage.next = page;
        mru.previous = page;
        page.next = mru;
    }

    public void moveToFront(Page page) {
        pullPage(page);
        placeInFront(page);
    }

    private void pullPage(Page page) {
        Page behind = page.previous;
        Page front = page.next;
        behind.next = front;
        front.previous = behind;
    }

    public Page put(String s, ByteBuffer b) {
        int h = hash(s) & pages.length - 1;
        b.flip();
        Page page = new Page(s, b);
        if (pages[h] != null) {
            pullPage(pages[h]);
            placeInFront(page);
        } else {
            if (isFull()) {
                int hash = hash(lru.next.getKey()) & pages.length - 1;
                pullPage(lru.next);
                pages[hash] = null;
            }
            placeInFront(page);
        }
        pages[h] = page;
        return pages[h];
    }

    public void add(String s, ByteBuffer b) {
        put(s, b).diskWrite();
    }

    public ByteBuffer get(String s) {
        int h = hash(s) & pages.length - 1;
        if (pages[h] != null) {
            if (pages[h].matches(s)) {
                if (pages[h].next != mru) {
                    moveToFront(pages[h]);
                }
                pages[h].value.clear();
                return pages[h].value;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public class Page {

        private String key;
        private ByteBuffer value;
        private int id;
        private Page next;
        private Page previous;

        public Page() {
            next = null;
            previous = null;
        }

        public Page(String s, ByteBuffer b) {
            key = s;
            value = b;
            id = getID();
            System.out.println("new cache." + id + " = " + key);
        }

        public boolean matches(String s) {
            return s.equals(key);
        }

        private String getKey() {
            return key;
        }

        public ByteBuffer getValue() {
            return value;
        }

        private void diskWrite() {
            File fileName = new File("cache." + id);
            if (fileName.exists()) {
                fileName.delete();
            }
            try {
                RandomAccessFile file = new RandomAccessFile(fileName, "rw");
                int numberOfPairs = value.getInt();
                file.writeInt(numberOfPairs);
                file.writeInt(value.getInt());
                byte[] bytes = new byte[key.length()];
                value.get(bytes, 0, bytes.length);
                file.write(bytes);
                for (int i = 0; i < numberOfPairs; i++) {
                    file.writeInt(value.getInt());
                    file.writeInt(value.getInt());
                }
                file.close();
            } catch (IOException e) {
            }
        }
    }
}
