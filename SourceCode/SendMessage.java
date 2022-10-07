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

    private static final ArrayList<Integer> NEIGHBOURS = ConfigurationClass.getNeighborNodes();
    private static final int ID = ConfigurationClass.id;
    private static final int TOTAL_NODE_COUNT = ConfigurationClass.map_size;
    private static final int MIN_PER_ACTIVE = ConfigurationClass.minActive;
    private static final int MAX_PER_ACTIVE = ConfigurationClass.maxActive;
    private static final int ACTIVE_DIFFERENCE = MAX_PER_ACTIVE - MIN_PER_ACTIVE;
    private static final long MIN_SEND_DELAY = ConfigurationClass.minSendDelay;
    private static final HashMap<Integer, ObjectOutputStream> OUTPUTSTREAM_MAP = NetworkOperations.writerStreamMap;
    private static final Random RANDOM_SELECTION = ConfigurationClass.RANDOM;
    public static volatile boolean is_running_flag = true;

    public SendMessage() {
    }

    @Override
    public void run() {
        while (is_running_flag) {

            if (!ConfigurationClass.check_active()) {
                continue;
            }

            int countMsgsToSend = RANDOM_SELECTION.nextInt(ACTIVE_DIFFERENCE) + MIN_PER_ACTIVE;
            sendApplicationMessages(countMsgsToSend);
            ConfigurationClass.set_active_status(false);

            if (ConfigurationClass.get_sent_msg_count() >= ConfigurationClass.maxNumber) {

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
            synchronized (ConfigurationClass.vector_clock) {
                ConfigurationClass.vector_clock[ID]++;
                System.arraycopy(ConfigurationClass.vector_clock, 0, localClock, 0, TOTAL_NODE_COUNT);
            }
            ProcessState p = new ProcessState(localClock);
            ArrayList<ProcessState> payloads = new ArrayList<>();
            payloads.add(p);
            message = new MessageModel(ID, payloads, 1);
            
            try {
                synchronized (outputStream) {
                    outputStream.writeObject(message);
                }
                ConfigurationClass.inc_sent_msg_count();

                if (ConfigurationClass.get_sent_msg_count() >= ConfigurationClass.maxNumber) {
                    is_running_flag = false;
                    break;
                }
            } catch (IOException e) {
                System.out.println("Exception Raised : IO while sending message!");
                e.printStackTrace();
            }

            try {
                Thread.sleep(MIN_SEND_DELAY);
            } catch (InterruptedException exp) {
                System.out.println("Exception Raised : Interrupted while sending  message!");
                Thread.currentThread().interrupt();
                exp.printStackTrace();
            }
        }
    }
}
