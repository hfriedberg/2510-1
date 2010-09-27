
package core;

import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


/**
 *  @author Yann Le Gall
 *  ylegall@gmail.com
 *  Sep 26, 2010 7:34:40 PM
 */
public class Main {
    
    static Process[] processes;
    static final String fileName = "out.txt";
    
    public static void main(String[] args) throws Exception {
        
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
        String algorithm = (String)obj.get("synchronization_technique");
        System.out.println(algorithm); // DEBUG
        int numProcesses = ((Long)obj.get("process_number")).intValue();
        System.out.println(numProcesses); // DEBUG
        processes = new Process[numProcesses];
        
        // create processes with tasks:
        for (int i=1; i <= numProcesses; i++) {
            processes[i-1] = new Process(i,(JSONArray)obj.get(i + ""));
        }
        
    }
}
