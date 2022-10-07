import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Logger {
    private static FileWriter writer;

    public static int counter = 0;

    public static void initLogger(String logFileName) {
        if (writer == null) {
            try {
                writer = new FileWriter(logFileName);
            } catch (IOException ex) {
                System.err.println("Error initializing the logger : " + Arrays.toString(ex.getStackTrace()));
            }
        }
    }

    public static synchronized void logMessage(String message) {
        if (writer == null) {
            initLogger("config-log.out");
        }

        try {
            String strBefore = counter == 0 ? "" : "\n";
            writer.write(strBefore + message);
            writer.flush();
            counter++;
        } catch (IOException e) {
            System.err.println("Error logging the message : " + message);
        }
    }
}
