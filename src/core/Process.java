package core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
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
    private List<Long> waitTimes = new ArrayList<Long>();
    
    
    private static final Logger logger = Logger.getLogger("core.Main");

    public Process(int ID, JSONArray taskArray) throws IOException {
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
                        			
			int numConnections = 0;  // the number of client connections
			int numServers = 0;      // the number of server connections

		        while (numServers < Main.numProcesses - this.pID - 1 || numConnections < this.pID){
						for(int i=0; i < this.pID; i++){
								if (connections[i] == null){
								
										try {
											connections[i] = new Connection( new Socket(Main.hostNames[i], Main.PORT + i), messages);
											  new Thread(connections[i]).start();
											numConnections++;
										} catch (Exception e){
											//ok to try again, but log for debugging purposes.
											//logger.info("server not ready yet");
										}
								}
						}
						if(numServers < Main.numProcesses - this.pID - 1){
							for(int i=this.pID+1; i < Main.numProcesses; i++){
								connections[i] = new Connection(serverSocket.accept(), messages);
								new Thread(connections[i]).start();
								numServers++;
							}
						}
			}
		} else {
			logger.info("unexpected algorithm type.");
		}
    }

    public void run(){

        // create the shared file
        File sharedFile = new File(Main.FILENAME);
        try {
            sharedFile.createNewFile();
            //fileWriter = new FileWriter(sharedFile);
        } catch (IOException ioe) {
            logger.warning(ioe.getMessage());
        }

	long totalStart = System.currentTimeMillis();

        // run an algorithm
        logger.info(String.format("starting %s algorithm\n", Main.algorithm));
        if (Main.algorithm.equals("tokenless")) {
        	try {
        	  tokenlessAlgorithm();	
        	} catch (Exception e) {
		  logger.info(e.toString());
		  e.printStackTrace();
        	  //hopefully won't happen...	
        	}
        } else {
			try {
            	tokenAlgorithm();
			} catch (Exception e) {
				// probably won't happen. 
			}
        }

	long totalEnd = System.currentTimeMillis();

	long totalWait = 0;
	int numWaits = 0;
	//Log statistics in the output file
	for(int i=0; i<waitTimes.size(); i++){
		numWaits++;
		totalWait += waitTimes.get(i);
	}
		
	int statId = this.pID + 1;
	try{
		appendToFile("\nNumber of messages sent for " + statId + " = " + numMsgSent);
		appendToFile("\nNumber of messages received for " + statId + " = " + numMsgReceived);
		appendToFile("\nAverage wait time for " + statId + " = " + (float)totalWait/numWaits + " milliseconds.");
		appendToFile("\nTotal time running for " + statId + " = " + ((float) (totalEnd - totalStart)) + " milliseconds.");
	}catch(Exception e){
		//There just won't be statistics in the output file...
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
			logger.info("SHUTTING DOWN PROCESS " + this.pID);
			for(int i = 0; i < Main.numProcesses; i++){
				if(i!=pID && connections[i]!=null){
			 		try {
						connections[i].write(new Message(Type.END, this.pID));
						connections[i].disconnect();
					}catch (Exception e) {
						logger.info("Exception during shutdown.");
						continue;	
					}
				}
			}
		}

        shutdown();
    }

	private void tokenAlgorithm() throws IOException{
		clock = 1;								// init clock to 1
		int activePeers = Main.numProcesses; 	// init the  number of active peers 
		Message msg;
		boolean hasToken;
		long startTime = 0;

		if (pID == 0) {
			hasToken = true;
		} else {
			hasToken = false;
		}

		while (activePeers > 1 || !tasks.isEmpty()) {
			if (hasToken && !tasks.isEmpty()) {
				Task curTask = tasks.peek();
				if (clock >= curTask.startTime) {
					if(startTime!=0){
						waitTimes.add(System.currentTimeMillis() - startTime);
					}else{
					 	waitTimes.add(new Long(0));
					}
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
					}else{
						startTime = System.currentTimeMillis();
					}
				}
			}else if(!tasks.isEmpty()){
			  if(startTime==0)
			    startTime = System.currentTimeMillis();
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
	    clock = 1;
	    int activePeers = Main.numProcesses;
	    Message msg;
	    int replies = 0;
	    boolean sentRequest = false;
	    Task curTask = null;
	    Message[] requestTable = new Message[Main.numProcesses];
            requests = new PriorityBlockingQueue<Message>();
            long startTime = 0;

	    while(activePeers > 1 || !tasks.isEmpty()){
	    	while (!messages.isEmpty()){
	    		msg = messages.poll();
			//logger.info(this.pID + " received this message: " + msg);
	    		numMsgReceived++;
			clock = Math.max(clock, msg.timestamp);
			switch (msg.type){
				case CS_REQUEST:
					//logger.info(this.pID + " received a request from " + msg.sender() + " at time " + msg.timestamp);
					if(requestTable[msg.sender()] != null){
					  requests.remove(requestTable[msg.sender()]);
					}
					requestTable[msg.sender()] = msg;
					requests.offer(msg);
					Message sendMessage = new Message(Type.REPLY, msg.sender(), clock);
					multicast(sendMessage);
					break;
				case REPLY:
					//logger.info(this.pID + " received a reply from " + msg.sender());
					if (sentRequest && msg.sender()==this.pID) {
						replies++;
						//logger.info(pID + " has now received " + replies + " replies");
					}
					break;
				case CS_RELEASE:
					//logger.info(this.pID + " received a release from " + msg.sender());
					requests.remove(requestTable[msg.sender()]);
					requestTable[msg.sender()] = null;
					break;
				case IDLE:
					//logger.fine(this.pID + " received an idle message from " + msg.sender());
					if(msg.sender() != this.pID){
						activePeers--;
					}
					break;
				case END:
					if(tasks.isEmpty()){
						return;
					}
					break;
				default:
					logger.fine("unexpected message received");
			}
		  }

	    	  curTask = tasks.peek();
		  if(curTask != null && !sentRequest){
	    		if (clock >= curTask.startTime) {
	    			msg = new Message(Type.CS_REQUEST, this.pID, clock);
				startTime = System.currentTimeMillis();
	    			multicast(msg);
				sentRequest = true;
	    			requests.offer(msg);
	    		}
	    	 }
				  
		  if(!requests.isEmpty()){
			  Message earliestRequest = requests.peek();
			  if(replies == Main.numProcesses - 1 && earliestRequest.sender() == this.pID){
     				  waitTimes.add(System.currentTimeMillis() - startTime);
				  startTime = 0;
				  requests.poll();
				  curTask = tasks.poll();
				  appendToFile(curTask);
				  clock += curTask.duration;

				  try {
					// let's simulate taking some time for this task.
					Thread.sleep(500*curTask.duration);
				  } catch (Exception e) {
					// I really don't like exception handling.
				  }

				  sentRequest = false;
				  replies = 0;
				  multicast(new Message(Type.CS_RELEASE, this.pID, clock));

				  if (tasks.isEmpty()) {
			  		logger.info("tasks complete");
			  		multicast(new Message(Type.IDLE, this.pID, clock));
		  		  }  
			  }else if(replies == Main.numProcesses - 1){
			//	logger.info(this.pID + " has these requests: " + requests);
			  }
		  }
	    }
    	}

    private void appendToFile(Task task) throws IOException {
        try {
            // write to the file
            logger.fine(String.format("performing task, %s", task));
            writer.write(String.format("%d, %d, %d, %s\n",
                    clock,
                    clock + task.duration,
                    this.pID + 1,
                    (task.action == Task.READ) ? "read" : "write"));
            writer.flush();
        } finally {
//            writer.close();
     	}
    }

    private void appendToFile(String s) throws IOException {
        try {
            // write to the file
            logger.fine("Writing statistic to file...");
            writer.write(s);
            writer.flush();
        } finally {
//            writer.close();
     	}
    }

    private void multicast(Message message) {
        for (int i = 0; i < connections.length; i++) {
            if (i != this.pID && connections[i]!= null) {
                try {
                    	connections[i].write(message);
			numMsgSent++;
                } catch (IOException ioe) {
                    logger.warning("Could not send message:" + ioe.getMessage());
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
