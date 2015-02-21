import java.util.*;

/**
 * Created by rohallaj on 2/20/15.
 */
public class Driver {
    public static void main(String[] argv) {
        Map<String, String> flags = Utils.parseCmdlineFlags(argv);
        if (!flags.containsKey("--serverPort")) {
            System.out.println("usage: Server --serverPort=12345");
            System.exit(-1);
        }

        int serverPort = -1;
        try {
            serverPort = Integer.parseInt(flags.get("--serverPort"));
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number! Must be an integer.");
            System.exit(-1);
        }

        HTTPServer server1 = new HTTPServer(serverPort);
        HTTPServer server = new HTTPServer(serverPort+1);
        server1.start();
        server.start();

    }
}
