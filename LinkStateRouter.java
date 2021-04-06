
/***************
 * LinkStateRouter
 * Author: Christian Duncan
 * Modified by: 
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

        public LinkStatePacket(int source, int dest, int hopCount, Map<Integer,Long> costs) {
            // The constructor automatically sets the payload to be the delta time
            super(source, dest, hopCount);
            this.costs = costs;
            this.nodesVisited = new HashSet<>(); // Keep track of the nodes that have been visited
        }
    }

    Map<Integer, Map<Integer, Long>> routingTable; // Stores every node in the network's neighbor costs 

    public LinkStateRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        routingTable = new HashMap<>(); // Each router builds a routing table for the entire graph
    }

    public static class Generator extends Router.Generator {
        public Router createRouter(int id, NetworkInterface nic) {
            return new LinkStateRouter(id, nic);
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
            this.routingTable.put(packet.source, packet.costs);
            // Continue flood routing the packet
            this.flood(packet);

            debug.println(5, "Packet source: " + packet.source);
            debug.println(5, "Packet data: " + packet.costs.toString());
        } else {
            debug.println(4, "Received a Packet");

        }
    }

    @Override
    protected void findCosts() {
        // if (foundCosts) {
        // return;
        // }
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

    protected void saveDistance(PongPacket pong){}

    // // TODO: (FINAL STEP) FIND THE SHORTEST PATH OF EACH NODE TO OTHER NODES!

    // class ShortestPath {
    // // A utility function to find the vertex with minimum distance value,
    // // from the set of vertices not yet included in shortest path tree
    // static final int V = 9;
    // int minDistance(int dist[], Boolean sptSet[])
    // {
    // // Initialize min value
    // int min = Integer.MAX_VALUE, min_index = -1;

    // for (int v = 0; v < V; v++)
    // if (sptSet[v] == false && dist[v] <= min) {
    // min = dist[v];
    // min_index = v;
    // }

    // return min_index;
    // }

    // // A utility function to print the constructed distance array
    // void printSolution(int dist[], int n)
    // {
    // System.out.println("Vertex Distance from Source");
    // for (int i = 0; i < V; i++)
    // System.out.println(i + " tt " + dist[i]);
    // }

    // /** Route the given packet out.
    // In our case, we go to all nodes except the originator
    // **/

    // // Function that implements Dijkstra's single source shortest path
    // // algorithm for a graph represented using adjacency matrix
    // // representation
    // void dijkstra(int graph[][], int src)
    // {
    // int dist[] = new int[V]; // The output array. dist[i] will hold
    // // the shortest distance from src to i

    // // sptSet[i] will true if vertex i is included in shortest
    // // path tree or shortest distance from src to i is finalized
    // Boolean sptSet[] = new Boolean[V];

    // // Initialize all distances as INFINITE and stpSet[] as false
    // for (int i = 0; i < V; i++) {
    // dist[i] = Integer.MAX_VALUE;
    // sptSet[i] = false;
    // }

    // // Distance of source vertex from itself is always 0
    // dist[src] = 0;

    // // Find shortest path for all vertices
    // for (int count = 0; count < V - 1; count++) {
    // // Pick the minimum distance vertex from the set of vertices
    // // not yet processed. u is always equal to src in first
    // // iteration.
    // int u = minDistance(dist, sptSet);

    // // Mark the picked vertex as processed
    // sptSet[u] = true;

    // // Update dist value of the adjacent vertices of the
    // // picked vertex.
    // for (int v = 0; v < V; v++)

    // // Update dist[v] only if is not in sptSet, there is an
    // // edge from u to v, and total weight of path from src to
    // // v through u is smaller than current value of dist[v]
    // if (!sptSet[v] && graph[u][v] != 0 &&
    // dist[u] != Integer.MAX_VALUE && dist[u] + graph[u][v] < dist[v])
    // dist[v] = dist[u] + graph[u][v];
    // }

    // // print the constructed distance array
    // printSolution(dist, V);
    // }
}