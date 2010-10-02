
package core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static core.Message.Type;
import org.json.simple.JSONArray;

/**
 * 
 * 
 * 
 */
public class Process implements Runnable {
    
    private int pID;
    private Deque<Task> tasks;
    private Connection[] connections;
    private ServerSocket serverSocket;
    private Queue<Message> messages;
    private boolean hasToken;
    private FileWriter fileWriter;
    private static final Logger logger = Logger.getLogger("core.Main");
    
    public Process(int ID, JSONArray taskArray) throws IOException {
        
        pID = ID;
        tasks = new ArrayDeque<Task>();
        serverSocket = new ServerSocket(pID + Main.PORT);
        messages = new PriorityBlockingQueue<Message>();
        
        for (int j=0; j < taskArray.size(); j++) {
            JSONArray jsonTask = (JSONArray)taskArray.get(j);
            tasks.add(new Task(
                    ((Long)jsonTask.get(0)).intValue(),
                    (String)jsonTask.get(1),
                    ((Long)jsonTask.get(2)).intValue())
                    );
        }
        
        makeConnections();
    }
    
    /**
     * 
     * @throws IOException
     */
    private void makeConnections() throws IOException {
        connections = new Connection[Main.numProcesses];

        // act as a client for to all smaller pIDs:
        for (int i = 0; i < pID; i++) {
            connect_loop:
            while (true) {
                try {
                    if (i != pID) {
                        connections[i] = new Connection(
                                new Socket( Main.hostNames[i], Main.PORT + i),
                                messages);
                        new Thread(connections[i]).start();
                    }
                } catch (IOException ioe) {
                    logger.info(String.format("waiting for peer %d", i));
                    continue connect_loop;
                }
                break connect_loop;
            }
        }

        // act as a server for all larger pIDs:
        for (int i = pID + 1; i < Main.numProcesses; i++) {
            logger.info(String.format("waiting for peer %d", i));
            connections[i] = new Connection(serverSocket.accept(), messages);
            new Thread(connections[i]).start();
        }
        logger.info("all peers connected");
        
    }

    /**
     * 
     */
    @Override
    public void run() {
        
        // create the shared file
        File sharedFile = new File(Main.FILENAME);
        try {
            sharedFile.createNewFile();
            fileWriter = new FileWriter(sharedFile);
        } catch (IOException ioe) {
            logger.warning(ioe.getMessage());
        }
        
        // run an algorithm
        logger.info(String.format("starting %s algorithm ", Main.algorithm));
        if (Main.algorithm.equals("tokenless")) {
            // TODO: implement
        } else {
            tokenRing();
        }
        
        // close connections:
        logger.info("process finished.");
        for (Connection c : connections) {
            try {
                c.write(new Message(Type.END, null));
            } catch (IOException ioe) {
                logger.warning(ioe.getMessage());
            }
        }
        shutdown();
    }
    
    /**
     * 
     */
    private void tokenRing() {
        Task task = null;
        Message msg = null;
        if (pID == 0) {
            hasToken = true;
            msg = new Message(Type.TOKEN, null);
        }
        
        while (!tasks.isEmpty()) {
            task = tasks.poll();
            while (!hasToken) {
                msg = messages.poll();
                if (msg != null) {
                    hasToken = (msg.type == Type.TOKEN);
                }
            }
            appendToFile(task);
            
            // pas the token to the next process
            hasToken = false;
            try {
                connections[(pID + 1) % Main.numProcesses].write(msg);
            } catch (IOException ioe) {
                logger.warning(ioe.getMessage());
            }
        }
    }
    
    /**
     * 
     * @param task
     */
    private void appendToFile(Task task) {
        try {
            logger.fine(String.format("writing to file, task = %s", task));
            fileWriter.write(String.format("%d, %d, %d, %s\n",
                    task.startTime,
                    task.startTime + task.duration,
                    this.pID,
                    (task.action == Task.READ) ? "read" : "write"));
        } catch (IOException ioe) {
            logger.warning(ioe.getMessage());
        }
    }

    /**
     * 
     */
    private void shutdown() {
        try {
            fileWriter.close();
            serverSocket.close();
        } catch (IOException ioe) {
            logger.info(ioe.getMessage());
        }
    }

}
