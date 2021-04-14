
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
        Map<Integer,Long> costs; // The source node's neighbor cost information (map of <nsap, distance>)
        Set<Integer> nodesVisited; // Set of nsaps representing the routers that have seen this packet

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

    // Calculate shortest paths from this node to every other node using Djikstra's algorithm. Populates this.routingTable
    public void findShortestPaths() {        
        Map<Integer, DLPair> workingTable = new HashMap<>();
        Map<Integer, DLPair> finalTable = new HashMap<>();

        // Initially, the working table just contains this
        workingTable.put(this.nsap, new DLPair(0,-1));

        // Perform Dijkstra's algorithm until the working table is empty
        while (!workingTable.isEmpty()) {
            // 1. Go through table find the node U with the smallest distance (d). Let l be the link it uses.
            Map.Entry<Integer, DLPair>  min = null;
            for (Map.Entry<Integer, DLPair> entry : workingTable.entrySet()) {
                if (min == null || entry.getValue().distance < min.getValue().distance) {
                    min = entry;
                }
            }

            // Store info about the selected node, U
            int nsapU = min.getKey();
            DLPair infoU = min.getValue();
            
            // 1a. Move U from the workingTable to the finalTable
            finalTable.put(nsapU, infoU);
            workingTable.remove(nsapU);
            
            // 2. Grab U's outgoing links from the linkStateTable. Bail out if we don't have all the information we need to continue.
            Set<Integer> linksU = null;
            try {
                linksU = linkStateTable.get(nsapU).keySet();
            } catch (NullPointerException e) {
                if (linkStateTable.isEmpty()) {
                    System.out.println("Router " + this.nsap + ": My link state table is empty. Not ready to build a routing table.");
                } else {
                    System.out.println("Router " + this.nsap + ": Could not find router " + nsapU + " in my link state table. All I have is " + linkStateTable);
                }
                return;
            }

            // 3. Perform the relaxation process for each node V connected to U
            for (Integer nsapV : linksU) {
                // Get the distance from source to V when going through U
                long distanceV = linkStateTable.get(nsapU).get(nsapV) + infoU.distance;
                
                // 4. If that distance is better than the distance currently stored in workingTable, update the workingTable with that distance and link l (also check for nulls)
                if (!finalTable.containsKey(nsapV)) {
                    DLPair currentInfoV = workingTable.get(nsapV);
                    if (currentInfoV == null || distanceV < currentInfoV.distance) {
                        // We know that going through U is a better way to get to V than what we have currently, so update the working table.
                        workingTable.put(nsapV, new DLPair(distanceV, infoU.link == -1 ? nsapV : infoU.link)); // If the link is -1, use ourself instead.
                    }
                }
            }
        }

        // Re-build the routing table using the results of the search we just did
        this.routingTable.clear();
        for (Map.Entry<Integer, DLPair> entry : finalTable.entrySet()) {
            Integer dest = entry.getKey();
            Integer link = entry.getValue().link;
            this.routingTable.put(dest, link);
        }
    }

    // Flood a packet to every other LinkStateRouter in the network
    private void flood(LinkStatePacket p) {
        ArrayList<Integer> outLinks = nic.getOutgoingLinks();
        int size = outLinks.size();
        for (int i = 0; i < size; i++) {
            if (!p.nodesVisited.contains(outLinks.get(i))) {
                // This packet hasn't reached this node yet - so send it along!
                nic.sendOnLink(i, p);
            }
        }
    }

    // Print out the network (for debugging)
    private void printNetwork() {
        System.out.printf("Network for %d:\n", this.nsap);
        for (Map.Entry<Integer, Map<Integer, Long>> entry : linkStateTable.entrySet()) {
            int source = entry.getKey();
            Map<Integer, Long> neighbors = entry.getValue();
            System.out.print("Src: " + source + " ");
            for (Map.Entry<Integer, Long> neighborEntry : neighbors.entrySet()) {
                int nsap = neighborEntry.getKey();
                long distance = neighborEntry.getValue();

                System.out.printf("[%d %d] ", nsap, distance);
            }
            System.out.println();
        }
    }

    @Override
    protected void route(Packet p) {
        if (p instanceof LinkStatePacket) {
            // We received a link state packet. These are handled differently from regular data packets
            debug.println(4, "Received a LinkStatePacket");

            LinkStatePacket packet = (LinkStatePacket) p;
            packet.nodesVisited.add(this.nsap); // Update packet to say it has visited this router
            this.linkStateTable.put(packet.source, packet.costs); // Get link state information from the packet
            this.flood(packet); // Continue flood routing the packet

            debug.println(5, "Packet source: " + packet.source);
            debug.println(5, "Packet data (costs): " + packet.costs.toString());
        } else {
            // This is a normal data packet
            debug.println(4, "Received a Packet");

            if (p.dest == this.nsap) {
                // Packet has arrived at its destination, so report that it was received successfully!
                nic.trackArrivals(p.payload);
            } else {
                // Lookup the next stop from routing table and send the packet there
                Integer nextStop = routingTable.get(p.dest);
                if (nextStop != null) {
                    int linkIndex = nic.getOutgoingLinks().indexOf(nextStop);
                    nic.sendOnLink(linkIndex, p);
                } else {
                    // Destination is not in the routing table yet. Drop the packet.
                    debug.println(4, "Router " + this.nsap + ": Router " + p.dest + " is not in my routing table yet. All I have is " + routingTable.toString());
                }
                
            }
        }
    }

    @Override
    protected void findCosts() {
        // For every neighbor of the curent router:
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        for (int i = 0; i < neighbors.size(); i++) {
            int neighborNsap = neighbors.get(i);

            // Send a ping to the neighbor (expecting a "pong" back)
            PingPacket pingPacket = new PingPacket(super.nsap, neighborNsap, 1);
            nic.sendOnLink(i, pingPacket); // Send out the ping packet

            // Send link state to the neighbor (to flood across the network)
            LinkStatePacket linkStatePacket = new LinkStatePacket(super.nsap, neighborNsap, super.neighborCosts);
            linkStatePacket.nodesVisited.add(this.nsap);
            nic.sendOnLink(i, linkStatePacket); // Send out link state packet
        }

        // Add our own neighbors to the link state table, in addition to the ones we get from other routers
        linkStateTable.put(this.nsap, this.neighborCosts);

        // (Re)build the routing table
        findShortestPaths();
    }

}