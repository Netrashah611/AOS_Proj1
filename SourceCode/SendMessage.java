/*
 * Runnable Class that, while the node is active, 
 * launches a thread to transmit messages on any open stream to a randomly chosen neighbor.
*/

import java.io.*;
import java.util.*;

public class SendMessage implements Runnable {

    private static final ArrayList<Integer> NEIGHBOURS = ConfigurationClass.getNeighborNodes();
    private static final int ID = ConfigurationClass.id;
    private static final Random RANDOM_SELECTION = ConfigurationClass.RANDOM;
    public static volatile boolean IS_RUNNING_FLAG = true;
    private static final int FINAL_NODE_COUNT = ConfigurationClass.map_size;
    private static final int MIN_ACTIVE = ConfigurationClass.minActive;
    private static final int MAX_ACTIVE = ConfigurationClass.maxActive;
    private static final int ACTIVE_DIFF = MAX_ACTIVE - MIN_ACTIVE;
    private static final long MIN_SEND_DELAY = ConfigurationClass.minSendDelay;
    private static final HashMap<Integer, ObjectOutputStream> OUTPUTSTREAM_MAP = NetworkOperations.writerStreamHashMap;
    
    public SendMessage() {
    }

    @Override
    public void run() {
        while (IS_RUNNING_FLAG) {

            if (!ConfigurationClass.check_active()) {
                continue;
            }

            int countMsgsToSend = RANDOM_SELECTION.nextInt(ACTIVE_DIFF) + MIN_ACTIVE;
            sendApplicationMessages(countMsgsToSend);
            ConfigurationClass.set_active_status(false);

            if (ConfigurationClass.get_sent_msg_count() >= ConfigurationClass.maxNumber) {
                IS_RUNNING_FLAG = false;
            }
        }
    }

 
    private int getRandomNeighbourNode() {
        int randomIndex = new Random().nextInt(500) % NEIGHBOURS.size();
        return NEIGHBOURS.get(randomIndex);
    }

    private void sendApplicationMessages(int countMsgsToSend) {
        for (int i = 0; i < countMsgsToSend; i++) {
            int nextNodeId = getRandomNeighbourNode();
            ObjectOutputStream outputStream = OUTPUTSTREAM_MAP.get(nextNodeId);
            MessageFramework message = null;
            int[] localClock = new int[FINAL_NODE_COUNT];
            synchronized (ConfigurationClass.vector_clock) {
                ConfigurationClass.vector_clock[ID]++;
                System.arraycopy(ConfigurationClass.vector_clock, 0, localClock, 0, FINAL_NODE_COUNT);
            }
            ProcessState p = new ProcessState(localClock);
            ArrayList<ProcessState> payloads = new ArrayList<>();
            payloads.add(p);
            message = new MessageFramework(ID, payloads, 1);
            
            try {
                synchronized (outputStream) {
                    outputStream.writeObject(message);
                }
                ConfigurationClass.inc_sent_msg_count();

                if (ConfigurationClass.get_sent_msg_count() >= ConfigurationClass.maxNumber) {
                    IS_RUNNING_FLAG = false;
                    break;
                }
            } catch (IOException e) {
                System.out.println("Exception Raised : IO while sending message!");
                e.printStackTrace();
            }

            try {
                Thread.sleep(MIN_SEND_DELAY);
            } catch (InterruptedException exp) {
                System.out.println("Exception Raised : Interrupted while sending message!");
                Thread.currentThread().interrupt();
                exp.printStackTrace();
            }
        }
    }
}
