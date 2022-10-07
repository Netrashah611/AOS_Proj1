import java.io.*;
import java.util.*;
import java.nio.file.Paths;

public class GlobalConfiguration {
    private static final char COMMENT = '#';
    private static final HashMap<Integer, NodeDetails> nodeMap = new HashMap<>();
    private static ArrayList<Integer> neighborNodes = new ArrayList<>();
    public static int sent_msg_count = 0;
    public static int received_msg_count = 0;
    public static boolean active = false;
    public static boolean snapshotrep = false;
    public static boolean system_terminated = false;
    public static boolean recvMarkMsg = false;
    public static boolean is_recv_snap_reply = false;
    public static int received_snapshot_reply_count = 0;
    public static int send_marker_node;
    public static int recvd_mark_count = 0;
    public static HashSet<Integer> recmark = new HashSet<>();
    public static final Random RANDOM = new Random();
    public static int id;
    public static long minSendDelay;
    public static int minActive;
    public static int maxActive;
    public static int maxNumber;
    public static int map_size;
    public static int[] vector_clock;
    public static int number_of_neighbours;
    public static long delay_snap;

    public static HashMap<Integer, NodeDetails> getNodeMap() {
        return nodeMap;
    }
    public static ArrayList<local_state> local = new ArrayList<>();

    public static String displayGlobalClock() {
        StringBuilder builder = new StringBuilder("VectorClock : [ ");
        for(int i = 0; i < vector_clock.length; i++) {
            builder.append(vector_clock[i] + " ");
        }
        builder.append("]");
        return builder.toString();

    }

    public static synchronized int get_sent_msg_count() {
        return sent_msg_count;
    }

    public static synchronized void inc_sent_msg_count() {
        sent_msg_count++;
    }

    public static synchronized int get_rcv_msg_count() {
        return received_msg_count;
    }

    public static synchronized void inc_rcv_msg_count() {
        received_msg_count++;
    }

    public static synchronized boolean check_active() {
        return active;
    }

    public static synchronized int getSentMsgCount() {
        return sent_msg_count;
    }

    public static synchronized void set_active_status(boolean isNodeActive) {
        active = isNodeActive;
    }

    public static synchronized int getRcvMsgCount() {
        return received_msg_count;
    }

    // //not used anywhere
    public static synchronized void setmarkermessagerecv(boolean markerReceived) {
        recvMarkMsg = markerReceived;
    }

    public static synchronized boolean is_snap_reply() {
        return is_recv_snap_reply;
    }

    public static synchronized void setsnaprep(boolean allSnapshotReceived) {
        is_recv_snap_reply = allSnapshotReceived;
    }

    public static synchronized boolean is_system_terminated() {
        return system_terminated;
    }

    public static synchronized void setIsSystemTerminated(boolean systemTerminated) {
        system_terminated = systemTerminated;
    }

    public static synchronized int getMarkerSender() {
        return send_marker_node;
    }

    public static synchronized void setMarkerSender(int markerSender) {
        send_marker_node = markerSender;
    }

    public static synchronized int getReceivedSnapshotReplyCount() {
        return received_snapshot_reply_count;
    }

    public static synchronized int getMarker() {
        return send_marker_node;
    }

    public static synchronized void incrementReceivedSnapshotReplies() {
        received_snapshot_reply_count++;
    }

    public static synchronized void incCurrentMarkersReceivedCount() {
        recvd_mark_count++;
    }

    public static synchronized ArrayList<local_state> getLocalStateAll() {
        return local;
    }

    public static synchronized void addLocalStateAll(ArrayList<local_state> payload) {
        local.addAll(payload);
    }

    public static synchronized void add_localstate(local_state payload) {
        local.add(payload);
    }

    public static void setupApplicationEnvironment(String configFileName, int id) {
        GlobalConfiguration.id = id;
        Scanner lineScanner;
        try (Scanner scanner = new Scanner(new File(configFileName))) {
            String input = getNextValidInputLine(scanner);

            lineScanner = new Scanner(input);

            int clusterSize = lineScanner.nextInt();
            GlobalConfiguration.map_size = clusterSize;
            GlobalConfiguration.vector_clock = new int[clusterSize];

            GlobalConfiguration.minActive = lineScanner.nextInt();
            GlobalConfiguration.maxActive = lineScanner.nextInt();
            GlobalConfiguration.minSendDelay = lineScanner.nextInt();
            GlobalConfiguration.delay_snap = lineScanner.nextInt();
            GlobalConfiguration.maxNumber = lineScanner.nextInt();
            lineScanner.close();

            input = getNextValidInputLine(scanner);

            lineScanner = new Scanner(input);
            int nodeNumber = lineScanner.nextInt();
            String machineName = lineScanner.next();
            int port = lineScanner.nextInt();

            NodeDetails info = new NodeDetails(machineName, port);
            nodeMap.put(nodeNumber, info);

            for (int i = 1; i < clusterSize; i++) {
                String line = scanner.nextLine();

                lineScanner = new Scanner(line);
                nodeNumber = lineScanner.nextInt();
                machineName = lineScanner.next();
                port = lineScanner.nextInt();

                info = new NodeDetails(machineName, port);
                nodeMap.put(nodeNumber, info);
            }
            lineScanner.close();

            input = getNextValidInputLine(scanner);

            // neighbours
            int lineNumber = 0;
            ArrayList<Integer> neighbors = new ArrayList<>();
            while (input != null) {
                lineScanner = new Scanner(input);
                if (lineNumber != id) {
                    while (lineScanner.hasNext()) {
                        String neighbor = lineScanner.next();
                        if (neighbor.charAt(0) == COMMENT) {
                            break;
                        }
                        int neighborId = Integer.parseInt(neighbor);
                        if (neighborId == id && !neighbors.contains(lineNumber)) {
                            neighbors.add(lineNumber);
                        }
                    }
                } else {
                    while (lineScanner.hasNext()) {
                        String neighbor = lineScanner.next();
                        if (neighbor.charAt(0) == COMMENT) {
                            break;
                        }
                        int neighborId = Integer.parseInt(neighbor);
                        if (!neighbors.contains(neighborId) && neighborId != id) {
                            neighbors.add(neighborId);
                        }
                    }
                }

                input = getNextValidInputLine(scanner);
                lineScanner.close();
                lineNumber++;
            }
            neighborNodes = neighbors;
            GlobalConfiguration.number_of_neighbours = neighbors.size();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Integer> getNeighborNodes() {
        return neighborNodes;
    }

    public static String getNextValidInputLine(Scanner scanner) {
        String input = null;
        while (scanner.hasNext()) {
            input = scanner.nextLine();
            if(input.isEmpty()) {
                continue;
            }
            if(input.charAt(0) != COMMENT) {
                break;
            }
       }
        return input;
    }

    public static String getLogFileName(final int nodeId, final String configFileName) {
        String fileName = Paths.get(configFileName).getFileName().toString();
        return String.format("%s-%s.out", 
                fileName.substring(0, fileName.lastIndexOf('.')), nodeId);
    }

    
    public static synchronized void reset_snap() {
        local.clear();
        local = new ArrayList<>();
        received_snapshot_reply_count = 0;
        is_recv_snap_reply = false;
        recvMarkMsg = id == 0;
        snapshotrep = false;
    }
}
