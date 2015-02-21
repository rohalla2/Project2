import java.io.IOException;

/**
 * Created by Jeff on 2/20/2015.
 */
public class SecureHTTPServer extends AbstractServer {

    public SecureHTTPServer(){
        super(12321);
    }

    @Override
    void bind() {

    }

    @Override
    boolean acceptFromClient() throws IOException {
        return false;
    }
}
