import java.io.Serializable;


/**
 Class that stores the local state of a node
 */
public class local_state implements Serializable {

    /*
     Default serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private int id;
    private int[] vectorClock;
    private boolean isActive;
    private int sentMsgCount = -1;
    private int receivedMsgCount = -1;

    public local_state(final int id,
            final int[] vectorClock,
            final boolean isActive,
            final int sentMsgCount,
            final int receivedMsgCount) {
        this.id = id;
        this.vectorClock = new int[vectorClock.length];
        System.arraycopy(vectorClock, 0, this.vectorClock, 0, vectorClock.length);
        this.isActive = isActive;
        this.sentMsgCount = sentMsgCount;
        this.receivedMsgCount = receivedMsgCount;
    }

    public local_state(final int[] vectorClock) {
        this.vectorClock = new int[vectorClock.length];
        System.arraycopy(vectorClock, 0, this.vectorClock, 0, vectorClock.length);
    }

    public int getId() {
        return id;
    }

    public int[] getVectorClock() {
        return vectorClock;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getSentMsgCount() {
        return sentMsgCount;
    }

    public int getReceivedMsgCount() {
        return receivedMsgCount;
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
