
package core;

import java.io.FileReader;
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
}
