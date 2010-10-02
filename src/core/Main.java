
package core;

import java.io.File;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
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
    static final Logger logger = Logger.getLogger("core.Main");
    public static String[] hostNames;
    static int processID;
    static String HOST;
    
    static {
        try {
            HOST = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
        }
    }
    
    private Main() {}

    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
        configureLogging(Level.ALL);
        
        JSONArray tasks = parseInput();
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
        hostNames = new String[numProcesses];
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
        sb.append(HOST);
        sb.append('_');
        sb.append(ManagementFactory.getRuntimeMXBean().getName().substring(0, 4));
        sb.append('_');
        sb.append(System.currentTimeMillis() % 1000);
        sb.append('_');
        sb.append(System.nanoTime() % 1000);
        String guid = sb.toString();
        logger.info(String.format("generating GUID: %s", guid));
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
            dir.deleteOnExit();
        }
        
        String myID = getGUID();
        
        // touch a file with name as guid
        try {
            File f = new File("peer-info/" + myID);
            f.createNewFile();
            f.deleteOnExit();
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
        
        String[] files = dir.list();
        logger.info("trying to discover other peers...");
        while (files.length < numProcesses) {
            files = dir.list();
            try {
                Thread.sleep((int)(Math.random()*800));
            } catch (InterruptedException ie) {
                logger.warning(ie.getMessage());
            }
        }
        logger.fine(Arrays.toString(files));
        
        Arrays.sort(files);
        for (int i=0; i < files.length; i++) {
            if (files[i].equals(myID)) {
                logger.fine(String.format("pID is %d", i));
                processID = i;
            } else {
                String host = files[i].split("_")[0];
                logger.fine(String.format("process %d is on %s", i, host));
                hostNames[i] = (host.equals(HOST))? "localhost": host;
            }
        }
        
    }
    
    /**
     * 
     */
    private static void configureLogging(Level level) {
        logger.setUseParentHandlers(false);
        Handler h = new ConsoleHandler();
        h.setFormatter(new Formatter() {
          @Override public String format(LogRecord record) {
            return String.format("(%s):\t%s.%s:\t%s\n",
                    record.getLevel(),
                    record.getSourceClassName(),
                    record.getSourceMethodName(),
                    record.getMessage());
          }
        });
        logger.addHandler(h);
        
        logger.setLevel(level);
        logger.getHandlers()[0].setLevel(level);
    }
}
