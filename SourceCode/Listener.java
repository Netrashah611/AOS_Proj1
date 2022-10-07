import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;


public class Listener implements Runnable {
    private int connector;
    private final ServerSocket listener_socket;
    private final ArrayList<Integer> neighbors;

    public Listener(final ServerSocket listener_socket, final ArrayList<Integer> neighbors) {
        this.listener_socket = listener_socket;
        this.neighbors = neighbors;
    }

    @Override
    public void run() {
        int numOfPeers = neighbors.size();
        Socket connectionSocket;

        try {
            // only establish connection if it is not there in socket map . it should be less than total num of neighbours
            while (NetworkOperations.getSocketMapSize() < numOfPeers) {
                try {
                    connectionSocket = listener_socket.accept();

                    ObjectInputStream oiStream = new ObjectInputStream(connectionSocket.getInputStream());
                    byte[] buff = new byte[4];
                    oiStream.read(buff, 0, 4);
                    ByteBuffer bytebuff = ByteBuffer.wrap(buff);
                    int nodeId = bytebuff.getInt();
                    connector = nodeId;
                    Logger.logMessage("Connected - " + nodeId);

                    NetworkOperations.addSToSocketEntry(nodeId, connectionSocket);
                    NetworkOperations.addInputStreamEntry(nodeId, oiStream);
                    NetworkOperations.addOutputStreamEntry(nodeId, new ObjectOutputStream(connectionSocket.getOutputStream()));

                } catch (IOException e) {
                    System.out.println("Exception Raised! Couldn't establish connection");
                    Logger.logMessage(connector + " - Listener - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Logger.logMessage(connector + " - Listener - " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                listener_socket.close();
            } catch (IOException e) {
                
                System.out.println("Exception Raised! : IO");
                e.printStackTrace();
            }
        }
    }
}
