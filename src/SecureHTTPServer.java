import javax.net.ssl.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * Created by rohallaj on 2/20/15.
 */
public class SecureHTTPServer extends AbstractServer {
    private final int SOCKET_LIVE_TIME = 5;
    private SSLServerSocket socket;
    private Socket mClientSocket;
    private KeyStore mKeyStore;
    //private SSLServerSocket mSSLServerSocket;
    private SSLContext mSSLContext;
    private KeyManagerFactory mKeyManagerFactory;
    private SSLServerSocketFactory mSSLServerSocketFactory;

    // http://www.java2s.com/Tutorial/Java/0490__Security/KeyStoreExample.htm
    // http://www.programcreek.com/java-api-examples/index.php?api=javax.net.ssl.KeyManagerFactory
    // http://www.herongyang.com/JDK/SSL-Socket-Server-Example-SslReverseEchoer.html
    public SecureHTTPServer(int serverPort){
        super(serverPort);
        try {
            mKeyStore = KeyStore.getInstance("JKS");
            char[] password = "password1".toCharArray();
            FileInputStream fIn = new FileInputStream("server.jks");
            mKeyStore.load(fIn,password);

            mKeyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            mKeyManagerFactory.init(mKeyStore,password);

            mSSLContext = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(mKeyStore);
            mSSLContext.init(mKeyManagerFactory.getKeyManagers(),tmf.getTrustManagers(),new SecureRandom());
            mSSLServerSocketFactory = mSSLContext.getServerSocketFactory();
            socket = (SSLServerSocket) mSSLServerSocketFactory.createServerSocket(getServerPort());

            printServerSocketInfo(socket);

            System.out.println("SSL SERVER STARTED ON PORT " + getServerPort());

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
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
                    printServerSocketInfo(socket);
                    int waitInterval = 0;
                    while (getLeaveConnectionOpen()) {
                        boolean isEmpty = false;
                        do {
                            if (!getFromClientStream().ready()){
//                                System.out.println("Client Stream not ready");
                                break;
                            }
//                            System.out.println("before get request header");
                            ArrayList<String> requestHeader = getRequestHeader();
                            System.out.println(requestHeader.get(0));

                            if(requestHeader != null && !requestHeader.isEmpty() && ( requestHeader.contains("Connection: close\r\n") || requestHeader.get(0).contains("HTTP/1.0"))) {
//                                System.out.println("In check for close");
                                setLeaveConnectionOpen(false);
                            }

                            if (requestHeader == null || requestHeader.isEmpty()) {
                                System.out.println("Ignoring empty request...");
                                isEmpty = true;
                            } else {
                                String[] requests = requestHeader.get(0).split(" ");
                                processRequest(requests[0], requests[1]);
                                waitInterval = 0;
                            }
                        } while (!isEmpty);

                        if (waitInterval != 0){
                            Thread.sleep(1000);
                            System.out.println("Waited " + waitInterval);
                        }
                        waitInterval++;
                        if(waitInterval == SOCKET_LIVE_TIME){
                            setLeaveConnectionOpen(false);
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

    public void initializeSSL(){

    }

    @Override
    public void bind()  {
        //socket = (mSSLServerSocket);
        System.out.println("HTTP Server bound and listening to port " + getServerPort());
    }

    @Override
    public boolean acceptFromClient() throws IOException {
        try {
           mClientSocket =  ((Socket) socket.accept());
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

    private static void printSocketInfo(SSLSocket s) {
        System.out.println("Socket class: "+s.getClass());
        System.out.println("   Remote address = "
                +s.getInetAddress().toString());
        System.out.println("   Remote port = "+s.getPort());
        System.out.println("   Local socket address = "
                +s.getLocalSocketAddress().toString());
        System.out.println("   Local address = "
                +s.getLocalAddress().toString());
        System.out.println("   Local port = "+s.getLocalPort());
        System.out.println("   Need client authentication = "
                +s.getNeedClientAuth());
        SSLSession ss = s.getSession();
        System.out.println("   Cipher suite = "+ss.getCipherSuite());
        System.out.println("   Protocol = "+ss.getProtocol());
    }
    private static void printServerSocketInfo(SSLServerSocket s) {
        System.out.println("Server socket class: "+s.getClass());
        System.out.println("   Socker address = "
                +s.getInetAddress().toString());
        System.out.println("   Socker port = "
                +s.getLocalPort());
        System.out.println("   Need client authentication = "
                +s.getNeedClientAuth());
        System.out.println("   Want client authentication = "
                +s.getWantClientAuth());
        System.out.println("   Use client mode = "
                +s.getUseClientMode());
    }
}
