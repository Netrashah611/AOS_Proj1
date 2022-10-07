import java.util.*;

public class TermDetector implements Runnable {
    private static int markerId = 1;

    private static final long SNAPSHOT_DELAY = GlobalConfiguration.delay_snap;
    private static final int ID = GlobalConfiguration.id;
    private static final int TOTAL_NODE_COUNT = GlobalConfiguration.map_size;

    private final ArrayList<Integer> neighbors;

    public volatile boolean is_running_flag = true;

    public TermDetector(final ArrayList<Integer> neighbors) {
        this.neighbors = neighbors;
    }

    public void sendMarkerMessages() {
        Logger.logMessage("Initiating snapshot...");
        // Add own local state to the received local state list
        int[] localClock = new int[TOTAL_NODE_COUNT];
        synchronized (GlobalConfiguration.vector_clock) {
            System.arraycopy(GlobalConfiguration.vector_clock, 0, localClock, 0, TOTAL_NODE_COUNT);
        }

        local_state myPayload = new local_state(ID, localClock,
                GlobalConfiguration.check_active(), GlobalConfiguration.get_sent_msg_count(),
                GlobalConfiguration.get_rcv_msg_count());
        Logger.logMessage(myPayload.toString());
        GlobalConfiguration.add_localstate(myPayload);
        broadcastMessage(2);
        markerId++;
    }

    private void sendFinishMessages() {
        Logger.logMessage("Sending finish messages...");
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

            while (!GlobalConfiguration.is_snap_reply()) {
                // Continue to wait till all replies received
            }

            TreeSet<local_state> replyPayloadList = new TreeSet<>(Comparator.comparingInt(local_state::getId));
            replyPayloadList.addAll(GlobalConfiguration.getLocalStateAll());

            StringBuilder builder = new StringBuilder("--------------Snapshot---------------\n");
            for (local_state p : replyPayloadList) {
                builder.append(p.toString() + "\n");
            }
            builder.append("-------------------------------------");
            Logger.logMessage(builder.toString());

            if (is_system_terminated(replyPayloadList)) {
                Logger.logMessage("********************System terminated...");
                sendFinishMessages();
                GlobalConfiguration.setIsSystemTerminated(true);
                break;
            }
            else {
                Logger.logMessage("********************NOT terminated...");
            }

            // Reset snapshot variables
            GlobalConfiguration.reset_snap();

            // If system not yet terminated, sleep for constant time
            try {
                Logger.logMessage("Snapshot process sleeping... " + SNAPSHOT_DELAY);
                Thread.sleep(SNAPSHOT_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

        }
    }

    private boolean is_system_terminated(TreeSet<local_state> payloads) {
        return isAllPassive(payloads) && isChannelsEmpty(payloads);
    }

    private boolean isAllPassive(TreeSet<local_state> payloads) {
        boolean isAnyActive = false;
        for (local_state payload : payloads) {
            isAnyActive |= payload.isActive();
        }
        return !isAnyActive;
    }

    private boolean isChannelsEmpty(TreeSet<local_state> payloads) {
        int totalSentCount = 0, totalReceiveCount = 0;
        for (local_state payload : payloads) {
            totalReceiveCount += payload.getReceivedMsgCount();
            totalSentCount += payload.getSentMsgCount();
        }
        return totalReceiveCount - totalSentCount == 0;
    }
}
