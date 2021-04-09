
/***************
 * LinkStateRouter
 * Author: Christian Duncan
 * Modified by: 
 * Represents a router that uses a Link State Routing algorithm.
 ***************/

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class LinkStateRouter extends AbstractDynamicRouter {

    public static class RoutedPacket extends Packet  {
        public List<Integer> route;

        public RoutedPacket (Packet packet, List<Integer> route) {
            super(packet.source, packet.dest, packet.hopCount, packet.payload);
            this.route = route;
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

    Map<Integer, Map<Integer, Long>> linkStateTable; // Stores <nsap, neighborCosts> -- every node in the network's neighbor costs 
    Map<Integer, Integer> routingTable; // Stores <dest, nextStep> -- if going from this to dest, route to nextStep

    public LinkStateRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        linkStateTable = new HashMap<>(); // Each router builds a table that shows link costs for the entire network
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
        // TODO: calculate shortest paths
        Map<Integer, DLPair> workingTable = new HashMap<>(); // All the nodes with their distances
        Map<Integer, DLPair> finalTable = new HashMap<>();
        workingTable.put(this.nsap,new DLPair(0,-1));
        while (!workingTable.isEmpty()) {
            // 1. go through table find the key (k) with the smallest distance (d). Let l be the link it uses
            Map.Entry<Integer, DLPair>  min = null;
            for (Map.Entry<Integer, DLPair> entry : workingTable.entrySet()) {
                DLPair info = entry.getValue();
                if (min == null || info.distance < min.getValue().distance) {
                    min = entry;
                }
            }

            // Store info about the selected node
            int nsap = min.getKey();
            DLPair info = min.getValue();
            
            // 1a. remove the key from the workingTable and move it to the finalTable
            finalTable.put(nsap, info);
            workingTable.remove(nsap);
            
            // 2. grab the links for that key (links are stored in the routingTable//linkStateTable) // ASK PHIL ABOUT ROUTINGTABLE
            Set<Integer> links = linkStateTable.get(nsap).keySet();
        
            // 3. for each link, get the distance to that link, and add it to d
            for (Integer link : links) {
                long distance = linkStateTable.get(nsap).get(link) + info.distance;
                
                // 4. if that distance is better than the distance stored in workingTable, then update the workingTable with that distance and link l (also check for nulls)
                // Integer previousDistance = workingTable.get(...);
                // if (previousDistance == null || distance < previousDistance) {
                //     // set new distance
                //     workingTable.set(...);
                // }
            
            }

            
        }
         // final table is the routing table we want to use in the route function
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
            debug.println(5, "Packet data: " + packet.costs.toString());
        } else {
            debug.println(4, "Received a Packet");

            if (p.dest == this.nsap) {
                nic.trackArrivals(p.payload);
            } else {
                // RoutedPacket routedPacket;
                // if (p instanceof RoutedPacket) {
                //     // Packet was already assigned a route
                //     routedPacket = (RoutedPacket) p;
                // } else {
                //     // Assign a route
                //     routedPacket = new RoutedPacket(p, routingTable.get(p.dest));
                //     routedPacket.route = routingTable.get(p.dest);
                // }
                // // At this point, we know the route, so send to the next node
                // int nextStop = routedPacket.route.get(0);
                // int linkIndex = nic.getOutgoingLinks().indexOf(nextStop);
                // routedPacket.route = routedPacket.route.subList(1, routedPacket.route.size()); // Chop off current node since it was visited
                // nic.sendOnLink(linkIndex, routedPacket);
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
            LinkStatePacket linkStatePacket = new LinkStatePacket(super.nsap, neighbor, 1, super.neighborCosts);
            nic.sendOnLink(i, linkStatePacket); // Send out link state packet
        }

        // Perform Djikstra's Algorithm to build a list of shortest paths using the routing table
        // findShortestPaths();
    }

}