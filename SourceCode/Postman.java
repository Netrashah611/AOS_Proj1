/*
 * Delivers a given message to the given node
*/

import java.io.*;
public class Postman implements Runnable {
    private final int nodeId;
    private final MessageFramework message;

    public Postman(int nodeId, MessageFramework message) {
        this.nodeId = nodeId;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream outputStream = NetworkOperations.getWriterStream(nodeId);
            synchronized (outputStream) {
                outputStream.writeObject(message);
            }
        } catch (IOException e) {
            System.out.println("Exception Raised! Couldn't deliver the message");
            e.printStackTrace();
        }
    }
}
