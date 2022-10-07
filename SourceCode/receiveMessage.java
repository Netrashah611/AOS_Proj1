import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.io.ObjectOutputStream;
/*
 Receiver thread for the node which receives tokens sent to a given input stream.
 It processes the received tokens and forwards accordingly.
 */
public class receiveMessage implements Runnable {

    private static final int ID = GlobalConfiguration.id;
    private static final int TOTAL_NODE_COUNT = GlobalConfiguration.map_size;
    private final ObjectInputStream inputStream;
    private final ArrayList<Integer> neighbors;
    private final int neighborCount;
    private final int expectedSnapshotReplies;
    public volatile boolean is_running_flag = true;

    public receiveMessage(final ObjectInputStream inputStream,
            final ArrayList<Integer> neighbors) {
        this.inputStream = inputStream;
        this.neighbors = neighbors;
        this.neighborCount = neighbors.size();
        this.expectedSnapshotReplies = ID == 0 ? this.neighborCount : this.neighborCount - 1;
    }

    @Override
    public void run() {
        while(is_running_flag) {
            try {
                MessageModel message = (MessageModel) inputStream.readObject();
                switch (message.getMessageType()) {
                    case 1: 
                        handleApplicationMessage(message);
                        break;
                    case 2:
                        handleMarkerMessage(message);
                        break;
                    case 5:
                        handleFinishMessage(message);
                        break;
                    default:
                        handleSnapshotReplyMessage(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleFinishMessage(MessageModel message) {
        // Send finish message to neighbors and wait for response
        MessageModel broadcastMarkerMsg = new MessageModel(ID, null, 5);
        for (Integer neighborId : neighbors) {
            if (neighborId != message.getId()) {
                launchSnapshotSender(neighborId, broadcastMarkerMsg);
            }
        }
        GlobalConfiguration.setIsSystemTerminated(true);
        is_running_flag = false;
    }

    /*
     <pre>Processes incoming snapshot reply message
      If type LOCAL_STATE - Adds received payload to the global payload list
      If type IGNORED - ignore the message
      When all the expected replies are received,
      send the consolidated payload to the node from which it received the marker message<pre>
      @param message {@link MessageModel}
     */
    private void handleSnapshotReplyMessage(MessageModel message) {
        // Increment received reply count
        GlobalConfiguration.incrementReceivedSnapshotReplies();

        if(message.getMessageType() == 4) {
            Logger.logMessage("Received IGNORED reply from " + message.getId());
            // Do nothing
        }
        else {
            // LOCAL_STATE type
            GlobalConfiguration.addLocalStateAll(message.getData());
            Logger.logMessage("Received LOCAL_STATE reply from " + message.getId()
                    + " ==> Received payload : " + message.getData());
        }
        // Check if all expected replies are received
        if((GlobalConfiguration.getReceivedSnapshotReplyCount() == expectedSnapshotReplies)) { 

            // If node ID = 0, then set all replies received as true
            if (ID == 0) {
                GlobalConfiguration.setsnaprep(true);
            }
            else {
                // Send consolidated local state reply
                Logger.logMessage("Received expected number of replies, send cumulative local states");
                ArrayList<local_state> snapshotPayload = new ArrayList<>();
                snapshotPayload.addAll(GlobalConfiguration.getLocalStateAll());

                MessageModel replyStateMsg = new MessageModel(ID, snapshotPayload, 3);
                int markerSenderNode = GlobalConfiguration.getMarkerSender();
                Logger.logMessage("Send snapshot reply to " + markerSenderNode
                        + " ==> Message : " + replyStateMsg);
                launchSnapshotSender(markerSenderNode, replyStateMsg);

            }

        }
    }

    /*
     <pre>Processes incoming application message
     Merge the piggybacked vector clock from the message 
     and become active if node satisfies predefined criteria<pre>
     @param message {@link MessageModel}
     */
    private void handleApplicationMessage(MessageModel message) {
        // Application message
        GlobalConfiguration.inc_rcv_msg_count();
        mergeVectorClocks(message);

        Logger.logMessage("Received application message : " + message
                + "\nMerged clock : " + GlobalConfiguration.displayGlobalClock());

        if (GlobalConfiguration.check_active()) {
            // Already active, ignore the message
            Logger.logMessage("Already active...");
            return;
        }
        if (GlobalConfiguration.get_sent_msg_count() >= GlobalConfiguration.maxNumber) {
            // Cannot become active, so ignore
            Logger.logMessage("Reached max send limit... cannot become active");
            return;
        }

        // Can become active
        Logger.logMessage("Becoming active...");
        GlobalConfiguration.set_active_status(true);
    }

    /*
     Processes incoming marker message
     If it is a valid marker message, broadcast it to other neighbors, else discard
     @param message {@link MessageModel}
     */
    private void handleMarkerMessage(MessageModel message) {
        GlobalConfiguration.incCurrentMarkersReceivedCount();

        if (GlobalConfiguration.recmark.contains(message.getMessageId()) || ID == 0) {
            // Send ignore message
            Logger.logMessage("Marker message received from " + message.getId() + "... IGNORE");
            MessageModel replyMessage =  new MessageModel(ID, null, 4);
            launchSnapshotSender(message.getId(), replyMessage);
        }
        else {
            // Add own local state to the received local state list
            GlobalConfiguration.recmark.add(message.getMessageId());
            GlobalConfiguration.reset_snap();
            GlobalConfiguration.setmarkermessagerecv(true);
            int[] localClock = new int[TOTAL_NODE_COUNT];
            synchronized (GlobalConfiguration.vector_clock) {
                System.arraycopy(GlobalConfiguration.vector_clock, 0, localClock, 0, TOTAL_NODE_COUNT);
            }

            local_state myPayload = new local_state(ID, localClock,
                    GlobalConfiguration.check_active(), GlobalConfiguration.get_sent_msg_count(),
                    GlobalConfiguration.get_rcv_msg_count());
            Logger.logMessage("Recording state : " + myPayload.toString());
            GlobalConfiguration.add_localstate(myPayload);

            GlobalConfiguration.setMarkerSender(message.getId());
            Logger.logMessage("Marker message received from " + message.getId() + "... BROADCAST\n"
                    + "Expecting replies = " + expectedSnapshotReplies);
            if(expectedSnapshotReplies == 0) {
                // Send consolidated local state reply
                Logger.logMessage("Received expected number of replies, send cumulative local states");
                ArrayList<local_state> snapshotPayload = new ArrayList<>();
                snapshotPayload.addAll(GlobalConfiguration.getLocalStateAll());

                MessageModel replyStateMsg = new MessageModel(ID, snapshotPayload, 3);
                int markerSenderNode = GlobalConfiguration.getMarkerSender();
                Logger.logMessage("Send snapshot reply to " + markerSenderNode
                        + " ==> Message : " + replyStateMsg);
                launchSnapshotSender(markerSenderNode, replyStateMsg);

                return;
            }
            // Send marker message to neighbors and wait for response
            MessageModel broadcastMarkerMsg = new MessageModel(ID, null, 2, message.getMessageId());
            for (Integer neighborId : neighbors) {
                if (neighborId != message.getId()) {
                    launchSnapshotSender(neighborId, broadcastMarkerMsg);
                }
            }
        }
    }

    /*
     Merge the incoming message's piggybacked vector clock into own global clock
     @param message {@link MessageModel}
     */
    private void mergeVectorClocks(MessageModel message) {
        int[] piggybackVectorClock = message.getData().get(0).getVectorClock();
        synchronized (GlobalConfiguration.vector_clock) {
            for (int i = 0; i < TOTAL_NODE_COUNT; i++) {
                GlobalConfiguration.vector_clock[i] = Math.max(GlobalConfiguration.vector_clock[i], piggybackVectorClock[i]);
            }
            GlobalConfiguration.vector_clock[ID]++;
        }
    }

    /*
     Launch {@link SnapshotSender} thread and wait for it to finish.
     It sends given snapshot reply message to given node.
     @param id - neighbor node id
     @param message {@link MessageModel}
     */
    private void launchSnapshotSender(int id, MessageModel message) {
        SnapshotSender snapshotSender = new SnapshotSender(id, message);
        Thread thread = new Thread(snapshotSender);
        thread.start();
    }

    public class SnapshotSender implements Runnable {

        private final int nodeId;
        private final MessageModel message;
    
        public SnapshotSender(final int id,
                final MessageModel msg) {
            nodeId = id;
            message = msg;
        }
    
        @Override
        public void run() {
            try {
                ObjectOutputStream outputStream = NetworkOperations.getWriterStream(nodeId);
                synchronized (outputStream) {
                    outputStream.writeObject(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
    }

    
}
