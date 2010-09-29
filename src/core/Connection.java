
package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private BufferedReader in;
    private PrintWriter out;

    Connection(Socket socket) throws IOException
    {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream());
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    
    void write(String message) {
        out.println(message);
        out.flush();
    }
    
    String read() throws IOException {
        String message = null;
        while (message == null) {
            System.out.println("HERE!");
            message = in.readLine();
        }
        return message;
    }
    
    void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
    
}
