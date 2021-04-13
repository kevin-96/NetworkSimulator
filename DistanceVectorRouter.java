import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/***************
 * DistanceVectorRouter Author: Christian Duncan Modified by: Represents a
 * router that uses a Distance Vector Routing algorithm.
 ***************/

public class DistanceVectorRouter extends AbstractDynamicRouter {
    Map<Integer, Long> routingTable; // Change to routingTable
    Map<Integer, Integer> routingTableIndex;
    // routingTable of each link
    ArrayList<Map<Integer, Long>> neighborTables; // Tables that are being recieved from neighbors

    public DistanceVectorRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        routingTable = new HashMap<>();
        routingTableIndex = new HashMap<>();
        int size = nic.getOutgoingLinks().size(); // number of links
        neighborTables = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            neighborTables.add(null);

        }
        // System.out.println(super.neighborCosts);

    }

    public static class Generator extends Router.Generator {
        public Router createRouter(int id, NetworkInterface nic) {
            return new DistanceVectorRouter(id, nic);
        }
    }

    public static class DistanceVectorStatePacket extends Packet {
        Map<Integer, Long> costs; // Link state packet contains all of the information it has learned from its
                                  // neighbors

        public DistanceVectorStatePacket(int source, int dest, Map<Integer, Long> neighborCosts) {
            // The constructor automatically sets the payload to be the delta time
            super(source, dest, neighborCosts);
            this.costs = neighborCosts;
        }
    }

    public static class TablePacket extends Packet {
        long pingTime;

        public TablePacket(int source, int dest, int hopCount) {
            // The constructor automatically sets the payload to be the current time
            super(source, dest, hopCount);
            this.pingTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void route(Packet p) {
        Packet packet = p;
        int destination = p.dest;
        // int nextNode = routingTableIndex.get(destination);
        // nic.transmit(nextNode, packet);
    }

    @Override
    protected void findCosts() {
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        for (int i = 0; i < neighbors.size(); i++) {
            int neighbor = neighbors.get(i);
            Packet pingPacket = new PingPacket(super.nsap, neighbor, 1);
            nic.sendOnLink(i, pingPacket);
        }
        debug.println(0, "Step 0");

        //findShortestPaths();
    }

    protected void sendTables(){
        
    }

    protected void findShortestPaths(Map<Integer, Long> neighborCosts) {

        // for (int o = 0; o < 100; o++) {
        //     debug.println(0, "NeighborCosts[" + o + "]=" + neighborCosts.get(o));
        // }
       // neighborCosts.keySet().toArray();
        for(Object src: neighborCosts.keySet().toArray())
        {
            debug.println(0, "NC.get=" + neighborCosts.get(src));
        }
        /*
         * debug.println(0, "Step 0.5");// + "Link Distance" + linkDis); int source =
         * super.nsap; Map<Integer, Long> neighborCosts = super.neighborCosts;//
         * RouterId=Node, Distance=Edge
         * 
         * int ncs = neighborCosts.size(); this.routingTable.put(source, (long) 0);
         * ArrayList<Integer> links = nic.getOutgoingLinks(); for (int o = 0; o < ncs;
         * o++) { debug.println(0, "NeighborCosts[" + o + "]=" + neighborCosts.get(o));
         * } debug.println(0, "NeighborCostsSize=" + ncs);// + "Link Distance" +
         * linkDis); debug.println(0, "Link Size=" + links.size()); for (int j = 1; j <
         * ncs; j++) { debug.println(0, "Step 2"); for (int k = 0; k < links.size();
         * k++) {
         * 
         * int srcLink = links.get(k); int desLink = (int)
         * neighborCosts.keySet().toArray()[k]; debug.println(0, "Des Link:" + desLink);
         * long linkDis = neighborCosts.get(k); debug.println(0,
         * "DistanceVectorrouter: Source Link:" + srcLink + "Destination Link:" +
         * desLink + "Link Distance:" + linkDis); if (this.routingTable.get(srcLink) !=
         * Long.MAX_VALUE && this.routingTable.get(srcLink) + linkDis <
         * this.routingTable.get(desLink)) { debug.println(0, "Step 4");
         * this.routingTable.put(desLink, this.routingTable.get(srcLink) + linkDis);
         * this.routingTableIndex.put(desLink, srcLink); } else { debug.println(0,
         * "RoutingTable.get(srcLink)=" + this.routingTable.get(srcLink)); } }
         * 
         * }
         */

        // Print when packets arrives and the cost
    }

}
