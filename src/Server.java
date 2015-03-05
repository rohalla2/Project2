import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server extends Thread {
    private static final String[] URLS_404 = {"/redirect.defs"};
    private static final int MAX_RETRY = 10;
    private static final int SERVER_TIMEOUT_MILLISECONDS = 10000;
    private final int serverPort;
    private Boolean mIsSecureServer;
    private String mServerTypeName;
    private ServerSocket socket;
    private SSLServerSocketFactory mSSLServerSocketFactory;
    private DataOutputStream toClientStream;
    private Socket mClientSocket;
    private Boolean leaveConnectionOpen;
    private BufferedReader fromClientStream;
    private HashMap<String,String> mRedirects;


    public Server(int serverPort) {
        this.serverPort = serverPort;
        mServerTypeName = "HTTP";
        mIsSecureServer = false;
    }

    public Server(int serverPort, boolean isSecureServer){
        this.serverPort = serverPort;
        this.mIsSecureServer = isSecureServer;
        mServerTypeName = "SSL";
        initializeSSL();
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

        Server server = new Server(serverPort);
        Server sslServer = new Server(sslServerPort, true);
        server.start();
        sslServer.start();
    }

    public int getServerPort() {
        return serverPort;
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
                    System.out.println("----- NEW " + mServerTypeName + " CLIENT CONNECTION ESTABLISHED -----");
                    for (int i = 0; i <= MAX_RETRY; i++) {
                        ArrayList<String> requestHeader = getRequestHeader();
                        if (requestHeader == null || requestHeader.isEmpty()) {
                            setLeaveConnectionOpen(false);
                        } else {
                            String[] requests = requestHeader.get(0).split(" ");
                            if ( requestHeader.contains("Connection: close") || requestHeader.get(0).contains("HTTP/1.0")) {
                                setLeaveConnectionOpen(false);
                            }
                            processRequest(requests[0], requests[1]);
                            if (!getLeaveConnectionOpen()) {
                                break;
                            }
                        }
                    }
                    System.out.println(" ----- ENDED " + mServerTypeName + " -----");
                } else {
                    System.out.println("Error accepting client connection.");
                }
            }catch(SocketException e){
                System.out.println("Client has not connected in " + SERVER_TIMEOUT_MILLISECONDS/1000 + " seconds. Closing Socket.");
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

    public Boolean getLeaveConnectionOpen() {
        return leaveConnectionOpen;
    }

    public void setLeaveConnectionOpen(Boolean leaveConnectionOpen) {
        this.leaveConnectionOpen = leaveConnectionOpen;
    }

    public void setFromClientStream(BufferedReader br){
        fromClientStream = br;
    }

    public void setToClientStream(DataOutputStream clientStream){
        toClientStream = clientStream;
    }

    public void bind()  {
        try {
            if (mIsSecureServer){
                socket = mSSLServerSocketFactory.createServerSocket(getServerPort());
            } else {
                socket = new ServerSocket(getServerPort());
            }
            System.out.println(mServerTypeName +  " Server bound and listening to port " + getServerPort());
        } catch (IOException e) {
            System.out.println("Problem Binding to client on " + mServerTypeName + " port " + getServerPort());
            e.printStackTrace();
        }
    }

    public boolean acceptFromClient() throws IOException {
        try {
            mClientSocket =  socket.accept();
            mClientSocket.setSoTimeout(SERVER_TIMEOUT_MILLISECONDS);
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

    public ArrayList<String> getRequestHeader () throws IOException {
        ArrayList<String> strHeader = new ArrayList<String>();
        String strLine;
        while (true) {
            strLine = fromClientStream.readLine();
            if (strLine == null) {
                break;
            } else if (strLine.isEmpty()) {
                break;
            } else {
                strHeader.add(strLine);
            }
        }

        return strHeader;
    }

    public void processRequest(String httpVerb, String resourcePath){
        System.out.println("Verb: " + httpVerb + " Resource: " + resourcePath);
        File resource = new File("www" + resourcePath);

        if (resource.isDirectory()){
            String header = buildHeader(404, "Not Found", null);
            sendResponse(header, null);
            return;
        }

        // if the requested file exists
        if (resource.exists() && !is404(resourcePath)) {
            if (httpVerb.equals("GET")) {
                this.get(resource);
            } else if (httpVerb.equals("HEAD")) {
                this.head();
            } else {
                String header = buildHeader(403, "Forbidden", null);
                sendResponse(header, null);
            }
        } else if (hasRedirect(resourcePath)) {  //if the file exists in the redirects
            HashMap<String,String> headerParams = new HashMap<String, String>();
            headerParams.put("Location", getRedirect(resourcePath));
            String header = buildHeader(301,"Moved Permanently", headerParams);
            sendResponse(header, null);
        } else { // no file or redirect
            String header = buildHeader(404, "Not Found", null);
            sendResponse(header, null);
        }

    }

    public boolean hasRedirect(String resourcePath){
        if(mRedirects.containsKey(resourcePath)){
            return true;
        } else {
            return false;
        }
    }

    public void loadRedirects(){
        mRedirects = new HashMap<String, String>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader("www/redirect.defs"));
            String line;
            while((line = reader.readLine()) != null){
                String[] parts = line.split(" ");
                mRedirects.put(parts[0], parts[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRedirect(String resourcePath){
        return mRedirects.get(resourcePath);
    }

    public String buildHeader(int status, String phrase, HashMap content){
        String strHeader = "HTTP/1.1 " + status + " " + phrase + "\r\n";
        strHeader += "Date: " + getServerDate() + "\r\n";
        if (getLeaveConnectionOpen()) {
            strHeader += "Connection: keep-alive\r\n";
        } else {
            strHeader += "Connection: close\r\n";
        }
        // iterate hashmap
        if (content != null) {
            Set set = content.entrySet();
            Iterator i = set.iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                strHeader += me.getKey() + ": " + me.getValue() + "\r\n";
            }
        }
        strHeader += "\r\n";

        return strHeader;
    }

    // http://stackoverflow.com/questions/7707555/getting-date-in-http-format-in-java
    public String getServerDate(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    public void sendResponse(String header, File file){

        try {
            toClientStream.writeBytes(header);
            if (file != null) {
                byte[] buffer = new byte[1000];
                FileInputStream in = new FileInputStream(file);
                while (in.available() > 0) {
                    toClientStream.write(buffer, 0, in.read(buffer));
                }
                toClientStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void get(File resource){
        String contentType = getContentType(resource.getName());

        HashMap<String, String> content = new HashMap<String,String>();
        content.put("Content-Type", contentType);
        content.put("Content-Length", String.valueOf(resource.length()));
        String header = buildHeader(200, "OK", content);

        sendResponse(header, resource);
    }

    public boolean is404(String resourcePath){
        for (int i = 0; i < URLS_404.length; i++){
            if (resourcePath.equals(URLS_404[i])){
                return true;
            }
        }

        return false;
    }

    // Figure out what MIME type to return
    public String getContentType(String filePath){
        if(filePath.contains(".html")){
            return "text/html";
        }
        else if (filePath.contains(".txt")){
            return "text/plain";
        }
        else if (filePath.contains(".pdf")){
            return "application/pdf";
        }
        else if (filePath.contains(".png")) {
            return "image/png";
        }
        else if (filePath.contains(".jpeg") || filePath.contains(".jpg")){
            return "image/jpeg";
        }
        else if (filePath.contains(".gif")){
            return "image/gif";
        }
        else {
            return "text/plain";
        }
    }

    public void head(){
        String header = buildHeader(200, "OK", null);
        sendResponse(header, null);
    }

    public void serverCleanup(){
        try {
            fromClientStream.close();
            toClientStream.close();
            mClientSocket.close();
        } catch (IOException e){
            System.out.println(e);
        }
    }

}