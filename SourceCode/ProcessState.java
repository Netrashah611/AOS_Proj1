/*
 * Class that stores the process/local state of a node
*/

import java.io.*;

public class ProcessState implements Serializable {

    private int id;
    private static final long serialVersionUID = 1L;
    private int sentMessagesCount = -1;
    private int receivedMessagesCount = -1;
    private int[] vectorClock;
    private boolean isActive;

    public ProcessState(final int id,
            final int[] vectorClock,
            final boolean isActive,
            final int sentMessagesCount,
            final int receivedMessagesCount) {
        this.id = id;
        this.vectorClock = new int[vectorClock.length];
        System.arraycopy(vectorClock, 0, this.vectorClock, 0, vectorClock.length);
        this.isActive = isActive;
        this.sentMessagesCount = sentMessagesCount;
        this.receivedMessagesCount = receivedMessagesCount;
    }

    public ProcessState(final int[] vectorClock) {
        this.vectorClock = new int[vectorClock.length];
        System.arraycopy(vectorClock, 0, this.vectorClock, 0, vectorClock.length);
    }

    public int getId() {
        return id;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getreceivedMessagesCount() {
        return receivedMessagesCount;
    }

    public int getsentMessagesCount() {
        return sentMessagesCount;
    }

    public int[] getTheVectorClock() {
        return vectorClock;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < vectorClock.length - 1; i++) {
            builder.append(vectorClock[i]).append(" ");
        }
        builder.append(vectorClock[vectorClock.length - 1]);

        return builder.toString();
    }

}
