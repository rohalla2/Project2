import java.util.*;

/**
 * Created by rohallaj on 2/20/15.
 */
public class Driver {
    public static void main(String[] argv) {
        Map<String, String> flags = Utils.parseCmdlineFlags(argv);
        if (!flags.containsKey("--serverPort")) {
            System.out.println("usage: Driver --serverPort=12345 --sslServerPort=23456");
            System.exit(-1);
        }

        if (!flags.containsKey("--sslServerPort")) {
            System.out.println("useage: Driver --serverPort=12345 --sslServerPort=23456");
            System.exit(-1);
        }

        int serverPort = -1;
        int sslServerPort = -1;

        try {
            serverPort = Integer.parseInt(flags.get("--serverPort"));
            sslServerPort = Integer.parseInt(flags.get("--sslServerPort"));
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number! Must be an integer.");
            System.exit(-1);
        }

        HTTPServer httpServer = new HTTPServer(serverPort);
        SecureHTTPServer sslServer = new SecureHTTPServer(sslServerPort);
        httpServer.start();
        sslServer.start();

    }
}
