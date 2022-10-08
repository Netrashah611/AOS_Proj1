// Applcation.java has the Main file 
/*
 * 1. It creates entries for connection sockets, input streams, and output streams in global maps and establishes connections with the neighbors
 * 2. Adds entries to global maps and requests a socket connection to the input neighbors. 
 * 3. Starts a thread and gives the sender and recipient threads the thread handle. 
 * 4. Starts a thread and gives the snapshot-taking thread its handle back.
*/

import java.io.*;
import java.util.*;
import java.nio.*;
import java.net.*;

public class Application {
    private int id;
    private HashMap<Integer, NodeDetails> node_lst;
    private NodeDetails host_node;
    private boolean checkInitial;
    private ArrayList<Integer> neighbors;
    private ServerSocket listener_socket;

    public Application(int nodeId) throws IOException {
        this.id = nodeId;
        this.checkInitial = nodeId == 0; // boolean
        this.node_lst = ConfigurationClass.getNodeMap();
        this.host_node = node_lst.get(nodeId);
        this.neighbors = ConfigurationClass.getNeighborNodes(); // will return list of neighbours as array list
        this.listener_socket = new ServerSocket(host_node.getPortNo()); // staring the server with given port number and connection is established
    }

    public void createConnection() throws InterruptedException, IOException {
        // Launch listener thread
        Listener listener = new Listener(listener_socket, neighbors);
        Thread ThreadListener = new Thread(listener);
        ThreadListener.start();
        Thread.sleep(AppConstants.DEFAULT_THREAD_SLEEP_MS);
        // Connect to all the neighbors
        generateSocketsForAllNeighbors();

        while (NetworkOperations.getsocketHashMapSize() < neighbors.size()) {
            System.out.println("Waiting for all nodes to be initialized");
            Thread.sleep(AppConstants.DEFAULT_THREAD_SLEEP_MS);
        }
        ThreadListener.interrupt();
    }

    private void generateSocketsForAllNeighbors() throws InterruptedException, IOException {
        int index = 0;
        while (index < neighbors.size()) {
            if (!NetworkOperations.containsSocketEntryInMap(neighbors.get(index)) && neighbors.get(index) > id) {
                createSocket(neighbors.get(index));
            }
            index++;
        }
    }

    public static synchronized void printdetails(int index, Socket socket) {
        System.out.println("Socket"+index+socket);
    }

    private void createSocket(int nodeId) throws IOException {
        NodeDetails info = node_lst.get(nodeId);
        Logger.logMessage("Socket connection in progress for " + nodeId);
        Socket sock = null;
        boolean connected = false;
        while (!connected) {
            try {
                sock = new Socket(info.getHostName(), info.getPortNo());
                System.out.println("Socket Connected !");
                connected = true;
            } catch (ConnectException ce) {
                System.out.println("Couldn't connect socket, retrying...");
                Logger.logMessage("Retrying socket connection ");
            }
        }
        Logger.logMessage("Successfully created a socket connection : " + nodeId);

        NetworkOperations.addSocketToSocketEntry(nodeId, sock);

        // add socket's i/p stream to ObjectOutputStream
        addToOutputStream(sock, nodeId);
    }

    private void launchReceiverThreads() throws InterruptedException {
        ArrayList<Thread> rcvThreadCollectionArray = new ArrayList<>();
        for (Integer neighborId : neighbors) {
            ObjectInputStream stream = NetworkOperations.getReaderStream(neighborId);
            receiveMessage receiver = new receiveMessage(stream, neighbors);
            Thread thread = new Thread(receiver);
            thread.start();
            rcvThreadCollectionArray.add(thread);
        }
        Thread.sleep(AppConstants.DEFAULT_THREAD_SLEEP_MS);
    }

    private void launchSenderThread() {
        // Launch sender thread
        SendMessage sender = new SendMessage();
        Thread thread = new Thread(sender);
        thread.start();
    }

    private void addToOutputStream(Socket sock, int nodeId) throws IOException {
        ByteBuffer bbuffer = ByteBuffer.allocate(4);
        bbuffer.putInt(id);
        ObjectOutputStream ooStream = new ObjectOutputStream(sock.getOutputStream());
        byte[] bytes = bbuffer.array();
        ooStream.write(bytes);
        ooStream.flush();
        ooStream.reset();
        NetworkOperations.addOutStreamEntry(nodeId, ooStream);
        NetworkOperations.addInStreamEntry(nodeId, new ObjectInputStream(sock.getInputStream()));
    }

    @Override
    public String toString() {
        return " Node -> " + id + "  Host Node -> " + host_node + "  Neighbor Nodes -> " + neighbors
                + "  Node Lists -> " + node_lst;
    }

    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);  // node id through args
        String configurationFileName = args[1];
        Logger.initLogger(ConfigurationClass.getLogFileName(id, configurationFileName));
        ConfigurationClass.setupApplicationEnvironment(configurationFileName, id); // parse the i/p and updates the neighbours and size of neighbours. Doesn't return anything

        Application cNode;
        try {
            cNode = new Application(id);
            Logger.logMessage(cNode.toString()); //prints connection status
            cNode.createConnection();

            cNode.launchReceiverThreads();
            cNode.launchSenderThread();

            ConfigurationClass.set_active_status(id % 2 == 0);
            cNode.termDetector();

            while(!ConfigurationClass.is_system_terminated()) {
            }

            Thread.sleep(AppConstants.DEFAULT_THREAD_SLEEP_MS);
            System.exit(AppConstants.SUCCESSFULL_PROGRAM_TERMINATION_EXIT_CODE);

        } catch (IOException exp) {
            System.out.println("Exception Raised : IO during initialization !");
            exp.printStackTrace();
        } catch (InterruptedException excp) {
            System.out.println("Exception Raised! : Interrupted during initialization");
            Thread.currentThread().interrupt();
            excp.printStackTrace();
        }

        System.exit(AppConstants.SUCCESSFULL_PROGRAM_TERMINATION_EXIT_CODE);
    }

    private void termDetector() {
        if (checkInitial) {
            Thread thread = new Thread(new TerminationDetector(neighbors));
            thread.start();
        }
    }
}
