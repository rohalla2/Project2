import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by rohallaj on 2/20/15.
 */
public class HTTPServer extends AbstractServer {

    @Override
    public void run() {
        super.run();
        loadRedirects();
        bind();
        // loop so server will begin listening again on the port once terminating a connection
        while(true) {
            try{
                if (acceptFromClient()) {
                    ArrayList<String> requestHeader = getRequestHeader();
                    // split the first line of the request
                    if(requestHeader == null || requestHeader.isEmpty()){
                        System.out.println("Ignoring empty request...");
                    } else {
                        String[] requests = requestHeader.get(0).split(" ");
                        // process the request
                        processRequest(requests[0], requests[1]);
                    }

                } else {
                    System.out.println("Error accepting client connection.");
                }
            } catch (IOException e) {
                System.out.println("Error communicating with client. aborting. Details: " + e);
            }
            // close sockets and buffered readers
            serverCleanup();
        }
    }


    public HTTPServer(int serverPort){
        super(serverPort);
    }

}
