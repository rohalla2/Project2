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
                    ArrayList<String> x = getRequestHeader();
                    // split the first line of the request
                    if(x.isEmpty()){
                        System.out.println("Get request header is empty.");

                        // TODO: Handle this 501 - Not Implemented
                        String header = buildHeader(501, "Not Implemented", null);
                        // server.sendResponse(header, null);
                    }
                    else {
                        String[] requests = x.get(0).split(" ");
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
