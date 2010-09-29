
package core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import org.json.simple.JSONArray;
import static core.Message.Type;

/**
 *  @author Yann Le Gall
 *  ylegall@gmail.com
 */
public class Process implements Runnable {
    
    private int pID;
    private Deque<Task> tasks;
    private Connection[] connections;
    private ServerSocket serverSocket;
    private Queue<Message> messages;
    
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
                                new Socket(
                                Main.HOST,
                                Main.PORT + i),
                                messages);
                        new Thread(connections[i]).start();
                    }
                } catch (IOException ioe) {
                    System.out.printf("\rwaiting for peer %d", i);
                    continue connect_loop;
                }
                break connect_loop;
            }
        }
        System.out.println();

        // act as a server for all larger pIDs:
        for (int i = pID + 1; i < Main.numProcesses; i++) {
            connections[i] = new Connection(serverSocket.accept(), messages);
            new Thread(connections[i]).start();
            System.out.printf("connected to peer %d\n", i);
        }
        
        // ============================================

        
//        // listen for other connections
//        int peer = 0;
//        while (pID > peer) {
//            connections[peer] = new Connection(serverSocket.accept(), messages);
//            new Thread(connections[peer]).start();
//            System.out.printf("connected to peer %d\n", peer);
//            peer++;
//        }
//
//        // connect to other processes
//        for (int i = 0; i < Main.numProcesses; i++) {
//            connect_loop:
//            while (true) {
//                try {
//                    if (i != pID) {
//                        connections[i] = new Connection(
//                                new Socket(
//                                Main.HOST,
//                                Main.PORT + i),
//                                messages);
//                    }
//                } catch (IOException ioe) {
//                    System.out.printf("\rwaiting for peer %d", i);
//                    continue connect_loop;
//                }
//                break connect_loop;
//            }
//        }
//        System.out.println();
//        peer++;
//
//        while (peer < Main.numProcesses) {
//            connections[peer] = new Connection(serverSocket.accept(), messages);
//            System.out.printf("connected to peer %d\n", peer);
//            peer++;
//        }
    }

    @Override
    public void run() {
        try {
            if (pID == 0) {
                for (int i = 1; i < connections.length; i++) {
                    connections[i].write(new Message(Type.INFO, "hello world from 0"));
                    connections[i].disconnect();
                }
            } else {
                Message msg = null;
                do {

                    do {} while (messages.isEmpty());
                    msg = messages.poll();
                    
                    System.out.println(msg);
                } while (msg.type != Type.END);
            }

            shutdown();

        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    /**
     * 
     */
    private void shutdown() {
        try {
            serverSocket.close();
        } catch (IOException ioe) {
        }
    }

}
