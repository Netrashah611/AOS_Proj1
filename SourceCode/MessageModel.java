import java.io.Serializable;
import java.util.ArrayList;


public class MessageModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int messageType;
    private int messageId = 0;
    private final int id;
    private final ArrayList<ProcessState> data;
    

    public MessageModel(final int id, final ArrayList<ProcessState> payload, final int messageType) {
        this.id = id;
        this.data = payload;
        this.messageType = messageType;
    }

    public MessageModel(final int id, final ArrayList<ProcessState> payload , final int messageType, final int mId) {
        this.id = id;
        this.data = payload;
        this.messageType = messageType;
        this.messageId = mId;
    }
    
    public int getId() {
        return id;
    }

    public ArrayList<ProcessState> getData() {
        return data;
    }

    public int getMessageType() {
        return messageType;
    }

    

    public int getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return "Id->" + id + " Data -> " + data + " Type -> " + messageType;
    }
}
