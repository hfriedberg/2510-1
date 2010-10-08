
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

   public static void main(String[] args) throws Exception {
        
        configureLogging(Level.ALL);

        // remove existing output file
        File f = new File(FILENAME);
        if (f.exists()) {
            f.delete();
        }
        
        JSONArray tasks = parseInput();
        new Process(processID, tasks).run();
    }

   private static JSONArray parseInput() throws Exception {
        FileReader fr = null;
        JSONObject obj = null;
        JSONParser parser = new JSONParser();

        // parse the JSON file for input data:
        logger.info("parsing JSON input file.\n");
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
        logger.info(String.format("generating GUID: %s\n", guid));
        return guid;
    }
    
   public static void discoverPeers() {
        
        // create a folder on AFS to
        // store peer information
        File dir = new File("peer-info");
        if (!dir.exists()) {
            dir.mkdir();
            dir.deleteOnExit();
        }
        
        
        // touch a file with guid as file name
        String myID = getGUID();
        try {
            File f = new File("peer-info/" + myID);
            f.createNewFile();
            f.deleteOnExit();
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
        
        // wait for other peers to become active
        String[] files = dir.list();
        logger.info("trying to discover other peers...");
        while (files.length < numProcesses) {
            files = dir.list();
            try {
                Thread.sleep((int)(Math.random()*500));
            } catch (InterruptedException ie) {
                logger.warning(ie.getMessage());
            }
        }
        
        // sort the file names to decide IDs
        // and to learn hostnames
        Arrays.sort(files);
        for (int i=0; i < files.length; i++) {
            if (files[i].equals(myID)) {
                logger.fine(String.format("pID is %d", i));
                processID = i;
            } else {
                String host = files[i].split("_")[0];
                logger.fine(String.format("peer %d is on %s", i, host));
                hostNames[i] = (host.equals(HOST))? "localhost": host;
            }
        }
        
    }
    
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
