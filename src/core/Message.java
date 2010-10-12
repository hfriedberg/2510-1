
package core;

import java.io.Serializable;

// class for storing messages
public class Message implements Comparable<Message>, Serializable {

    Type type;
    String content;
    long timestamp;

    static enum Type {
        ACK,
        INFO,
        IDLE,
        CS_REQUEST,
        CS_RELEASE,
        TOKEN,
        END
    }

   Message(Type type, String content) {
        this.type = type;
        this.content = content;
    }
    
   Message(Type type, long timeStamp) {
        this.type = type;
        this.timestamp = timeStamp;
    }

    void timeStamp() {
       this.timestamp = System.currentTimeMillis();
    }
    
    void setTimeStamp(long time) {
        this.timestamp = time;
    }

    public int compareTo(Message o) {
        return (int) (this.timestamp - o.timestamp);
    }

    public String toString() {
        return String.format("{%s:\"%s\":%d}",
                type.toString(),
                content,
                timestamp);
    }

}
