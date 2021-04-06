/***************
 * Abstract Dynamic Router
 * Author: Christian Duncan
 * Modified by: Joey Germain, Phillip Nam, Kevin Sangurima
 * An abstract class that represents a dynamic router
 ***************/
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public abstract class AbstractDynamicRouter extends Router {
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
        Map<Integer,Long> costs; // Link state packet contains all of the information it has learned from its neighbors
        Set<Integer> nodesVisited;

        public LinkStatePacket(int source, int dest, int hopCount, Map<Integer,Long> costs) {
            // The constructor automatically sets the payload to be the delta time
            super(source, dest, hopCount);
            this.costs = costs;
            this.nodesVisited = new HashSet<>(); // Keep track of the nodes that have been visited
        }
    }

    Debug debug;
    Map<Integer, Long> neighborCosts; // Stores the costs of each router's neighbors (Between neighbors)
    Map<Integer, Map<Integer, Long>> routingTable; // Stores every node in the network's neighbor costs

    public AbstractDynamicRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        debug = Debug.getInstance();  // For debugging!
        neighborCosts = new HashMap<>(); // Each router knows the costs of its neighbors
        routingTable = new HashMap<>(); // Each router builds a routing table for the entire graph
    }

    protected abstract void route(Packet p);

    protected void flood(LinkStatePacket p) {
        ArrayList<Integer> outLinks = nic.getOutgoingLinks();
        int size = outLinks.size();
        for (int i = 0; i < size; i++) {
            if (!p.nodesVisited.contains(outLinks.get(i))) {
                // This packet hasn't reached this node yet - so send it along!
                nic.sendOnLink(i, p);
            }
        }
        if (this.nsap == 10) {
            System.out.println(routingTable);
        }
    }

    // TODO: Remove the "foundCosts" stuff from the code. It makes sure findCosts only runs once, which we're just doing for debugging
    protected boolean foundCosts = false;
    protected void findCosts() {
//        if (foundCosts) {
//            return;
//        }
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        for (int i = 0; i < neighbors.size(); i++) {
            int neighbor = neighbors.get(i);
            Packet pingPacket = new PingPacket(this.nsap, neighbor, 1);
            LinkStatePacket linkStatePacket = new LinkStatePacket(this.nsap, neighbor, 1, this.neighborCosts); // Link State Packet created
            nic.sendOnLink(i, pingPacket); // Send out the ping packet
            nic.sendOnLink(i, linkStatePacket); // Send out link state packet
        }

        foundCosts = true;
        // TODO: Build the routing table based on the graph (Hashmap of costs, hashmap of hashmap)
        // Map<Integer, Map<Integer, Long>> routingTable;

        // Debug: Print a node's graph

        // TODO: Perform Djikstra's Algorithm for the routing table (Make function call)
        // shortestPath(routingTable);
    }

    int costDelay = 10000;
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
                debug.println(3, "(AbstractDynamicRouter.run): I am being asked to transmit: " + toSend.data + " to the destination: " + toSend.destination);
            }

            NetworkInterface.ReceivePair toRoute = nic.getReceived();
            if (toRoute != null) {
                // There is something to route through - or it might have arrived at destination
                process = true;
                debug.println(3, "(AbstractDynamicRouter.run): I received: " + toRoute.data + " from source: " + toRoute.originator);

                if (toRoute.data instanceof PingPacket) {
                    debug.println(4, "Received a PingPacket");
                    // If we receive a ping packet, respond with a pong
                    PingPacket packet = (PingPacket) toRoute.data;
                    int source = packet.source;
                    long pingTime = packet.pingTime;
                    PongPacket pong = new PongPacket(this.nsap, source, 1, pingTime);
//                    nic.transmit(source, pong);
                    nic.sendOnLink(nic.getOutgoingLinks().indexOf(source), pong);
                } else if (toRoute.data instanceof PongPacket) {
                    debug.println(4, "Received a PongPacket");
                    // If we receive a pong packet, use it to store the cost we previously requested
                    PongPacket packet = (PongPacket) toRoute.data;
                    int source = packet.source; // Source of the packet is the destination of the ping packet
                    long cost = packet.pongTime;
                    neighborCosts.put(source, cost);
                    debug.println(5, "Cost(" + this.nsap + ", " + source + ") = " + cost);
                } else if (toRoute.data instanceof LinkStatePacket) {
                    debug.println(4, "Received a LinkStatePacket");
                    LinkStatePacket packet = (LinkStatePacket) toRoute.data;
                    packet.nodesVisited.add(this.nsap);
                    // Get information from packet - new packet?
                    this.routingTable.put(packet.source, packet.costs);
                    // Continue flood routing the packet
                    this.flood(packet);

                    debug.println(5, "Packet source: " + packet.source);
                    debug.println(5, "Packet data: " + packet.costs.toString());
                } else if (toRoute.data instanceof Packet) {
                    debug.println(4, "Received a Packet");
                    route((Packet) toRoute.data);
                } else {
                    debug.println(4, "Packet is not of type: Packet, PingPacket, PongPacket or LinkStatePacket");
                }
            }

            if (!process) {
                // Didn't do anything, so sleep a bit
                try { Thread.sleep(1); } catch (InterruptedException e) { }
            }
        }
    }
}
