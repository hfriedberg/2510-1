package core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Logger;
import static core.Message.Type;
import org.json.simple.JSONArray;

public class Process implements Runnable
{

    private int pID;
    private long clock;
    private Queue<Task> tasks;
    private Connection[] connections;
    private ServerSocket serverSocket;
    private Queue<Message> messages;
    private Queue<Message> requests;
    private boolean hasToken;
    private FileWriter writer;
   	private int prevpID;
    static int nextpID;

    // statistics
    private int numMsgSent, numMsgReceived, numAccess;
//    private List<Integer> waitTimes;
    
    
    private static final Logger logger = Logger.getLogger("core.Main");

    public Process(int ID, JSONArray taskArray) throws IOException {
        logger.info("creating process...");
        pID = ID;
        tasks = new LinkedList<Task>();
	serverSocket = new ServerSocket(pID + Main.PORT);
        messages = new PriorityBlockingQueue<Message>();
        writer = new FileWriter(Main.FILENAME, true);
        System.out.println(taskArray);
        for (int j = 0; j < taskArray.size(); j++) {
            JSONArray jsonTask = (JSONArray) taskArray.get(j);
            tasks.add(new Task(
                    ((Long) jsonTask.get(0)).intValue(),
                    (String) jsonTask.get(1),
                    ((Long) jsonTask.get(2)).intValue()));
        }
        makeConnections();
    }

