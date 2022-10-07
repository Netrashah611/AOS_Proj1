import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/*
 Listener thread for connection requests from other nodes.
 It accepts a connection (if not already established) and adds it into global connection maps
 */
public class Listener implements Runnable {
    private final ServerSocket listener_socket;

    private final ArrayList<Integer> neighbors;

    private int connector;

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

                    ObjectInputStream ois = new ObjectInputStream(connectionSocket.getInputStream());
                    byte[] buff = new byte[4];
                    ois.read(buff, 0, 4);
                    ByteBuffer bytebuff = ByteBuffer.wrap(buff);
                    int nodeId = bytebuff.getInt();
                    connector = nodeId;
                    Logger.logMessage("Connected : " + nodeId);

                    NetworkOperations.addSToSocketEntry(nodeId, connectionSocket);
                    NetworkOperations.addInputStreamEntry(nodeId, ois);
                    NetworkOperations.addOutputStreamEntry(nodeId, new ObjectOutputStream(connectionSocket.getOutputStream()));

                } catch (IOException e) {
                    Logger.logMessage(connector + " - Listener : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Logger.logMessage(connector + " - Listener : " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                listener_socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
