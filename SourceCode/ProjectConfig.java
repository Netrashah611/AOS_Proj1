// import java.io.*;
// import java.util.*;
// import java.nio.file.Paths;

// public class GlobalConfiguration {
//     private static final char COMMENT = '#';
//     private static final HashMap<Integer, NodeDetails> nodeMap = new HashMap<>();
//     private static ArrayList<Integer> neighborNodes = new ArrayList<>();

//     public static void setupApplicationEnvironment(String configFileName, int id) {
//         GlobalConfiguration.id = id;
//         Scanner lineScanner;
//         try (Scanner scanner = new Scanner(new File(configFileName))) {
//             String input = getNextValidInputLine(scanner);

//             lineScanner = new Scanner(input);

//             int clusterSize = lineScanner.nextInt();
//             GlobalConfiguration.map_size = clusterSize;
//             GlobalConfiguration.vector_clock = new int[clusterSize];

//             GlobalConfiguration.minActive = lineScanner.nextInt();
//             GlobalConfiguration.maxActive = lineScanner.nextInt();
//             GlobalConfiguration.minSendDelay = lineScanner.nextInt();
//             GlobalConfiguration.delay_snap = lineScanner.nextInt();
//             GlobalConfiguration.maxNumber = lineScanner.nextInt();
//             lineScanner.close();

//             input = getNextValidInputLine(scanner);

//             lineScanner = new Scanner(input);
//             int nodeNumber = lineScanner.nextInt();
//             String machineName = lineScanner.next();
//             int port = lineScanner.nextInt();

//             NodeDetails info = new NodeDetails(machineName, port);
//             nodeMap.put(nodeNumber, info);

//             for (int i = 1; i < clusterSize; i++) {
//                 String line = scanner.nextLine();

//                 lineScanner = new Scanner(line);
//                 nodeNumber = lineScanner.nextInt();
//                 machineName = lineScanner.next();
//                 port = lineScanner.nextInt();

//                 info = new NodeDetails(machineName, port);
//                 nodeMap.put(nodeNumber, info);
//             }
//             lineScanner.close();

//             input = getNextValidInputLine(scanner);

//             // neighbours
//             int lineNumber = 0;
//             ArrayList<Integer> neighbors = new ArrayList<>();
//             while (input != null) {
//                 lineScanner = new Scanner(input);
//                 if (lineNumber != id) {
//                     while (lineScanner.hasNext()) {
//                         String neighbor = lineScanner.next();
//                         if (neighbor.charAt(0) == COMMENT) {
//                             break;
//                         }
//                         int neighborId = Integer.parseInt(neighbor);
//                         if (neighborId == id && !neighbors.contains(lineNumber)) {
//                             neighbors.add(lineNumber);
//                         }
//                     }
//                 } else {
//                     while (lineScanner.hasNext()) {
//                         String neighbor = lineScanner.next();
//                         if (neighbor.charAt(0) == COMMENT) {
//                             break;
//                         }
//                         int neighborId = Integer.parseInt(neighbor);
//                         if (!neighbors.contains(neighborId) && neighborId != id) {
//                             neighbors.add(neighborId);
//                         }
//                     }
//                 }

//                 input = getNextValidInputLine(scanner);
//                 lineScanner.close();
//                 lineNumber++;
//             }
//             neighborNodes = neighbors;
//             GlobalConfiguration.number_of_neighbours = neighbors.size();

//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//     public static ArrayList<Integer> getNeighborNodes() {
//         return neighborNodes;
//     }

//     public static String getNextValidInputLine(Scanner scanner) {
//         String input = null;
//         while (scanner.hasNext()) {
//             input = scanner.nextLine();
//             if(input.isEmpty()) {
//                 continue;
//             }
//             if(input.charAt(0) != COMMENT) {
//                 break;
//             }
//        }
//         return input;
//     }

//     public static String getLogFileName(final int nodeId, final String configFileName) {
//         String fileName = Paths.get(configFileName).getFileName().toString();
//         return String.format("%s-%s.out", 
//                 fileName.substring(0, fileName.lastIndexOf('.')), nodeId);
//     }

//     public static HashMap<Integer, NodeDetails> getNodeMap() {
//         return nodeMap;
//     }
// }
