package p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.*;//Import the required data structures

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
        if(receivedMessageIds.contains(msg.messageId)){//if the message has already been recorded ignore it-IT24104152
            return;
        }
        receivedMessageIds.add(msg.messageId);//record the unrecorded messages-IT24104152

        System.out.println("Received message from " + msg.sender + ": " + msg.content);

        // TODO: Ex 1 - If this is a broadcast message, forward it to other nodes.
        // Hint: Call `broadcastMessage(msg.content);` here if `msg.isBroadcast` is true.
        if(msg.isBroadcast)//If only the isBroadcast is set to true then forward it-IT24104152
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
        for(rice.pastry.NodeHandle nodeHandle : pastryNeighbors) {//Looping through the neighbors and sending them the messages-IT24104152
            endpoint.route(msg.sender.getId(), msg,  nodeHandle);
        }
    }

    /**
     * Handles unicast function-IT24104152
     */
    public void unicastMessage(String idStr, String content) {
        p2p.SimpleMessage msg = new p2p.SimpleMessage(endpoint.getLocalNodeHandle(), content);

        System.out.println("you entered "+idStr);
        for (NodeHandle nh : node.getLeafSet().asList()) {
            System.out.println(nh.getId().toStringFull());
            if (nh.getId().toStringFull().equals(idStr)) {
                endpoint.route(msg.sender.getId(), msg, nh);
                System.out.println("Unicasting to " + idStr + " message -> " + content);
                return;
            }
        }

        System.out.println("No user with the ID number -> " + idStr);
        return;
    }

    /**
     * Handles multicast function-IT24104152
     */
    public void multicastMessage(String[] nodeIds, String content) {
        p2p.SimpleMessage msg = new p2p.SimpleMessage(endpoint.getLocalNodeHandle(), content);

        for(String idStr : nodeIds){
            boolean found = false;

            for (NodeHandle nh : node.getLeafSet().asList()) {
                if (nh.getId().toStringFull().equals(idStr)) {
                    endpoint.route(msg.sender.getId(), msg, nh);
                    System.out.println("Sending message to " + idStr + " -> " + content);
                    found = true;
                    break; // move to next nodeId
                }
            }

            if(!found){
                System.out.println("No user with the ID number -> " + idStr);
            }
        }
    }


    /**
     * Allows message forwarding in the Pastry network.
     */
    public boolean forward(RouteMessage message) {
        // TODO: Ex 1 - Enable forwarding to allow messages to be routed through the network.
        // Hint: Return `true` here instead of `false` if you want forwarding to be enabled.
        return true;//Returns true for forwarding to be enabled-IT24104152
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
     * Helper method to check if a string is a valid node ID-IT24104152
     */
    private static boolean isNodeId(String string) {
        // Example: Pastry node IDs are hex strings (length depends on your implementation, usually 40 chars)
        return string.matches("[0-9a-fA-F]+");
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
            node.boot(Collections.singletonList(new InetSocketAddress("192.168.1.8", bootstrapPort)));//The address is changed when required-IT24104152
        }

        // Wait for node initialization
        Thread.sleep(3000);
        System.out.println("PeerNode " + peer.nodeId + " is ready.");
        //The instruction message is changed for the added functionalities of unicast, multicast, and getting information about the nodes-IT24104152
        System.out.println("Enter commands ('exit' to shut down, 'status' to check node info, 'broadcast <message>' to send a message to all nodes, 'unicast <node> <message>' to send unicast messages, 'multicast <node> ... <node> <message>' to multicast, 'get' to get information about current nodes) :");

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
                peer.broadcastMessage(messageContent);//added the broadcast functionality
                
                System.out.println("Broadcasting message: " + messageContent);
            } else if(command.startsWith("unicast ")){//This else if block will handle the unicast functionality-IT24104152
                String[] parts = command.split(" ", 3); // split into 3 parts: "unicast", "<nodeId>", "<message>"
                if(parts.length < 3){
                    System.out.println("Usage: unicast <nodeId> <message>");
                    continue;
                }

                String id = parts[1]; // node ID
                String messageContent = parts[2]; // rest of the message

                peer.unicastMessage(id, messageContent);
            } else if(command.startsWith("multicast")){//This else if block will handle the multicast functionality-IT24104152
                String[] parts = command.split(" "); // split by spaces
                if(parts.length < 3) { // at least: multicast <id1> <message>
                    System.out.println("Usage: multicast <id1> <id2> ... <message>");
                    continue;
                }

                // Collect node IDs until the first part that is not a valid node ID
                int firstMessageIndex = 1;
                while(firstMessageIndex < parts.length && isNodeId(parts[firstMessageIndex])) {//this loop stops when it encounters that is not a node id using the helper method
                    firstMessageIndex++;
                }

                if(firstMessageIndex == 1) {//if the loop never advanced then it means no valid node IDs were entered
                    System.out.println("No valid node IDs provided.");
                    continue;
                }

                String[] nodeIds = Arrays.copyOfRange(parts, 1, firstMessageIndex);//copies the node ID parts
                String messageContent = String.join(" ", Arrays.copyOfRange(parts, firstMessageIndex, parts.length));//copies the non node ID parts

                peer.multicastMessage(nodeIds, messageContent);
            } else if(command.startsWith("get")){//This else if block will handle obtaining the node ID-IT24104152
                Set<String> printed = new HashSet<>();
                for(NodeHandle nh : node.getLeafSet().asList()){
                    String id = nh.getId().toStringFull();
                    if(!printed.contains(id)){
                        System.out.println("Leaf node: " + id);
                        printed.add(id);
                    }
                }
             }else {
                //The instruction message is changed for the added functionalities of unicast, multicast, and getting information about the nodes-IT24104152
                System.out.println("Unknown command. Available commands: 'exit' to shut down, 'status' to check node info, 'broadcast <message>' to send a message to all nodes, 'unicast <node> <message>' to send unicast messages, 'multicast <node> ... <node> <message>' to multicast, 'get' to get information about current nodes");
            }
        }
    }
}
