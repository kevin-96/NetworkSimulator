
/***************
 * LinkStateRouter
 * Authors: Kevin Sangurima, Joey Germain, Phillip Nam
 * Represents a router that uses a Link State Routing algorithm.
 ***************/

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class LinkStateRouter extends AbstractDynamicRouter {

    public static class LinkStatePacket extends Packet {
        Map<Integer,Long> costs; // Link state packet contains all of the information it has learned from its neighbors
        Set<Integer> nodesVisited;

        public LinkStatePacket(int source, int dest, Map<Integer,Long> costs) {
            // Hop count is irrelevant because the packet automatically dies once it has visited all routers
            super(source, dest, Integer.MAX_VALUE);
            this.costs = costs;
            this.nodesVisited = new HashSet<>(); // Keep track of the nodes that have been visited
        }
    }

    Map<Integer, Map<Integer, Long>> linkStateTable; // Stores <nsap, neighborCosts> -- every node in the network's neighbor costs 
    Map<Integer, Integer> routingTable; // Stores <dest, nextStep> -- if going from this to dest, route to nextStep

    public LinkStateRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        linkStateTable = new HashMap<>();
        routingTable = new HashMap<>();
    }

    public static class Generator extends Router.Generator {
        public Router createRouter(int id, NetworkInterface nic) {
            return new LinkStateRouter(id, nic);
        }
    }

    // Distance/Link Pair Class (DL Pair)
    public static class DLPair {
        long distance;
        int link;

        public DLPair (long distance, int link) {
            this.distance = distance;
            this.link = link;
        }
    }

    // Calculate shortest paths from this node to every other node using Djikstra's algorithm. Populates this.paths
    public void findShortestPaths() {        
        Map<Integer, DLPair> workingTable = new HashMap<>(); // All the nodes with their distances
        Map<Integer, DLPair> finalTable = new HashMap<>();
        workingTable.put(this.nsap, new DLPair(0,-1));
        while (!workingTable.isEmpty()) {
            // 1. go through table find the key (k) with the smallest distance (d). Let l be the link it uses
            Map.Entry<Integer, DLPair>  min = null;
            for (Map.Entry<Integer, DLPair> entry : workingTable.entrySet()) {
                if (min == null || entry.getValue().distance < min.getValue().distance) {
                    min = entry;
                }
            }

            // Store info about the selected node
            int nsap = min.getKey();
            DLPair info = min.getValue();
            
            // 1a. remove the key from the workingTable and move it to the finalTable
            finalTable.put(nsap, info);
            workingTable.remove(nsap);
            
            // 2. grab the links for that key (links are stored in the linkStateTable)
            Set<Integer> links = null;
            try {
                links = linkStateTable.get(nsap).keySet();
            } catch (NullPointerException e) {
                if (linkStateTable.isEmpty()) {
                    System.out.println("Router " + this.nsap + ": My link state table is empty. Not ready to build a routing table.");
                } else {
                    System.out.println("Router " + this.nsap + ": Could not find router " + nsap + " in my link state table. All we have is " + linkStateTable);
                }
                return;
            }

            //Relaxation Process
            // 3. for each link, get the distance to that link, and add it to d
            for (Integer link : links) {
                long distance = linkStateTable.get(nsap).get(link) + info.distance;
                
                // 4. if that distance is better than the distance stored in workingTable, then update the workingTable with that distance and link l (also check for nulls)
                DLPair currentValues = finalTable.get(nsap);
                if (currentValues == null) {
                    finalTable.put(nsap, new DLPair(distance, link)); // TODO: replace with workingTable(?) and replace distance with small distance and the new link
                } else if (distance < currentValues.distance) {
                    // set new distance
                    currentValues.distance = distance;
                    currentValues.link = link;
                }
            }
        }
         // finalTable is the routing table we want to use in the route function
         this.routingTable.clear();
         for (Map.Entry<Integer, DLPair> entry : finalTable.entrySet()) {
            this.routingTable.put(entry.getKey(), entry.getValue().link);
        }
    }

    protected void flood(LinkStatePacket p) {
        ArrayList<Integer> outLinks = nic.getOutgoingLinks();
        int size = outLinks.size();
        for (int i = 0; i < size; i++) {
            if (!p.nodesVisited.contains(outLinks.get(i))) {
                // This packet hasn't reached this node yet - so send it along!
                nic.sendOnLink(i, p);
            }
        }
    }

    @Override
    protected void route(Packet p) {
        if (p instanceof LinkStatePacket) {
            debug.println(4, "Received a LinkStatePacket");
            LinkStatePacket packet = (LinkStatePacket) p;
            packet.nodesVisited.add(this.nsap);
            // Get information from packet - new packet?
            this.linkStateTable.put(packet.source, packet.costs);
            // Continue flood routing the packet
            this.flood(packet);

            debug.println(5, "Packet source: " + packet.source);
            debug.println(5, "Packet data (costs): " + packet.costs.toString());
        } else {
            debug.println(4, "Received a Packet");

            if (p.dest == this.nsap) {
                nic.trackArrivals(p.payload);
            } else {
                // Lookup next stop from routing table and send the packet there
                Integer nextStop = routingTable.get(p.dest);
                if (nextStop != null) {
                    int linkIndex = nic.getOutgoingLinks().indexOf(nextStop);
                    nic.sendOnLink(linkIndex, p);
                } else {
                    // Destination is not in the routing table yet
                    debug.println(4, "Router " + this.nsap + ": Router " + p.dest + " is not in my routing table yet. All I have is " + routingTable.toString());
                    // drop packet?
                }
                
            }
        }
    }

    @Override
    protected void findCosts() {
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        for (int i = 0; i < neighbors.size(); i++) {
            int neighbor = neighbors.get(i);
            // Send a ping to the neighbor (expecting a "pong" back)
            Packet pingPacket = new PingPacket(super.nsap, neighbor, 1);
            nic.sendOnLink(i, pingPacket); // Send out the ping packet

            // Send link state to the neighbor (to flood across the graph)
            LinkStatePacket linkStatePacket = new LinkStatePacket(super.nsap, neighbor, super.neighborCosts);
            linkStatePacket.nodesVisited.add(this.nsap);
            nic.sendOnLink(i, linkStatePacket); // Send out link state packet
        }

        // Add our own neighbors to the link state table, in addition to the ones we get from other routers
        linkStateTable.put(this.nsap, this.neighborCosts);

        // Perform Djikstra's Algorithm to build a list of shortest paths using the routing table
        findShortestPaths();
    }

}