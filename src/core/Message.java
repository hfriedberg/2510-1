
package core;

import java.io.Serializable;

/**
 *
 */
public class Message implements Comparable<Message>, Serializable
{

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

    /**
     * 
     * @param type
     * @param content
     */
    Message(Message.Type type, String content) {
        this.type = type;
        this.content = content;
    }

    /**
     *
     */
    void timeStamp() {
       this.timestamp = System.currentTimeMillis();
    }

    /**
     * 
     * @param o
     * @return
     */
    @Override
    public int compareTo(Message o) {
        return (int) (this.timestamp - o.timestamp);
    }

    /**
     * 
     * @return
     */
    @Override
    public String toString() {
        return String.format("{%s:\"%s\":%d}",
                type.toString(),
                content,
                timestamp);
    }

}
