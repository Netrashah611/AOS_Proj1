import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/*
 Runnable Class which starts a thread to randomly select a neighbour
 and send messages on any available stream when the node is active
 */
public class SendMessage implements Runnable {

    private static final ArrayList<Integer> NEIGHBOURS = GlobalConfiguration.getNeighborNodes();
    private static final int ID = GlobalConfiguration.id;
    private static final int TOTAL_NODE_COUNT = GlobalConfiguration.map_size;
    private static final int MIN_PER_ACTIVE = GlobalConfiguration.minActive;
    private static final int MAX_PER_ACTIVE = GlobalConfiguration.maxActive;
    private static final int ACTIVE_DIFFERENCE = MAX_PER_ACTIVE - MIN_PER_ACTIVE;
    private static final long MIN_SEND_DELAY = GlobalConfiguration.minSendDelay;
    private static final HashMap<Integer, ObjectOutputStream> OUTPUTSTREAM_MAP = NetworkOperations.writerStreamMap;
    private static final Random RANDOM_SELECTION = GlobalConfiguration.RANDOM;
    public static volatile boolean is_running_flag = true;

    public SendMessage() {
    }

    @Override
    public void run() {
        while (is_running_flag) {

            if (!GlobalConfiguration.check_active()) {
                continue;
            }

            int countMsgsToSend = RANDOM_SELECTION.nextInt(ACTIVE_DIFFERENCE) + MIN_PER_ACTIVE;
            sendApplicationMessages(countMsgsToSend);
            GlobalConfiguration.set_active_status(false);

            if (GlobalConfiguration.get_sent_msg_count() >= GlobalConfiguration.maxNumber) {

                is_running_flag = false;
            }
        }
    }

    /*
     Function to generate randomly generated neighbour id
     */
    private int getRandomNeighbourNode() {
        int randomIndex = new Random().nextInt(100) % NEIGHBOURS.size();
        return NEIGHBOURS.get(randomIndex);
    }

    /*
      Function which selects a neighbor randomly and sends message to it.
      The execution is stopped if total sent messages reach the max limit.
     */
    private void sendApplicationMessages(int countMsgsToSend) {
        for (int i = 0; i < countMsgsToSend; i++) {
            int nextNodeId = getRandomNeighbourNode();
            // Send to this random node
            ObjectOutputStream outputStream = OUTPUTSTREAM_MAP.get(nextNodeId);
            MessageModel message = null;
            int[] localClock = new int[TOTAL_NODE_COUNT];
            synchronized (GlobalConfiguration.vector_clock) {
                GlobalConfiguration.vector_clock[ID]++;
                System.arraycopy(GlobalConfiguration.vector_clock, 0, localClock, 0, TOTAL_NODE_COUNT);
            }
            local_state p = new local_state(localClock);
            ArrayList<local_state> payloads = new ArrayList<>();
            payloads.add(p);
            message = new MessageModel(ID, payloads, 1);
            
            try {
                synchronized (outputStream) {
                    outputStream.writeObject(message);
                }
                GlobalConfiguration.inc_sent_msg_count();

                if (GlobalConfiguration.get_sent_msg_count() >= GlobalConfiguration.maxNumber) {
                    is_running_flag = false;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(MIN_SEND_DELAY);
            } catch (InterruptedException exp) {
                Thread.currentThread().interrupt();
                exp.printStackTrace();
            }
        }
    }
}
