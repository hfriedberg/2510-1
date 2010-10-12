
package core;

import java.util.logging.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;
import static core.Message.Type;

// connection class for handling socket connections to other processes 
public class Connection implements Runnable {

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Queue<Message> messages;
    private boolean connected;
    private static final Logger logger = Logger.getLogger("core.Main");

    Connection(Socket socket, Queue<Message> messages) throws IOException
    {
        this.socket = socket;
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
        this.messages = messages;
    }

	void write(Message message) throws IOException {
        output.writeObject(message);
        output.flush();
    }
    

    public void run() {

        connected = true;
        Message msg = null;

        // message loop
        do {
            try {
                
                // get a message and add it to the queue
                msg = (Message) input.readObject();
                messages.add(msg);

                if(msg.type == Type.END) {	
                    connected = false;
                }

            } catch (IOException ex) {
                logger.warning(ex.getMessage());
                break;
			} catch (ClassNotFoundException ex) {
                logger.severe(ex.getMessage());
                continue;
            }

        } while (connected);

        // cleanup
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
            input.close();
            output.close();
        } catch (IOException ioe) {
            logger.warning(ioe.getMessage());
        }

    }

    void disconnect() {
        connected = false;
        try {
            write(new Message(Type.END, ""));
        } catch (IOException ioe) {
            logger.info(ioe.getMessage());
        }
    }

}
