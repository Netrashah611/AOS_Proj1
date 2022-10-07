import java.io.*;
import java.util.ArrayList;

public class receiveMessage implements Runnable {

    private static final int ID = ConfigurationClass.id;
    private static final int TOTAL_NODE_COUNT = ConfigurationClass.map_size;
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
                System.out.println("Exception Raised!");
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
        ConfigurationClass.setIsSystemTerminated(true);
        is_running_flag = false;
    }


    private void handleSnapshotReplyMessage(MessageModel message) {
        // Increment received reply count
        ConfigurationClass.incrementReceivedSnapshotReplies();

        if(message.getMessageType() == 4) {
            Logger.logMessage("Received reply from " + message.getId() + "of type ignore");
            // Do nothing
        }
        else {
            // LOCAL_STATE type
            ConfigurationClass.addLocalStateAll(message.getData());
            Logger.logMessage("Received process state reply from " + message.getId()
                    + " -> Received payload : " + message.getData());
        }
        // Check if all expected replies are received
        if((ConfigurationClass.getReceivedSnapshotReplyCount() == expectedSnapshotReplies)) { 

            // If node ID = 0, then set all replies received as true
            if (ID == 0) {
                ConfigurationClass.setsnaprep(true);
            }
            else {
                // Send consolidated local state reply
                Logger.logMessage("Expected replies arrived, send cumulative process states");
                ArrayList<ProcessState> snapshotPayload = new ArrayList<>();
                snapshotPayload.addAll(ConfigurationClass.getLocalStateAll());

                MessageModel replyStateMsg = new MessageModel(ID, snapshotPayload, 3);
                int markerSenderNode = ConfigurationClass.getMarkerSender();
                Logger.logMessage("Send snapshot reply to " + markerSenderNode
                        + " -> Message : " + replyStateMsg);
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
        ConfigurationClass.inc_rcv_msg_count();
        mergeVectorClocks(message);

        Logger.logMessage("Received application message : " + message
                + "\nMerged clock : " + ConfigurationClass.displayGlobalClock());

        if (ConfigurationClass.check_active()) {
            // Already active, ignore the message
            Logger.logMessage("Already active...");
            return;
        }
        if (ConfigurationClass.get_sent_msg_count() >= ConfigurationClass.maxNumber) {
            // Cannot become active, so ignore
            Logger.logMessage("Reached max send limit... cannot become active");
            return;
        }

        // Can become active
        Logger.logMessage("Becoming active...");
        ConfigurationClass.set_active_status(true);
    }

    private void handleMarkerMessage(MessageModel message) {
        ConfigurationClass.incCurrentMarkersReceivedCount();

        if (ConfigurationClass.recmark.contains(message.getMessageId()) || ID == 0) {
            // Send ignore message
            Logger.logMessage("Marker message received from " + message.getId() + "... IGNORE");
            MessageModel replyMessage =  new MessageModel(ID, null, 4);
            launchSnapshotSender(message.getId(), replyMessage);
        }
        else {
            // Add own local state to the received local state list
            ConfigurationClass.recmark.add(message.getMessageId());
            ConfigurationClass.reset_snap();
            ConfigurationClass.setmarkermessagerecv(true);
            int[] localClock = new int[TOTAL_NODE_COUNT];
            synchronized (ConfigurationClass.vector_clock) {
                System.arraycopy(ConfigurationClass.vector_clock, 0, localClock, 0, TOTAL_NODE_COUNT);
            }

            ProcessState myPayload = new ProcessState(ID, localClock,
                    ConfigurationClass.check_active(), ConfigurationClass.get_sent_msg_count(),
                    ConfigurationClass.get_rcv_msg_count());
            Logger.logMessage("Recording state : " + myPayload.toString());
            ConfigurationClass.add_localstate(myPayload);

            ConfigurationClass.setMarkerSender(message.getId());
            Logger.logMessage("Marker message received from " + message.getId() + "... BROADCAST\n"
                    + "Expecting replies = " + expectedSnapshotReplies);
            if(expectedSnapshotReplies == 0) {
                // Send consolidated local state reply
                Logger.logMessage("Received expected number of replies, send cumulative local states");
                ArrayList<ProcessState> snapshotPayload = new ArrayList<>();
                snapshotPayload.addAll(ConfigurationClass.getLocalStateAll());

                MessageModel replyStateMsg = new MessageModel(ID, snapshotPayload, 3);
                int markerSenderNode = ConfigurationClass.getMarkerSender();
                Logger.logMessage("Send snapshot reply to " + markerSenderNode
                        + " -> Message : " + replyStateMsg);
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


    private void mergeVectorClocks(MessageModel message) {
        int[] piggybackVectorClock = message.getData().get(0).getVectorClock();
        synchronized (ConfigurationClass.vector_clock) {
            for (int i = 0; i < TOTAL_NODE_COUNT; i++) {
                ConfigurationClass.vector_clock[i] = Math.max(ConfigurationClass.vector_clock[i], piggybackVectorClock[i]);
            }
            ConfigurationClass.vector_clock[ID]++;
        }
    }


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
                System.out.println("Exception Raised!");
                e.printStackTrace();
            }
        }
    
    }

    
}
