import javax.xml.bind.SchemaOutputResolver;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by rohallaj on 2/20/15.
 */
public class HTTPServer extends AbstractServer {
    private final int MAX_RETRY = 10;
    private ServerSocket socket;
    private Socket mClientSocket;
    public ServerSocket getSocket() {
        return socket;
    }

    public void setSocket(ServerSocket socket) {
        this.socket = socket;
    }

    public Socket getClientSocket() {
        return mClientSocket;
    }

    public void setClientSocket(Socket clientSocket) {
        mClientSocket = clientSocket;
    }

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
                    for (int i = 0; i <= MAX_RETRY; i++) {
                        while (getLeaveConnectionOpen()) {
                            boolean isEmpty = false;
                            do {
                                if (!getFromClientStream().ready()) {
//                                System.out.println("Client Stream not ready");
                                    break;
                                }
//                            System.out.println("before get request header");
                                ArrayList<String> requestHeader = getRequestHeader();

                                if (requestHeader != null && !requestHeader.isEmpty() && (requestHeader.contains("Connection: close\r\n") || requestHeader.get(0).contains("HTTP/1.0"))) {
//                                System.out.println("In check for close");
                                    setLeaveConnectionOpen(false);
                                }

                                if (requestHeader == null || requestHeader.isEmpty()) {
                                    System.out.println("Ignoring empty request...");
                                    isEmpty = true;
                                } else {
                                    String[] requests = requestHeader.get(0).split(" ");
                                    processRequest(requests[0], requests[1]);
                                }
                            } while (!isEmpty);
                        }
                    }
                    System.out.println(" ----- ENDED HTTP -----");
                } else {
                    System.out.println("Error accepting client connection.");
                }
            }catch(IOException e){
                System.out.println("Error communicating with client. aborting. Details: " + e);
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
            mClientSocket.setSoTimeout(10000);
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

    @Override
    public void serverCleanup() {
        super.serverCleanup();
        try {
            mClientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