    private void makeConnections() throws IOException {
        connections = new Connection[Main.numProcesses];
		
		if(Main.algorithm.equals("token")){
			boolean isClient = false;
			boolean isServer = false;
			while(!isClient || !isServer){
				prevpID = this.pID - 1;
				if (prevpID < 0) {
					prevpID = Main.numProcesses - 1;
				}
				
				try {
					connections[prevpID] = new Connection( new Socket(Main.hostNames[prevpID], Main.PORT + prevpID), messages );
					new Thread(connections[prevpID]).start();
					isClient = true;
				} catch (Exception e) {
					// server isn't running yet - it's okay try again later.
				}

				nextpID = (this.pID + 1) % Main.numProcesses;
				
				if(!isServer) {
					connections[nextpID] = new Connection(serverSocket.accept(), messages);
					new Thread(connections[nextpID]).start();
					isServer = true;
				}

			}
		} else if (Main.algorithm.equals("tokenless")){
			//Take out extra logging info when everything is done
			System.out.println("starting connections...");
                        			
			int numConnections = 0;  // the number of client connections
			int numServers = 0;      // the number of server connections
			boolean isServer = false;

		        while (numServers < Main.numProcesses - this.pID - 1 || numConnections < this.pID){
						for(int i=0; i < this.pID; i++){
								if (connections[i] == null){
								
										try {
											System.out.println("Try to start connection to " + i  + " from " + this.pID);
											connections[i] = new Connection( new Socket(Main.hostNames[i], Main.PORT + i), messages);
											new Thread(connections[i]).start();
											numConnections++;
											System.out.println("number of connections for " + pID + " is " + connections);
										} catch (Exception e){
											//ok to try again, but log for debugging purposes.
											logger.info("server not up yet");
										}
								}
						}
						if(numServers < Main.numProcesses - this.pID - 1){
							for(int i=this.pID; i < Main.numProcesses - 1; i++){
								logger.info("starting server " + i);
								connections[this.pID] = new Connection(serverSocket.accept(), messages);
								new Thread(connections[i]).start();
								numServers++;
								logger.info("finished server " + i);
							}
						}
			}
		} else {
			logger.info("unexpected algorithm type.");
		}
		logger.info("made connections");
    }

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
        	try {
        	  tokenlessAlgorithm();	
        	} catch (Exception e) {
        	  //hopefully won't happen...	
        	}
        } else {
			try {
            	tokenAlgorithm();
			} catch (Exception e) {
				// probably won't happen. 
			}
        }

        // close connections:
        logger.info("process finished.");

		if(Main.algorithm.equals("token")) {
			try {
				connections[nextpID].write(new Message(Type.END, this.pID));
				connections[nextpID].disconnect();
				connections[prevpID].disconnect();
			} catch (Exception e) {
				logger.info("Exception during shutdown.");
			}
		} else {
        	multicast(new Message(Type.END, null));
		}

        shutdown();
    }

	private void tokenAlgorithm() throws IOException{
		clock = 1;								// init clock to 1
		int activePeers = Main.numProcesses; 	// init the  number of active peers 
		Message msg;
		boolean hasToken;

		if (pID == 0) {
			hasToken = true;
		} else {
			hasToken = false;
		}

		while (activePeers > 1 || !tasks.isEmpty()) {
			if (hasToken && !tasks.isEmpty()) {
				Task curTask = tasks.peek();
				if (clock >= curTask.startTime) {
					tasks.poll();

					appendToFile(curTask);

					clock += curTask.duration;
					
					try {
						// let's simulate taking some time for this task.
						Thread.sleep(500*curTask.duration);
					} catch (Exception e) {
						// I really don't like exception handling.
					}

					if (tasks.isEmpty()) {
						logger.info("tasks complete");
						connections[nextpID].write(new Message(Type.IDLE, this.pID));
					}
				}
			} 

			if (hasToken) {
				hasToken = false;
				logger.fine("passing token to: " + nextpID + " from: " + this.pID);
				logger.fine("tasks remaining: " + tasks.size() + "\t clock: " + clock);
				numMsgSent++;
				// pass the token
				connections[nextpID].write(new Message(Type.TOKEN, clock));
			} 
			
			// handle messages if have them
			if (!messages.isEmpty()) {
				msg = messages.poll();
				numMsgReceived++;

				if(msg.type == Type.TOKEN) {
					clock = Math.max(clock, msg.timestamp);
				}
				
				switch (msg.type) {
					case TOKEN:
						logger.fine("token obtained");
						hasToken = true;
						break;
					case IDLE:
						logger.fine("idle message received");
						
						// timestamp in this case is actually the pID of the now idle process
						if(msg.timestamp != this.pID){
							activePeers--;
							connections[nextpID].write(new Message(Type.IDLE, msg.timestamp));
						}
						break;
					case END:
						logger.fine("end message received");
						if(tasks.isEmpty()){
							connections[nextpID].write(new Message(Type.END, this.pID));
							return;
						}
						break;
					default:
						logger.fine("unexpected message received");
				}
			}
		}
	}

	private void tokenlessAlgorithm() throws IOException {
	//takek out extra logging stuff when it's done
		clock = 1;
		int activePeers = Main.numProcesses - 1;
	    Message msg;
	    int replies = 0;
	    boolean sentRequest = false;
	    Task curTask = null;
	    Message[] requestTable = new Message[Main.numProcesses];
      
	    System.out.println(tasks);
	    while(!tasks.isEmpty()){
	    	logger.info("working...");
	    	if(!sentRequest){
	    		curTask = tasks.peek();
	    		if (clock >= curTask.startTime) {
	    			msg = new Message(Type.CS_REQUEST, this.pID, clock);
	    			sentRequest = true;
	    			multicast(msg);
	    			requests.offer(msg);
	    		}
	    	}
			
	    	if (!messages.isEmpty()) {
	    		msg = messages.poll();
	    		numMsgReceived++;

				switch (msg.type) {
					case CS_REQUEST:
						clock = Math.max(clock, msg.timestamp);
						requestTable[msg.sender] = msg;
						requests.offer(msg);
						connections[msg.sender].write(new Message(Type.REPLY, clock));
						break;
					case REPLY:
					    if (sentRequest) {
					      replies++;
					    }
					    break;
					case CS_RELEASE :
						requests.remove(requestTable[msg.sender]);
						break;
					case IDLE:
						logger.fine("idle message received");
						// timestamp in this case is actually the pID of the now idle process
						if(msg.timestamp != this.pID){
						  activePeers--;
						}
						break;
					case END:
						logger.fine("end message received");
						if(tasks.isEmpty()){
							return;
						}
						break;
					default:
						logger.fine("unexpected message received");
				}
		  }
		  
		  Message earliestRequest = requests.peek();
		  if(replies == activePeers && earliestRequest.sender == this.pID){
			  tasks.poll();
			  appendToFile(curTask);
			  multicast(new Message(Type.CS_RELEASE, this.pID));
		  }
    	  
		  if (tasks.isEmpty()) {
			  logger.info("tasks complete");
			  multicast(new Message(Type.IDLE, this.pID));
		  }  
      }
	}

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

    private void multicast(Message message) {
        for (int i = 0; i < connections.length; i++) {
            if (i != this.pID) {
                try {
                	System.out.println(i);
                	System.out.println(message);
                    connections[i].write(message);
                } catch (IOException ioe) {
                    logger.warning(ioe.getMessage());
                }
            }
        }
    }

    private void shutdown() {
        try {
            writer.close();
            serverSocket.close();
        } catch (IOException ioe) {
            logger.warning(ioe.getMessage());
        }
    }
}
