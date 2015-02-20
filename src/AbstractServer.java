import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

abstract class AbstractServer extends Thread {
    private static final String[] URLS_404 = {"/redirect.defs"};
    private final int serverPort;
    private ServerSocket socket;
    private Socket mClientSocket;
    private DataOutputStream toClientStream;
    private BufferedReader fromClientStream;
    private HashMap<String,String> mRedirects;

    public AbstractServer(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Creates a socket + binds to the desired server-side port #.
     *
     * @throws {@link IOException} if the port is already in use.
     */
    public void bind()  {
        try{
            socket = new ServerSocket(serverPort);
            System.out.println("Server bound and listening to port " + serverPort);
        } catch (IOException e) {
            System.out.println("Error binding to port " + serverPort);
        }
    }

    /**
     * Waits for a client to connect, and then sets up stream objects for communication
     * in both directions.
     *
     * @return {@code true} if the connection is successfully established.
     * @throws {@link IOException} if the server fails to accept the connection.
     */
    public boolean acceptFromClient() throws IOException {
        try {
            mClientSocket = socket.accept();
        } catch (SecurityException e) {
            System.out.println("The security manager intervened; your config is very wrong. " + e);
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println("Probably an invalid port number. " + e);
            return false;
        }

        toClientStream = new DataOutputStream(mClientSocket.getOutputStream());
        fromClientStream = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));
        return true;
    }

    public ArrayList<String> getRequestHeader () throws IOException {
        ArrayList<String> strHeader = new ArrayList<String>();
        String strLine = null;
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