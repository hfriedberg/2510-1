
package core;

import java.io.File;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
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
    public static final String HOST = "localhost";
    
    private Main() {}

    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        int processID = -1;
        
        try {
            processID = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.err.println(nfe.getMessage());
            System.exit(-1);
        }
        
        JSONArray tasks = parseInput(processID);
        discoverPeers();
        System.exit(0);
        
        new Process(processID, tasks).run();
    }

    /**
     * 
     * @param pid
     * @return
     * @throws Exception
     */
    private static JSONArray parseInput(int pid) throws Exception {
        FileReader fr = null;
        JSONObject obj = null;
        JSONParser parser = new JSONParser();

        // parse the JSON file for input data:
        try {
            fr = new FileReader("input.json");
            obj = (JSONObject)parser.parse(fr);
        } finally {
            fr.close();
        }

        // get parameters from the json object:
        algorithm = (String)obj.get("synchronization_technique");
        numProcesses = ((Long)obj.get("process_number")).intValue();
        return (JSONArray)obj.get((pid + 1) + "");
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
        return sb.toString();
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
            System.err.println(e.getMessage());
        }
        
        String[] files = dir.list();
        System.out.print("\ntrying to discover other peers...");
        while (files.length < numProcesses) {
            files = dir.list();
            try {
                Thread.sleep((int)(Math.random()*800));
            } catch (InterruptedException ie) {
                System.err.println(ie.getMessage());
            }
        }
        System.out.println("done.\n");
        
        Arrays.sort(files);
        
        for (int i=0; i < files.length; i++) {
            if (files[i].equals(myID)) {
                System.out.printf("my id is %d\n",i);
            } else {
                System.out.printf("process %d is on %s\n", i, files[i].split("-")[0]);
            }
        }
        
    }
}
