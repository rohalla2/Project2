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
                    while (getLeaveConnectionOpen()) {
                        int waitInterval = 0;
                        while(!getFromClientStream().ready()){
                            Thread.sleep(1000);
                            waitInterval++;
                            System.out.println("in wait loop " + waitInterval);
                            if(waitInterval == 5){
                                setLeaveConnectionOpen(false);
                                break;
                            }
                        }
                        ArrayList<String> requestHeader = getRequestHeader();
                        // split the first line of the request
                        if (requestHeader == null || requestHeader.isEmpty()) {
                            System.out.println("Ignoring empty request...");
                        } else {
                            String[] requests = requestHeader.get(0).split(" ");
                            // process the request
                            processRequest(requests[0], requests[1]);
                        }
                        if(requestHeader != null && !requestHeader.isEmpty() && ( requestHeader.contains("Connection: close\r\n") || requestHeader.get(0).contains("HTTP/1.0"))) {
                            setLeaveConnectionOpen(false);
                        }
                    }
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
