package p2p;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import java.util.UUID; // TODO: Ex 2 - Import UUID to generate unique message IDs.

/**
 * Represents a message that can be sent in the P2P network.
 */
class SimpleMessage implements Message {
    public NodeHandle sender;
    public String content;

    //the below filed used by PeerNode.java which has not been initially added-IT24104152
    public boolean isBroadcast;

    // TODO: Ex 2 - Add a unique message ID to prevent duplicate broadcasts.
    // Hint: Use `UUID.randomUUID().toString()` to generate a unique ID.
    public String messageId;

    /**
     * Constructor for creating a message.
     * @param sender The sender node.
     * @param content The message content.
     */
    public SimpleMessage(NodeHandle sender, String content) {
        this.sender = sender;
        this.content = content;

        // TODO: Ex 2 - Generate a unique message ID.
        this.messageId = UUID.randomUUID().toString();
    }

    /**
     * Returns the priority of the message.
     */
     public int getPriority() {
        return Message.LOW_PRIORITY;
    }
}