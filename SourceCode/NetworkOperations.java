/*
 * A class for storing and accessing network communication variables
*/

import java.io.*;
import java.net.Socket;
import java.util.HashMap;


public class NetworkOperations {

    protected static HashMap<Integer, Socket> socketHashMap = new HashMap<>();
    protected static HashMap<Integer, ObjectOutputStream> writerStreamHashMap = new HashMap<>();
    protected static HashMap<Integer, ObjectInputStream> readerStreamHashMap = new HashMap<>();
    
    public static int getsocketHashMapSize() {
        return socketHashMap.size();
    }

    public static boolean containsSocketEntryInMap(int index) {
        return socketHashMap.containsKey(index);
    }

    public static ObjectInputStream getReaderStream(int index) {
        return readerStreamHashMap.get(index);
    }

    public static ObjectOutputStream getWriterStream(int index) {
        return writerStreamHashMap.get(index);
    }

    public static synchronized void addSocketToSocketEntry(int index, Socket socket) {
        socketHashMap.put(index, socket);
    }

    public static synchronized void addInStreamEntry(int index, ObjectInputStream stream) {
        readerStreamHashMap.put(index, stream);
    }

    public static synchronized void addOutStreamEntry(int index, ObjectOutputStream stream) {
        writerStreamHashMap.put(index, stream);
    }

    

}
