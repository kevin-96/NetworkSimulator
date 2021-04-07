
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

    private static class RoutingPaths {
        private Map<Integer, Map<Integer, List<Integer>>> paths; // paths[0][6] => path from 0 - 6 

        public RoutingPaths() {
            paths = new HashMap<>();
        }

        public void put(int src, int dest, List<Integer> path) {
            paths.putIfAbsent(src, new HashMap<>());
            paths.get(src).put(dest, path);
        }

        public List<Integer> get(int src, int dest) {
            try {
                return paths.get(src).get(dest);
            }
            catch (Exception e) {
                return null;
            }
        }

        // public void calculate(int src, int dest, )

    }

    Map<Integer, Map<Integer, Long>> routingTable; // Stores every node in the network's neighbor costs 
    RoutingPaths paths; // paths[0][6] => path from 0 - 6 

    public LinkStateRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        routingTable = new HashMap<>(); // Each router builds a routing table for the entire graph
        paths = new RoutingPaths();
    }

    public static class Generator extends Router.Generator {
        public Router createRouter(int id, NetworkInterface nic) {
            return new LinkStateRouter(id, nic);
        }
    }

    private void calculateRoutingPaths() {
        RoutingPaths paths = new RoutingPaths();
        for (Integer src : this.routingTable.keySet()) {
            for (Integer dest : this.routingTable.get(src).keySet()) {
                if (!src.equals(dest)) {
                    paths.put(src, dest, djikstra(src, dest));
                }
            }
        }
        this.paths = paths;
    }
    private List<Integer> djikstra(int src, int dest) {
        List<Integer> path = new ArrayList<>();
        
        // TODO: calculate shortest path
        
        return path;
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
            this.routingTable.put(packet.source, packet.costs);
            // Continue flood routing the packet
            this.flood(packet);

            debug.println(5, "Packet source: " + packet.source);
            debug.println(5, "Packet data: " + packet.costs.toString());
        } else {
            debug.println(4, "Received a Packet");
            if (p.dest != nsap) {
                // TODO: Djikstra?

            }
        }
    }

    @Override
    protected void findCosts() {
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        for (int i = 0; i < neighbors.size(); i++) {
            int neighbor = neighbors.get(i);
            Packet pingPacket = new PingPacket(super.nsap, neighbor, 1);
            LinkStatePacket linkStatePacket = new LinkStatePacket(super.nsap, neighbor, 1, super.neighborCosts);
            nic.sendOnLink(i, pingPacket); // Send out the ping packet
            nic.sendOnLink(i, linkStatePacket); // Send out link state packet
        }

        // TODO: Build the routing table based on the graph (Hashmap of costs, hashmap (Most likely will need to change this. to super.)
        // of hashmap)
        // Map<Integer, Map<Integer, Long>> routingTable;

        // Debug: Print a node's graph

        // TODO: Perform Djikstra's Algorithm for the routing table (Make function call)
        // shortestPath(routingTable);

    }

}