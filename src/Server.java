import javax.xml.bind.SchemaOutputResolver;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.net.SocketException;


/**
 * Created by rohallaj on 2/20/15.
 */
public class Server extends AbstractServer {
    private final int MAX_RETRY = 10;
    private ServerSocket socket;
    private Socket mClientSocket;
    public ServerSocket getSocket() {
        return socket;
    }

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

        Server httpServer = new Server(serverPort);
        HTTPSServer sslServer = new HTTPSServer(sslServerPort);
        httpServer.start();
        sslServer.start();
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
                if (acceptFromClient()) {
                    setLeaveConnectionOpen(true);
                    System.out.println("----- NEW CLIENT CONNECTION ESTABLISHED -----");
                    for (int i = 0; i <= MAX_RETRY; i++) {
                        ArrayList<String> requestHeader = getRequestHeader();
                        if (requestHeader == null || requestHeader.isEmpty()) {
                            setLeaveConnectionOpen(false);
                        } else {
                            String[] requests = requestHeader.get(0).split(" ");
                            if (requestHeader.contains("Connection: close") || requestHeader.get(0).contains("HTTP/1.0")) {
                                setLeaveConnectionOpen(false);
                            }
                            processRequest(requests[0], requests[1]);
                            System.out.println("process request");
                            if (!getLeaveConnectionOpen()) {
                                break;
                            }
                        }
                    }
                    System.out.println(" ----- ENDED HTTP -----");
                } else {
                    System.out.println("Error accepting client connection.");
                }
            }catch(SocketException e){
                System.out.println("Client has not connected in 10 seconds. Closing Socket.");
            }catch(IOException e){
                System.out.println("Error communicating with client. aborting. Details: " + e);
            }

            // close sockets and buffered readers
            System.out.println("Cleaning UP");
            serverCleanup();
        }
    }


    public Server(int serverPort){
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
            mClientSocket =  (socket.accept());
            mClientSocket.setSoTimeout(10000);
        } catch (SecurityException e) {
            System.out.println("The security manager intervened; your config is very wrong. " + e);
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println("Probably an invalid port number. " + e);
            return false;
        }

        setToClientStream(new DataOutputStream(mClientSocket.getOutputStream()));
        setFromClientStream(new BufferedReader(new InputStreamReader(mClientSocket.getInputStream())));
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
