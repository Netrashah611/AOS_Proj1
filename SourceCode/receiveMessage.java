/*
 * Class for receiver thread for the node which receives tokens sent to a given input stream
*/

import java.io.*;
import java.util.ArrayList;

public class receiveMessage implements Runnable {
    private final ArrayList<Integer> neighbors;
    public volatile boolean IS_RUNNING_FLAG = true;
    private static final int ID = ConfigurationClass.id;
    private final int neighborCount;
    private final int expectedSnapshotReplies;
    private final ObjectInputStream inputStream;
    private static final int FINAL_NODE_COUNT = ConfigurationClass.map_size;

    public receiveMessage(final ObjectInputStream inputStream,
            final ArrayList<Integer> neighbors) {
        this.inputStream = inputStream;
        this.neighbors = neighbors;
        this.neighborCount = neighbors.size();
        this.expectedSnapshotReplies = ID == 0 ? this.neighborCount : this.neighborCount - 1;
    }

    @Override
    public void run() {
        while(IS_RUNNING_FLAG) {
            try {
                MessageFramework message = (MessageFramework) inputStream.readObject();
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
                System.out.println("Exception Raised while handling the message!");
                e.printStackTrace();
            }
        }
    }

    private void handleFinishMessage(MessageFramework message) {
        MessageFramework broadcastMarkerMsg = new MessageFramework(ID, null, 5);
        for (Integer neighborId : neighbors) {
            if (neighborId != message.getId()) {
                launchSnapshotSender(neighborId, broadcastMarkerMsg);
            }
        }
        ConfigurationClass.setIsSystemTerminated(true);
        IS_RUNNING_FLAG = false;
    }

    private void handleSnapshotReplyMessage(MessageFramework message) {
        ConfigurationClass.incrementReceivedSnapshotReplies();
        if(message.getMessageType() == 4) {
            Logger.logMessage("Received reply from " + message.getId() + "of type ignore");         
        }
        else {           
            ConfigurationClass.addProcessStateAll(message.getData());
            Logger.logMessage("Received process state reply from " + message.getId() + " -> Received payload : " + message.getData());
        }
        
        if((ConfigurationClass.getReceivedSnapshotReplyCount() == expectedSnapshotReplies)) {        
            if (ID == 0) {
                ConfigurationClass.setsnaprep(true);
            }
            else {
                
                Logger.logMessage("Expected replies arrived, send cumulative process states");
                ArrayList<ProcessState> snapshotPayload = new ArrayList<>();
                snapshotPayload.addAll(ConfigurationClass.getProcessStateAll());

                MessageFramework replyStateMsg = new MessageFramework(ID, snapshotPayload, 3);
                int markerSenderNode = ConfigurationClass.getMarkerSender();
                Logger.logMessage("Send snapshot reply to " + markerSenderNode + " -> Message : " + replyStateMsg);
                launchSnapshotSender(markerSenderNode, replyStateMsg);
            }
        }
    }

    private void handleApplicationMessage(MessageFramework message) {
        
        ConfigurationClass.inc_rcv_msg_count();
        mergeVectorClocks(message);
        Logger.logMessage("Received Application message : " + message + "\nMerged clock : " + ConfigurationClass.displayGlobalClock());

        if (ConfigurationClass.check_active()) {
            Logger.logMessage("Already active ! ");
            return;
        }
        if (ConfigurationClass.get_sent_msg_count() >= ConfigurationClass.maxNumber) {
            Logger.logMessage("Maximum send limit reached, can not become active");
            return;
        }

        Logger.logMessage("Becoming active !");
        ConfigurationClass.set_active_status(true);
    }

    private void handleMarkerMessage(MessageFramework message) {
        ConfigurationClass.incCurrentMarkersReceivedCount();

        if (ConfigurationClass.recmark.contains(message.getMessageId()) || ID == 0) {
            Logger.logMessage("Marker message received from " + message.getId() + " -- Ignore !");
            MessageFramework replyMessage =  new MessageFramework(ID, null, 4);
            launchSnapshotSender(message.getId(), replyMessage);
        }
        else {
            ConfigurationClass.recmark.add(message.getMessageId());
            ConfigurationClass.reset_snap();
            ConfigurationClass.setmarkermessagerecv(true);
            int[] localClock = new int[FINAL_NODE_COUNT];
            synchronized (ConfigurationClass.vector_clock) {
                System.arraycopy(ConfigurationClass.vector_clock, 0, localClock, 0, FINAL_NODE_COUNT);
            }

            ProcessState myPayload = new ProcessState(ID, localClock,
                    ConfigurationClass.check_active(), ConfigurationClass.get_sent_msg_count(),
                    ConfigurationClass.get_rcv_msg_count());
            Logger.logMessage("Recording State: " + myPayload.toString());
            ConfigurationClass.addProcessState(myPayload);

            ConfigurationClass.setMarkerSender(message.getId());
            Logger.logMessage("Marker message received from " + message.getId() + "-- BROADCAST\n" + "Expecting replies = " + expectedSnapshotReplies);
            if(expectedSnapshotReplies == 0) {
                Logger.logMessage("Received expected number of replies, send cumulative process states");
                ArrayList<ProcessState> snapshotPayload = new ArrayList<>();
                snapshotPayload.addAll(ConfigurationClass.getProcessStateAll());

                MessageFramework replyStateMsg = new MessageFramework(ID, snapshotPayload, 3);
                int markerSenderNode = ConfigurationClass.getMarkerSender();
                Logger.logMessage("Send snapshot reply to " + markerSenderNode + " -> Message : " + replyStateMsg);
                launchSnapshotSender(markerSenderNode, replyStateMsg);

                return;
            }
            MessageFramework broadcastMarkerMsg = new MessageFramework(ID, null, 2, message.getMessageId());
            for (Integer neighborId : neighbors) {
                if (neighborId != message.getId()) {
                    launchSnapshotSender(neighborId, broadcastMarkerMsg);
                }
            }
        }
    }

    private void mergeVectorClocks(MessageFramework message) {
        int[] piggybackVectorClock = message.getData().get(0).getTheVectorClock();
        synchronized (ConfigurationClass.vector_clock) {
            for (int i = 0; i < FINAL_NODE_COUNT; i++) {
                ConfigurationClass.vector_clock[i] = Math.max(ConfigurationClass.vector_clock[i], piggybackVectorClock[i]);
            }
            ConfigurationClass.vector_clock[ID]++;
        }
    }

    private void launchSnapshotSender(int id, MessageFramework message) {
        SnapshotSender snapshotSender = new SnapshotSender(id, message);
        Thread thread = new Thread(snapshotSender);
        thread.start();
    }

    public class SnapshotSender implements Runnable {

        private final int nodeId;
        private final MessageFramework message;
        public SnapshotSender(final int id,
                final MessageFramework msg) {
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
                System.out.println("Exception Raised: IO while sending snapshot !");
                e.printStackTrace();
            }
        }
    }
}
