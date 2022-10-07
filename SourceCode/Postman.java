import java.io.IOException;
import java.io.ObjectOutputStream;

// Delivers a given message to the given node
public class Postman implements Runnable {
    private final int nodeId;
    private final MessageModel message;

    public Postman(int nodeId, MessageModel message) {
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
            System.out.println("Exception Raised!");
            e.printStackTrace();
        }
    }
}
