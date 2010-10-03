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
import java.util.logging.Logger;
import static core.Message.Type;
import org.json.simple.JSONArray;

/**
 * 
 * 
 * 
 */
public class Process implements Runnable
{

    private int pID;
    private long clock;
    private Deque<Task> tasks;
    private Connection[] connections;
    private ServerSocket serverSocket;
    private Queue<Message> messages;
    private boolean hasToken;
    private FileWriter writer;
    
    // statistics
//    private int numMsgSent, numMsgReceived, numAccess;
//    private List<Integer> waitTimes;
    
    
    private static final Logger logger = Logger.getLogger("core.Main");

    public Process(int ID, JSONArray taskArray) throws IOException {

        pID = ID;
        tasks = new ArrayDeque<Task>();
        serverSocket = new ServerSocket(pID + Main.PORT);
        messages = new PriorityBlockingQueue<Message>();
        writer = new FileWriter(Main.FILENAME, true);

        for (int j = 0; j < taskArray.size(); j++) {
            JSONArray jsonTask = (JSONArray) taskArray.get(j);
            tasks.add(new Task(
                    ((Long) jsonTask.get(0)).intValue(),
                    (String) jsonTask.get(1),
                    ((Long) jsonTask.get(2)).intValue()));
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
                                new Socket(Main.hostNames[i], Main.PORT + i),
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
        logger.info("all peers connected\n");

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
            //fileWriter = new FileWriter(sharedFile);
        } catch (IOException ioe) {
            logger.warning(ioe.getMessage());
        }

        // run an algorithm
        logger.info(String.format("starting %s algorithm\n", Main.algorithm));
        if (Main.algorithm.equals("tokenless")) {
            // TODO: implement
        } else {
            tokenRing();
        }

        // close connections:
        logger.info("process finished.");
        multicast(new Message(Type.END, null));

        shutdown();
    }

    /**
     * 
     */
    private void tokenRing() {

        clock = 1;
        Message msg = null;
        int activePeers = Main.numProcesses;

        // process 0 starts with the token
        if (pID == 0) {
            hasToken = true;
            msg = new Message(Type.TOKEN, null);
        }

        // maintain the ring wile peers are still active
        while (activePeers > 1 || !tasks.isEmpty()) {
            
            //clock++;

            // if we have the token and a task,
            // perform the task and pass the token
            if (hasToken) {
                if (!tasks.isEmpty()) {
                    
                    // dequeue the next task and write to the file
                    // TODO: the tasks are assumed to be given in order
                    Task task = tasks.peek();
                    if (this.clock >= task.startTime) {
                        tasks.poll();
                        try {
                            
                            // write to the shared file:
                            appendToFile(task);
                            
                            // simulate work during duration of task:
                            clock += task.duration;
                            
                        } catch (IOException ioe) {
                            logger.warning(ioe.getMessage());
                        }

                        // if we are done with all tasks
                        // then notify other peers
                        if (tasks.isEmpty()) {
                            logger.info("tasks complete");
                            multicast(new Message(Type.IDLE, clock));
                        }
                    }
                        
                }

                // pass the token to the next process
                hasToken = false;
                try {
                    int next = (pID + 1) % Main.numProcesses;
                    connections[next].write(new Message(Type.TOKEN, clock));
                } catch (IOException ioe) {
                    logger.warning(ioe.getMessage());
                }
            }

            // get the next message
            if (!messages.isEmpty()) {
                msg = messages.poll();
                clock = Math.max(clock, msg.timestamp);
                switch (msg.type) {
                    case TOKEN:
                        logger.fine("token obtained");
                        hasToken = true;
                        break;
                    case IDLE:
                        logger.fine("received idle message");
                        activePeers--;
                        break;
                }
            }
        }
    }

    /**
     * 
     * @param task
     */
    private void appendToFile(Task task) throws IOException {
//        FileWriter writer = null;
        try {
            
            // write to the file
//            writer = new FileWriter(Main.FILENAME, true);
            logger.fine(String.format("performing task, %s", task));
            writer.write(String.format("%d, %d, %d, %s\n",
//                    task.startTime,
//                    task.startTime + task.duration,
                    clock,
                    clock + task.duration,
                    this.pID + 1,
                    (task.action == Task.READ) ? "read" : "write"));
            writer.flush();
        } finally {
//            writer.close();
        }
    }

    /**
     * 
     * @param message
     */
    private void multicast(Message message) {
        for (int i = 0; i < connections.length; i++) {
            if (i != this.pID) {
                try {
                    connections[i].write(message);
                } catch (IOException ioe) {
                    logger.warning(ioe.getMessage());
                }
            }
        }
    }

    /**
     * 
     */
    private void shutdown() {
        try {
            writer.close();
            serverSocket.close();
        } catch (IOException ioe) {
            logger.warning(ioe.getMessage());
        }
    }
}
