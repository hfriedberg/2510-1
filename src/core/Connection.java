
package core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author Yann Le Gall
 * ylegall@gmail.com
 * @date Sep 28, 2010
 */
public class Connection {

    private Socket socket;
    private BufferedInputStream in;
    private PrintWriter out;

    Connection(Socket socket) throws IOException
    {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream());
        in = new BufferedInputStream(socket.getInputStream());
    }

    
}
