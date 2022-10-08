/*
 * When a termination message is discovered, the system is terminated by the Termination Detector thread class
*/

import java.util.*;

public class TerminationDetector implements Runnable {
    private static int MARKER_ID = 1;
    private final ArrayList<Integer> neighbors;
    private static final int ID = ConfigurationClass.id;
    private static final long SNAPSHOT_DELAY = ConfigurationClass.delay_snap;
    private static final int FINAL_NODE_COUNT = ConfigurationClass.map_size;
    public volatile boolean IS_RUNNING_FLAG = true;

    public TerminationDetector(final ArrayList<Integer> neighbors) {
        this.neighbors = neighbors;
    }

    public void sendTheMarkerMessages() {
        Logger.logMessage("Initiating snapshot !");
        int[] localClock = new int[FINAL_NODE_COUNT];
        synchronized (ConfigurationClass.vector_clock) {
            System.arraycopy(ConfigurationClass.vector_clock, 0, localClock, 0, FINAL_NODE_COUNT);
        }

        ProcessState myPayload = new ProcessState(ID, localClock, ConfigurationClass.check_active(), ConfigurationClass.get_sent_msg_count(), ConfigurationClass.get_rcv_msg_count());
        Logger.logMessage(myPayload.toString());
        ConfigurationClass.addProcessState(myPayload);
        broadcastMessage(2);
        MARKER_ID++;
    }

    private void sendtheFinishMessages() {
        Logger.logMessage("Sending the finish messages !");
        broadcastMessage(5);
    }

    private void broadcastMessage(int messageType) {
        MessageFramework snapshotMessage = new MessageFramework(ID, null, messageType, MARKER_ID);
        for (Integer neighborId : neighbors) {
            Thread thread = new Thread(new Postman(neighborId, snapshotMessage));
            thread.start();
        }
    }

    @Override
    public void run() {
        while (IS_RUNNING_FLAG) {
            sendTheMarkerMessages();
            while (!ConfigurationClass.is_snap_reply()) {
                // Wait till all replies arrives
            }

            TreeSet<ProcessState> replyForPayloadList = new TreeSet<>(Comparator.comparingInt(ProcessState::getId));
            replyForPayloadList.addAll(ConfigurationClass.getProcessStateAll());
            StringBuilder builder = new StringBuilder("_________Snapshot_________\n");
            for (ProcessState p : replyForPayloadList) {
                builder.append(p.toString() + "\n");
            }
            builder.append("_______________");
            Logger.logMessage(builder.toString());

            if (is_system_terminated(replyForPayloadList)) {
                Logger.logMessage("%%%%%%%%%%% System Terminated %%%%%%%%%%% ");
                sendtheFinishMessages();
                ConfigurationClass.setIsSystemTerminated(true);
                break;
            }
            else {
                Logger.logMessage(" %%%%%%%%%%% Not Terminated %%%%%%%%%%%");
            }
            ConfigurationClass.reset_snap();
            try {
                Logger.logMessage("Snapshot process sleeping! " + SNAPSHOT_DELAY);
                Thread.sleep(SNAPSHOT_DELAY);
            } catch (InterruptedException e) {
                System.out.println("Exception Raised : Interrupted while trying to sleep!");
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    private boolean areAllPassive(TreeSet<ProcessState> payloads) {
        boolean isAnyActive = false;
        for (ProcessState payload : payloads) {
            isAnyActive |= payload.isActive();
        }
        return !isAnyActive;
    }

    private boolean areChannelsEmpty(TreeSet<ProcessState> payloads) {
        int totalOfSentCount = 0, totalOfReceiveCount = 0;
        for (ProcessState payload : payloads) {
            totalOfReceiveCount += payload.getreceivedMessagesCount();
            totalOfSentCount += payload.getsentMessagesCount();
        }
        return totalOfReceiveCount - totalOfSentCount == 0;
    }

    private boolean is_system_terminated(TreeSet<ProcessState> payloads) {
        return areAllPassive(payloads) && areChannelsEmpty(payloads);
    }
}
