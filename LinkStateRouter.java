/***************
 * LinkStateRouter
 * Author: Christian Duncan
 * Modified by: Joey Germain, Phillip Nam, Kevin Sangurima
 * Represents a router that uses a Link State Routing algorithm.
 ***************/
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class LinkStateRouter extends Router {
    // A generator for the given LinkStateRouter class
    public static class Generator extends Router.Generator {
        public Router createRouter(int id, NetworkInterface nic) {
            return new LinkStateRouter(id, nic);
        }
    }

    public static class Packet {
        // This is how we will store our Packet Header information
        int source;
        int dest;
        int hopCount;  // Maximum hops to get there
        Object payload;  // The payload!
        
        public Packet(int source, int dest, int hopCount) {
            this.source = source;
            this.dest = dest;
            this.hopCount = hopCount;
        }

    }

    /* TODO: Construct the Link State Packet
    Packet includes:
    - Identity of sender
    - Sequence number (32-bit) and age (decrement age after each second)
    - List of neighbors
    */

    // Determining when the packet is constructed (Important)

    public static class PingPacket extends Packet {
        long pingTime;

        public PingPacket(int source, int dest, int hopCount) {
            // The constructor automatically sets the payload to be the current time
            super(source, dest, hopCount);
            this.pingTime = System.currentTimeMillis();
        }
    }

    public static class PongPacket extends Packet {
        long pongTime;

        public PongPacket(int source, int dest, int hopCount, long pingTime) {
            // The constructor automatically sets the payload to be the delta time
            super(source, dest, hopCount);
            this.pongTime = System.currentTimeMillis() - pingTime;
        }
    }
    
    public static class LinkStatePacket extends Packet {
        Map<Integer,Long> costs; // Link state packet contain all of the information it has learned from its neighbors

        public LinkStatePacket(int source, int dest, int hopCount, Map<Integer,Long> costs) {
            // The constructor automatically sets the payload to be the delta time
            super(source, dest, hopCount);
            this.costs = costs;
        }
    }

    Debug debug;
    Map<Integer, Long> neighborCosts; // Stores the costs of each router's neighbors (Between neighbors)
    Map<Integer, Map<Integer, Long>> routingTable; // Stores every node in the network's neighbor costs

    public LinkStateRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        debug = Debug.getInstance();  // For debugging!
        neighborCosts = new HashMap<>(); // 
    }

    private void findCosts() {
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        for (int i = 0; i < neighbors.size(); i++) {
            int neighbor = neighbors.get(i);
            Packet pingPacket = new PingPacket(this.nsap, neighbor, 1);
            LinkStatePacket linkStatePacket = new LinkStatePacket(this.nsap, neighbor, 1, this.costs) // Link State Packet created
            nic.sendOnLink(i, pingPacket); // could also use sendOnLink (or toSend?)
            nic.sendOnLink(i, linkStatePacket); // Send out link state packet
        }
        // TODO: Build the routing table based on the graph (Hashmap of costs, hashmap of hashmap)
        Map<Integer, Map<Integer, Long>> routingTable
        

        // Debug: Print a node's graph
        // Perform Djikstra's Algorithm for the routing table (Make function call)
    }

/////////////////////////////////////////////////////////


////////////////////////////////////////////////////////
    

    int costDelay = 5000;
    public void run() {
        // findCosts();
        long nextFindCost = System.currentTimeMillis() + 1000;
        while (true) {
            if (System.currentTimeMillis() > nextFindCost) {
                nextFindCost = System.currentTimeMillis() + costDelay;
                findCosts();
            }
            // See if there is anything to process
            boolean process = false;
            NetworkInterface.TransmitPair toSend = nic.getTransmit();
            if (toSend != null) {
                // There is something to send out
                process = true;
                debug.println(3, "(LinkStateRouter.run): I am being asked to transmit: " + toSend.data + " to the destination: " + toSend.destination);
            }

            NetworkInterface.ReceivePair toRoute = nic.getReceived();
            if (toRoute != null) {
                // There is something to route through - or it might have arrived at destination
                process = true;
                debug.println(3, "(LinkStateRouter.run): I received: " + toRoute.data + " from source: " + toRoute.originator);

                if (toRoute.data instanceof PingPacket) {
                    debug.println(4, "Received a PingPacket");
                    // If we receive a ping packet, respond with a pong
                    PingPacket packet = (PingPacket) toRoute.data;
                    int source = packet.source;
                    long pingTime = packet.pingTime
                    PongPacket pong = new PongPacket(this.nsap, source, 1, pingTime);
                    nic.transmit(source, pong);
                } else if (toRoute.data instanceof PongPacket) {
                    debug.println(4, "Received a PongPacket");
                    // If we receive a pong packet, use it to store the cost we previously requested
                    PongPacket packet = (PongPacket) toRoute.data;
                    int source = packet.source; // Source of the packet is the destination of the ping packet
                    long cost = packet.pongTime;
                    neighborCosts.put(source, cost);
                    debug.println(5, "Cost(" + this.nsap + ", " + source + ") = " + cost);
                } else if (toRoute.data instanceof LinkStatePacket) {
                    // TODO
                } else if (toRoute.data instanceof Packet) {
                    debug.println(4, "Received a Packet");
                    // TODO: route normally
                } else {
                    debug.println(4, "Packet is not of type: Packet, PingPacket, or PongPacket");
                }
            }

            if (!process) {
                // Didn't do anything, so sleep a bit
                try { Thread.sleep(1); } catch (InterruptedException e) { }
            }
        }
    }
}
