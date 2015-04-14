/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package datamining;

import java.nio.ByteBuffer;

/**
 *
 * @author travis
 */
public class BufferPool {

    private final long[] bufferTable = new long[131072];
    private static ByteBuffer[] pool = new ByteBuffer[131072];
    private static int nodeByteSize; // (2t * 8) + ((2t-1) * 49) + 20

    public BufferPool(int t) {
        nodeByteSize = ((2 * t) * 8) + (((2 * t) - 1) * 57) + 20;
        for (int i = 0; i < bufferTable.length; i++) {
            bufferTable[i] = -1;
        }
    }

    static int hash(int hashCode) {
        hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
        return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
    }

    public ByteBuffer createBuffer() {
        return ByteBuffer.allocate(nodeByteSize);
    }

    public ByteBuffer createBuffer(long child, int h) {
        bufferTable[h] = child;
        pool[h] = createBuffer();
        return pool[h];
    }

    public ByteBuffer getBuffer(long child) {
        int h = hash((int)child) & (bufferTable.length - 1);
        if (bufferTable[h] == -1) { // buffer is uninitialized
            return createBuffer(child, h);
        } else if (isBufferAvailable(child, h)) { // buffer matched requested node
            return pool[h];
        } else {
            pool[h].flip();
            Node n = new Node(pool[h]);
            n.diskWrite();
            pool[h].clear();
            return pool[h];
        }
    }

    boolean isBufferAvailable(long child, int h) {
        if (bufferTable[h] == child) {
            return true;
        } else {
            bufferTable[h] = child;
            return false;
        }
    }

    public void closeBuffers() {
        int used = 0, unused = 0;
        for (ByteBuffer b : pool) {
            if (b != null) {
                b.flip();
                Node n = new Node(b);
                n.diskWrite();
                used++;
            } else {
                unused++;
            }
        }
        System.out.println(used+"/131072 buffers used, "+unused+"/131072 buffers unused.");
    }

}