
package core;

import java.util.ArrayDeque;
import java.util.Deque;
import org.json.simple.JSONArray;

/**
 *  @author Yann Le Gall
 *  ylegall@gmail.com
 *  Sep 26, 2010 7:59:58 PM
 */
public class Process implements Runnable {
    
    private int ID;
    private Deque<Task> tasks;
    
    public Process(int ID, JSONArray taskArray) {
        
        this.ID = ID;
        tasks = new ArrayDeque<Task>();
        
        for (int j=0; j < taskArray.size(); j++) {
            JSONArray jsonTask = (JSONArray)taskArray.get(j);
            tasks.add(new Task(
                    ((Long)jsonTask.get(0)).intValue(),
                    (String)jsonTask.get(1),
                    ((Long)jsonTask.get(2)).intValue())
                    );
        }
    }
    
    public void run() {
        
    }

}
