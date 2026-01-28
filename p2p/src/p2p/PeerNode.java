package p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import rice.environment.Environment;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

/**
 * Represents a P2P node in the Pastry network.
 * Supports sending, receiving, and broadcasting messages.
 */
public class PeerNode implements Application {
    private Endpoint endpoint;
    private String nodeId;
    private PastryNode node;

    // Store received message IDs to prevent duplicates (Exercise 2)
    private Set<String> receivedMessageIds = new HashSet<>();

    /**
     * Initializes a new PeerNode and registers it within the network.
     * @param node The PastryNode associated with this peer.
     */
    public PeerNode(PastryNode node) {
        this.endpoint = node.buildEndpoint(this, "PeerNode");
        this.nodeId = node.getId().toString();
        this.node = node;
        endpoint.register();
    }

    /**
     * Handles incoming messages. Ensures broadcast messages are not rebroadcasted multiple times.
     */
    public void deliver(Id id, Message message) {
        p2p.SimpleMessage msg = (p2p.SimpleMessage) message;

        // TODO: Ex 2 - Prevent flooding by checking if the message ID was already received.
        // Hint: Use `receivedMessageIds.contains(msg.messageId)` to check duplicates.
        // If it's a duplicate, ignore it.
        if(receivedMessageIds.contains(msg.messageId)){
            return;
        }

        receivedMessageIds.add(msg.messageId);
        System.out.println("Received message from " + msg.sender + ": " + msg.content);

        // TODO: Ex 1 - If this is a broadcast message, forward it to other nodes.
        // Hint: Call `broadcastMessage(msg.content);` here if `msg.isBroadcast` is true.
//        if(msg.isBroadcast())
        broadcastMessage(msg.content);
    }

    /**
     * TODO: Ex 1 - Implement a method to broadcast a message to all connected nodes.
     * Hints:
     * - Use `node.getLeafSet().asList()` to get a list of neighbors.
     * - Iterate over the neighbors and send a `SimpleMessage`.
     * - Use `endpoint.route()` to send messages.
     */

    public void broadcastMessage(String content) {
        p2p.SimpleMessage msg = new p2p.SimpleMessage(endpoint.getLocalNodeHandle(), content);

        System.out.println("Broadcasting message -> " + content);

        // Get all connected nodes and send the message
        List<rice.pastry.NodeHandle> pastryNeighbors = node.getLeafSet().asList();
        
        // TODO: Ex 1 - Loop through `pastryNeighbors` and route the message to them.
        for(rice.pastry.NodeHandle nodeHandle : pastryNeighbors) {
            endpoint.route(msg.sender.getId(), msg,  nodeHandle);
        }
    }

    /**
     * Allows message forwarding in the Pastry network.
     */
    public boolean forward(RouteMessage message) {
        // TODO: Ex 1 - Enable forwarding to allow messages to be routed through the network.
        // Hint: Return `true` here instead of `false` if you want forwarding to be enabled.
        return true;
    }

    /**
     * Updates when a node joins or leaves the network.
     */
    public void update(NodeHandle handle, boolean joined) {
        if (joined) {
            System.out.println("Node joined: " + handle);
        } else {
            System.out.println("Node left: " + handle);
        }
    }

    /**
     * Main method to start the peer node.
     * @param args Command-line arguments: <port> [bootstrap]
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java PeerNode <port> [bootstrap]");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]); // Parse port number
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number. Please provide a valid integer.");
            return;
        }

        Environment env = new Environment();

        PastryNodeFactory factory = new SocketPastryNodeFactory(new RandomNodeIdFactory(env), port, env);
        PastryNode node = factory.newNode();
        PeerNode peer = new PeerNode(node);

        // Handle bootstrap logic
        if (args.length > 1 && args[1].equalsIgnoreCase("bootstrap")) {
            System.out.println("Starting bootstrap node on port " + port + "...");
            node.boot(Collections.emptyList()); // No bootstrap node
        } else {
            int bootstrapPort = 10001;
            if (args.length > 1) {
                try {
                    bootstrapPort = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid bootstrap port. Using default: " + bootstrapPort);
                }
            }

            System.out.println("Bootstrapping from localhost:" + bootstrapPort + "...");
            node.boot(Collections.singletonList(new InetSocketAddress("172.28.8.120", bootstrapPort)));
        }

        // Wait for node initialization
        Thread.sleep(3000);
        System.out.println("PeerNode " + peer.nodeId + " is ready.");
        System.out.println("Enter commands ('exit' to shut down, 'status' to check node info, 'broadcast <message>' to send a message to all nodes):");

        // Interactive CLI for user input
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String command;

        while (true) {
            System.out.print("> "); // Prompt user for input
            command = reader.readLine().trim();

            if (command.equalsIgnoreCase("exit")) {
                System.out.println("Shutting down...");
                env.destroy();
                break;
            } else if (command.equalsIgnoreCase("status")) {
                System.out.println("Node ID: " + peer.nodeId);
                System.out.println("Node is running on port " + port + "...");
            } else if (command.startsWith("broadcast ")) {
                String messageContent = command.substring(10);
                
                // TODO: Ex 1 - Call the broadcast method to send a message to all nodes.
                // Hint: Use `peer.broadcastMessage(messageContent);`
                peer.broadcastMessage(messageContent);
                
                System.out.println("Broadcasting message: " + messageContent);
            } else {
                System.out.println("Unknown command. Available commands: 'status', 'broadcast <message>', 'exit'");
            }
        }
    }
}
