
package core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;
import static core.Message.Type;

/**
 *
 * @author Yann Le Gall
 * ylegall@gmail.com
 */
public class Connection implements Runnable {

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Queue<Message> messages;
    private boolean connected;

    Connection(Socket socket, Queue<Message> messages) throws IOException
    {
        this.socket = socket;
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
        this.messages = messages;
    }

    /**
     * 
     * @param message
     * @throws IOException
     */
    void write(Message message) throws IOException {
        message.timeStamp();
        output.writeObject(message);
        output.flush();
    }
    

    /**
     *
     */
    @Override
    public void run() {

        connected = true;
        Message msg = null;

        // message loop
        do {
            try {
                msg = (Message) input.readObject();
                messages.add(msg);
                if(msg.type == Type.END) {
                    connected = false;
                }

            } catch (IOException ex) {
                break; // close connection
            } catch (ClassNotFoundException ex) {
                // TODO: ignore and continue?
                continue;
            }

        } while (connected);

        // cleanup
        try {
            input.close();
            // TODO: send close message
            output.close();
            socket.close();
        } catch (IOException ioe) {
            // print and ignore
        }

    }

    void disconnect() {
        connected = false;
        try {
            write(new Message(Type.END, ""));
        } catch (IOException ioe) {
        }
    }

}
