/*
 * Class that represents the communications that the nodes send and receive.
*/

import java.io.*;
import java.util.*;

public class MessageFramework implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int messageType;
    private int messageId = 0;
    private final int id;
    private final ArrayList<ProcessState> data;
    
    public MessageFramework(final int id, final ArrayList<ProcessState> payload, final int messageType) {
        this.id = id;
        this.data = payload;
        this.messageType = messageType;
    }

    public MessageFramework(final int id, final ArrayList<ProcessState> payload , final int messageType, final int mId) {
        this.id = id;
        this.data = payload;
        this.messageType = messageType;
        this.messageId = mId;
    }

    @Override
    public String toString() {
        return "Id->" + id + " Data -> " + data + " Type -> " + messageType;
    }

    public int getId() {
        return id;
    }

    public int getMessageId() {
        return messageId;
    }

    public ArrayList<ProcessState> getData() {
        return data;
    }

    public int getMessageType() {
        return messageType;
    }

    
}
