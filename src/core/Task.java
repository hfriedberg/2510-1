
package core;

/**
 *  @author Yann Le Gall
 *  ylegall@gmail.com
 *  Sep 26, 2010 8:01:11 PM
 */
public class Task {
    public static final char READ = 'r';
    public static final char WRITE = 'w';
    
    int startTime;
    char action;
    int duration;

    public Task(int startTime, String action, int duration) {
        this.startTime = startTime;
        this.action = action.charAt(0);
        this.duration = duration;
    }
    
    
}
