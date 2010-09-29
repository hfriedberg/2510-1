
package core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import org.json.simple.JSONArray;

/**
 *  @author Yann Le Gall
 *  ylegall@gmail.com
 *  Sep 26, 2010 7:59:58 PM
 */
public class Process implements Runnable {
    
    private int pID;
    private Deque<Task> tasks;
    private Connection[] connections;
    private ServerSocket serverSocket;
    
    public Process(int ID, JSONArray taskArray) throws IOException {
        
        pID = ID;
        tasks = new ArrayDeque<Task>();
        serverSocket = new ServerSocket(pID + Main.PORT);
        
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
    private void makeConnections() throws IOException
    {
        connections = new Connection[Main.numProcesses];
        
        // listen for other connections
        int peer = 0;
        while (pID > peer) {
            connections[peer] = new Connection(serverSocket.accept());
            peer++;
        }
                System.out.println("here1");
        // connect to other processes
        for (int i = 0; i < Main.numProcesses; i++) {
            if (i != pID) {
                connections[i] = new Connection( new Socket(
                        Main.HOST,
                        Main.PORT + i ));
            }
        }
        peer++;
                System.out.println("here2");
        while (peer < Main.numProcesses) {
            connections[peer] = new Connection(serverSocket.accept());
            peer++;
        }
                        System.out.println("here3");
    }

    @Override
    public void run() {

        try {
            if (pID == 0) {
                for (int i = 1; i < connections.length; i++) {
                    connections[i].write(String.format("Hello %d, from process 0", i));
                    connections[i].write("END");
                }
            } else {
                System.out.printf("received message: %s\n", connections[0].read());
                System.out.printf("received another message: %s\n", connections[0].read());
            }
            serverSocket.close();
            
            for (int i=0; i < connections.length; i++) {
                if (i != pID) {
                    connections[i].close();
                }
            }
            
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        
    }

}
