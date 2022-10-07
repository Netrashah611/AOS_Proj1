import java.util.*;

public class TermDetector implements Runnable {
    private static int markerId = 1;

    private static final long SNAPSHOT_DELAY = ConfigurationClass.delay_snap;
    private static final int ID = ConfigurationClass.id;
    private static final int TOTAL_NODE_COUNT = ConfigurationClass.map_size;

    private final ArrayList<Integer> neighbors;

    public volatile boolean is_running_flag = true;

    public TermDetector(final ArrayList<Integer> neighbors) {
        this.neighbors = neighbors;
    }

    public void sendMarkerMessages() {
        Logger.logMessage("Initiating snapshot !");
        // Add own local state to the received local state list
        int[] localClock = new int[TOTAL_NODE_COUNT];
        synchronized (ConfigurationClass.vector_clock) {
            System.arraycopy(ConfigurationClass.vector_clock, 0, localClock, 0, TOTAL_NODE_COUNT);
        }

        ProcessState myPayload = new ProcessState(ID, localClock,
                ConfigurationClass.check_active(), ConfigurationClass.get_sent_msg_count(),
                ConfigurationClass.get_rcv_msg_count());
        Logger.logMessage(myPayload.toString());
        ConfigurationClass.add_localstate(myPayload);
        broadcastMessage(2);
        markerId++;
    }

    private void sendFinishMessages() {
        Logger.logMessage("Sending the finish messages !");
        broadcastMessage(5);
    }

    private void broadcastMessage(int messageType) {
        MessageModel snapshotMessage = new MessageModel(ID, null, messageType, markerId);
        for (Integer neighborId : neighbors) {
            Thread thread = new Thread(new Postman(neighborId, snapshotMessage));
            thread.start();
        }
    }

    @Override
    public void run() {

        while (is_running_flag) {

            sendMarkerMessages();

            while (!ConfigurationClass.is_snap_reply()) {
                // Continue to wait till all replies received
            }

            TreeSet<ProcessState> replyPayloadList = new TreeSet<>(Comparator.comparingInt(ProcessState::getId));
            replyPayloadList.addAll(ConfigurationClass.getLocalStateAll());

            StringBuilder builder = new StringBuilder("_________Snapshot_________\n");
            for (ProcessState p : replyPayloadList) {
                builder.append(p.toString() + "\n");
            }
            builder.append("_______________");
            Logger.logMessage(builder.toString());

            if (is_system_terminated(replyPayloadList)) {
                Logger.logMessage("%%%%%%%%%%% System Terminated %%%%%%%%%%% ");
                sendFinishMessages();
                ConfigurationClass.setIsSystemTerminated(true);
                break;
            }
            else {
                Logger.logMessage(" %%%%%%%%%%% Not Terminated %%%%%%%%%%%");
            }

            // Reset snapshot variables
            ConfigurationClass.reset_snap();

            // If system not yet terminated, sleep for constant time
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

    private boolean is_system_terminated(TreeSet<ProcessState> payloads) {
        return isAllPassive(payloads) && isChannelsEmpty(payloads);
    }

    private boolean isAllPassive(TreeSet<ProcessState> payloads) {
        boolean isAnyActive = false;
        for (ProcessState payload : payloads) {
            isAnyActive |= payload.isActive();
        }
        return !isAnyActive;
    }

    private boolean isChannelsEmpty(TreeSet<ProcessState> payloads) {
        int totalSentCount = 0, totalReceiveCount = 0;
        for (ProcessState payload : payloads) {
            totalReceiveCount += payload.getReceivedMsgCount();
            totalSentCount += payload.getSentMsgCount();
        }
        return totalReceiveCount - totalSentCount == 0;
    }
}
