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
    private ServerSocket socket;
    private Socket mClientSocket;
    private SSLServerSocketFactory mSSLServerSocketFactory;

    public SecureHTTPServer(int serverPort){
        super(serverPort);
        initializeSSL();
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
                    System.out.println("----- NEW CLIENT CONNECTION ESTABLISHED -----");
                            ArrayList<String> requestHeader = getRequestHeader();

                            if (requestHeader == null || requestHeader.isEmpty()) {
                                System.out.println("Ignoring empty request...");
                            } else {
                                String[] requests = requestHeader.get(0).split(" ");
                                processRequest(requests[0], requests[1]);
                            }
                    System.out.println(" ----- ENDED -----");
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

    // http://www.java2s.com/Tutorial/Java/0490__Security/KeyStoreExample.htm
    // http://www.programcreek.com/java-api-examples/index.php?api=javax.net.ssl.KeyManagerFactory
    // http://www.herongyang.com/JDK/SSL-Socket-Server-Example-SslReverseEchoer.html
    public void initializeSSL(){
        try {
            SSLContext mSSLContext = SSLContext.getInstance("TLS");
            char[] password = "password1".toCharArray();
            KeyStore mKeyStore = KeyStore.getInstance("JKS");
            FileInputStream fIn = new FileInputStream("server.jks");
            mKeyStore.load(fIn,password);
            KeyManagerFactory mKeyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            mKeyManagerFactory.init(mKeyStore,password);
            mSSLContext.init(mKeyManagerFactory.getKeyManagers(),null,null);
            mSSLServerSocketFactory = mSSLContext.getServerSocketFactory();
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
    public void bind()  {
        try {
            socket = mSSLServerSocketFactory.createServerSocket(getServerPort());
            System.out.println("HTTPS Server bound and listening to port " + getServerPort());
        } catch (IOException e) {
            System.out.println("Problem Binding to client on SSL port " + getServerPort());
            e.printStackTrace();
        }
    }

    @Override
    public boolean acceptFromClient() throws IOException {
        try {
           mClientSocket =  (socket.accept());
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