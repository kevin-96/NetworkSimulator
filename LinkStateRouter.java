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
        neighborCosts = new HashMap<>(); // Each router knows the costs of its neighbors
    }

    private void findCosts() {
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        for (int i = 0; i < neighbors.size(); i++) {
            int neighbor = neighbors.get(i);
            Packet pingPacket = new PingPacket(this.nsap, neighbor, 1);
            LinkStatePacket linkStatePacket = new LinkStatePacket(this.nsap, neighbor, 1, this.costs) // Link State Packet created
            nic.sendOnLink(i, pingPacket); // Send out the ping packet
            nic.sendOnLink(i, linkStatePacket); // Send out link state packet
        }
        // TODO: Build the routing table based on the graph (Hashmap of costs, hashmap of hashmap)
        Map<Integer, Map<Integer, Long>> routingTable
        
        // Debug: Print a node's graph
        System.out.println(/* a node */);

        // TODO: Perform Djikstra's Algorithm for the routing table (Make function call)
        shortestPath(routingTable);
    }

/////////////////////////////////////////////////////////
    // TODO: Find the shortest path for each router using Dijkstra's Algorithm (GfG)

    class ShortestPath {
    // A utility function to find the vertex with minimum distance value,
    // from the set of vertices not yet included in shortest path tree
    static final int V = 9;
    int minDistance(int dist[], Boolean sptSet[])
    {
        // Initialize min value
        int min = Integer.MAX_VALUE, min_index = -1;
  
        for (int v = 0; v < V; v++)
            if (sptSet[v] == false && dist[v] <= min) {
                min = dist[v];
                min_index = v;
            }
  
        return min_index;
    }
  
    // A utility function to print the constructed distance array
    void printSolution(int dist[], int n)
    {
        System.out.println("Vertex   Distance from Source");
        for (int i = 0; i < V; i++)
            System.out.println(i + " tt " + dist[i]);
    }
  
    // Function that implements Dijkstra's single source shortest path
    // algorithm for a graph represented using adjacency matrix
    // representation
    void dijkstra(int graph[][], int src)
    {
        int dist[] = new int[V]; // The output array. dist[i] will hold
        // the shortest distance from src to i
  
        // sptSet[i] will true if vertex i is included in shortest
        // path tree or shortest distance from src to i is finalized
        Boolean sptSet[] = new Boolean[V];
  
        // Initialize all distances as INFINITE and stpSet[] as false
        for (int i = 0; i < V; i++) {
            dist[i] = Integer.MAX_VALUE;
            sptSet[i] = false;
        }
  
        // Distance of source vertex from itself is always 0
        dist[src] = 0;
  
        // Find shortest path for all vertices
        for (int count = 0; count < V - 1; count++) {
            // Pick the minimum distance vertex from the set of vertices
            // not yet processed. u is always equal to src in first
            // iteration.
            int u = minDistance(dist, sptSet);
  
            // Mark the picked vertex as processed
            sptSet[u] = true;
  
            // Update dist value of the adjacent vertices of the
            // picked vertex.
            for (int v = 0; v < V; v++)
  
                // Update dist[v] only if is not in sptSet, there is an
                // edge from u to v, and total weight of path from src to
                // v through u is smaller than current value of dist[v]
                if (!sptSet[v] && graph[u][v] != 0 && 
                   dist[u] != Integer.MAX_VALUE && dist[u] + graph[u][v] < dist[v])
                    dist[v] = dist[u] + graph[u][v];
        }
  
        // print the constructed distance array
        printSolution(dist, V);
    }


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
