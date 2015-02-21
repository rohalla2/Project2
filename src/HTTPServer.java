import javax.xml.bind.SchemaOutputResolver;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by rohallaj on 2/20/15.
 */
public class HTTPServer extends AbstractServer {
    private final int SOCKET_LIVE_TIME = 5;
    @Override
    public void run() {
        super.run();
        loadRedirects();
        bind();
        // loop so server will begin listening again on the port once terminating a connection
        while(true) {
            try {
                setLeaveConnectionOpen(true);
                if (acceptFromClient()) {
                    System.out.println("----- NEW CLIENT CONNECTION ESTABLISHED -----");
                    while (getLeaveConnectionOpen()) {
                        int waitInterval = 0;
                        while(!getFromClientStream().ready()){
                            Thread.sleep(1000);
                            waitInterval++;
                            System.out.println("in wait loop " + waitInterval);
                            if(waitInterval == SOCKET_LIVE_TIME){
                                setLeaveConnectionOpen(false);
                                break;
                            }
                        }
                        System.out.println("Outside of wait loop");
                        if (waitInterval == SOCKET_LIVE_TIME) {
                            break;
                        }
                        ArrayList<String> requestHeader = getRequestHeader();
                        System.out.println("Before Check header for close connection");
                        if(requestHeader != null && !requestHeader.isEmpty() && ( requestHeader.contains("Connection: close\r\n") || requestHeader.get(0).contains("HTTP/1.0"))) {
                            System.out.println("In check for close");
                            setLeaveConnectionOpen(false);
                        }
                        System.out.println("After check");

                        System.out.println("Before process header");
                        // split the first line of the request
                        if (requestHeader == null || requestHeader.isEmpty()) {
                            System.out.println("Ignoring empty request...");
                        } else {
                            System.out.println("About to process request");
                            String[] requests = requestHeader.get(0).split(" ");
                            // process the request
                            processRequest(requests[0], requests[1]);
                        }
                    }
                    System.out.println(" ----- ENDED -----");
                } else {
                    System.out.println("Error accepting client connection.");
                }
            }catch(IOException e){
                System.out.println("Error communicating with client. aborting. Details: " + e);
            } catch (InterruptedException e){
                System.out.println(e);
                System.out.println(e);
            }
            // close sockets and buffered readers
            System.out.println("Cleaning UP");
            serverCleanup();
        }
    }


    public HTTPServer(int serverPort){
        super(serverPort);
    }

    @Override
    public void bind()  {
        try{
            setSocket(new ServerSocket(getServerPort()));
            System.out.println("HTTP Server bound and listening to port " + getServerPort());
        } catch (IOException e) {
            System.out.println("Error binding to port " + getServerPort());
        }
    }

    @Override
    public boolean acceptFromClient() throws IOException {
        try {
            setClientSocket(getSocket().accept());
        } catch (SecurityException e) {
            System.out.println("The security manager intervened; your config is very wrong. " + e);
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println("Probably an invalid port number. " + e);
            return false;
        }

        setToClientStream(new DataOutputStream(getClientSocket().getOutputStream()));
        setFromClientStream(new BufferedReader(new InputStreamReader(getClientSocket().getInputStream())));
        return true;
    }
}
