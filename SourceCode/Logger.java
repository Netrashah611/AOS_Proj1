/*
 * Class to handle the log message to be written in Log file name
*/

import java.io.*;
import java.util.Arrays;

public class Logger {
    public static int counter = 0;
    private static FileWriter writer;
    public static void initLogger(String logFileName) {
        if (writer == null) {
            try {
                writer = new FileWriter(logFileName);
            } catch (IOException ex) {
                System.out.println("Exception Raised! Error initializing the logger");
                System.err.println("Error initializing the logger : " + Arrays.toString(ex.getStackTrace()));
            }
        }
    }

    public static synchronized void logMessage(String message) {
        if (writer == null) {
            initLogger("config_log.out");
        }

        try {
            String strBefore = counter == 0 ? "" : "\n";
            writer.write(strBefore + message);
            writer.flush();
            counter++;
        } catch (IOException e) {
            System.out.println("Exception Raised! Error logging the message");
            System.err.println("Error logging the message : " + message);
        }
    }
}
