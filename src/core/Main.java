
package core;

import java.io.File;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Formatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


/**
 *  @author Yann Le Gall
 *  ylegall@gmail.com
 * 
 */
public class Main {

    static final String FILENAME = "out.txt";
    static String algorithm;
    static int numProcesses;
    public static final int PORT = 3333;
    static final Logger logger = Logger.getLogger("Main");
    public static String[] hostNames;
    static int processID;
    
    private Main() {}

    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
        logger.setLevel(Level.ALL);
        configureLogging();
        
        JSONArray tasks = parseInput();
        System.exit(0);
        new Process(processID, tasks).run();
    }

    /**
     * 
     * @param pid
     * @return
     * @throws Exception
     */
    private static JSONArray parseInput() throws Exception {
        FileReader fr = null;
        JSONObject obj = null;
        JSONParser parser = new JSONParser();

        // parse the JSON file for input data:
        logger.info("parsing JSON input file.");
        try {
            fr = new FileReader("input.json");
            obj = (JSONObject)parser.parse(fr);
        } finally {
            fr.close();
        }

        // get parameters from the json object:
        algorithm = (String)obj.get("synchronization_technique");
        numProcesses = ((Long)obj.get("process_number")).intValue();
        
        discoverPeers();
        
        return (JSONArray)obj.get((processID + 1) + "");
    }
    
    /**
     * 
     * @return
     */
    public static String getGUID()
    {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException uhe) {
            sb.append("localhost");
        }
        sb.append('-');
        sb.append(ManagementFactory.getRuntimeMXBean().getName().substring(0, 4));
        sb.append('-');
        sb.append(System.currentTimeMillis() % 1000);
        sb.append('-');
        sb.append(System.nanoTime() % 1000);
        String guid = sb.toString();
        logger.log(Level.INFO, "generating GUID: {0}", guid);
        return guid;
    }
    
    /**
     * 
     */
    public static void discoverPeers() {
        
        // create a folder
        File dir = new File("peer-info");
        if (!dir.exists()) {
            dir.mkdir();
        }
        
        String myID = getGUID();
        
        // touch a file with name as guid
        try {
            new File("peer-info/" + myID).createNewFile();
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
        
        String[] files = dir.list();
        logger.info("\ntrying to discover other peers...");
        while (files.length < numProcesses) {
            files = dir.list();
            try {
                Thread.sleep((int)(Math.random()*800));
            } catch (InterruptedException ie) {
                logger.warning(ie.getMessage());
            }
        }
        
        Arrays.sort(files);
        for (int i=0; i < files.length; i++) {
            if (files[i].equals(myID)) {
                logger.log(Level.FINE, "pID is {0}", i);
                processID = i;
            } else {
                String host = files[i].split("-")[0];
                logger.fine(String.format("process %d is on %s\n", i, host));
                hostNames[i] = host;
            }
        }
        
    }
    
    /**
     * 
     */
    private static void configureLogging() {
        logger.setUseParentHandlers(false);
        Handler h = new ConsoleHandler();
        h.setFormatter(new Formatter() {
          @Override public String format(LogRecord record) {
            return String.format("(%s):\t%s.%s:\t\"%s\"",
                    record.getLevel(),
                    record.getSourceClassName(),
                    record.getSourceMethodName(),
                    record.getMessage());
          }
        });
        logger.addHandler(h);
    }
}
