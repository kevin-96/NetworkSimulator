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
        Map<Integer, Long> tableDistances;

        public TablePacket(int source, Map<Integer, Long> tableDistances) {
            super(source, -1, 1);
            this.tableDistances = tableDistances;

        }
    }

    @Override
    protected void route(Packet p) {
        // Packet packet = p;
        int destination = p.dest;
        int source = p.source;
        if (p instanceof TablePacket) {
            int sourceIndex = nic.getOutgoingLinks().indexOf(source);
            neighborTables.set(sourceIndex, ((TablePacket) p).tableDistances);
        } else {
            // Send packet along
        }

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
        // findShortestPaths(super.neighborCosts);
        buildTableIndex();

        // findShortestPaths();
    }

    protected void sendTables() {

    }
    protected void buildTableIndex() {
        //create a temp empty table index and a empty table distance

        Map<Integer,Integer> tempTableIndex = new HashMap<>(); //NSAP to index, use for routing
        Map<Integer, Long> tempTableDistances = new HashMap<>(); //Table with distances to be sent to other nodes
        
        //Insert ur own node with distance 0 and index -1 
        tempTableIndex.put(this.nsap, -1);
        tempTableDistances.put(this.nsap,0L);
         
        //go through all neighbor tables to update table index and table dis 
        if(this.nsap == 14){
         for (int i = 0; i< neighborTables.size(); i++){
            int nsap = nic.getOutgoingLinks().get(i);
            System.out.print("Source:" + nsap + " :");
            Map<Integer,Long> table = neighborTables.get(i);
            if (table != null){
                for (Integer dest: table.keySet()) {
                    Long distance = table.get(dest);
                    System.out.print(dest + "," + distance + " ");
                }
            }
            System.out.println();
         }
        }
        
        //transmit tableDistance to all neighbors
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();

        for (int i = 0; i < neighbors.size(); i++) {
            TablePacket p = new TablePacket(this.nsap, tempTableDistances);
            nic.sendOnLink(i, p);
        }
    
        //Make temp tableIndex the routingTableIndex
        this.routingTableIndex = tempTableIndex;    
    }
   /* protected void buildTableIndex() {
        // create a temp empty table index and a empty table distance

        Map<Integer, Long> neighborCosts = super.neighborCosts;
        Map<Integer, Integer> tempTableIndex = new HashMap<>(); // NSAP to index, use for routing
        Map<Integer, Long> tempTableDistances = new HashMap<>(); // Table with distances to be sent to other nodes

        // Insert ur own node with distance 0 and index -1
        tempTableIndex.put(this.nsap, -1);
        tempTableDistances.put(this.nsap, 0L);

        // go through all neighbor tables to update table index and table dis

        for (int i = 0; i < neighborTables.size(); i++) {
            int nsap = nic.getOutgoingLinks().get(i);
            Map<Integer, Long> table = neighborTables.get(i);//Is always null
            if (table != null) {
                for (Integer dest : table.keySet()) {
                    Long distance = table.get(dest);
                    System.out.println(":|");
                }
            }
            System.out.println();
        }

        System.out.println("^-------");
        for (int i = 0; i < neighborTables.size(); i++) {
            int nsap = nic.getOutgoingLinks().get(i);
            System.out.println("Source:" + nsap + " :");
            Map<Integer, Long> table = neighborTables.get(i);
            if(table==null)
            {
                System.out.println(":(");
            }
            if (table != null) {
                System.out.println(":)");
                for (Integer dest : table.keySet()) {
                    if (table.get(dest) == null) {
                        table.put(dest, Long.MAX_VALUE);
                    }
                    if (table.get(dest) != Long.MAX_VALUE
                            && nic.getOutgoingLinks().get(i) + neighborCosts.get(dest) < table.get(dest)) {
                                table.put(dest, nic.getOutgoingLinks().get(i) + neighborCosts.get(dest) );
                                System.out.println(":))");
                    }

                }
            }
            tempTableDistances=table;
            System.out.println();
        }
        System.out.println(">--------");
        /*
         * /*
         * 
         * Get the costs and we need to put the neighbor ID and the distance into the
         * tempTableIndex
         
        // transmit tableDistance to all neighbors

        ArrayList<Integer> neighbors = nic.getOutgoingLinks();

        for (int i = 0; i < neighbors.size(); i++) {
            TablePacket p = new TablePacket(this.nsap, tempTableDistances);
            nic.sendOnLink(i, p);
        }

        // Make temp tableIndex the routingTableIndex
        this.routingTableIndex = tempTableIndex;
    }
    */
    protected void findShortestPaths(Map<Integer, Long> neighborCosts) {

        // for (int o = 0; o < 100; o++) {
        // debug.println(0, "NeighborCosts[" + o + "]=" + neighborCosts.get(o));
        // }
        for (Object src : neighborCosts.keySet().toArray()) {
            debug.println(0, "NC.get=" + neighborCosts.get(src) + " From Source: " + (int) src);
        }
        for (int i = 0; i < nic.getOutgoingLinks().size(); i++) {
            debug.println(0, "getOutGoingLinks[i]=" + nic.getOutgoingLinks().get(i));
        }

        // debug.println(0, "Step 0.5");// + "Link Distance" + linkDis);
        // int source = super.nsap;
        // int ncs = neighborCosts.size();
        // this.routingTable.put(source, (long) 0);
        // ArrayList<Integer> links = nic.getOutgoingLinks();
        // debug.println(0, "NeighborCostsSize=" + ncs);// + "Link Distance" + linkDis);
        // debug.println(0, "Link Size=" + links.size());
        // for (int j = 1; j < ncs; j++) {
        // for (int k = 0; k < links.size(); k++) {
        // int srcLink = links.get(k);
        // int desLink = (int) neighborCosts.keySet().toArray()[k];
        // debug.println(0, "Des Link:" + desLink);
        // long linkDis = neighborCosts.get(k);
        // debug.println(0, "DistanceVectorrouter: Source Link:" + srcLink +
        // "Destination Link:" + desLink
        // + "Link Distance:" + linkDis);
        // if (this.routingTable.get(srcLink) != Long.MAX_VALUE
        // && this.routingTable.get(srcLink) + linkDis < this.routingTable.get(desLink))
        // {
        // debug.println(0, "Step 4");
        // this.routingTable.put(desLink, this.routingTable.get(srcLink) + linkDis);
        // this.routingTableIndex.put(desLink, srcLink);
        // } else {
        // debug.println(0, "RoutingTable.get(srcLink)=" +
        // this.routingTable.get(srcLink));
        // }
        // }

        // }

        // Print when packets arrives and the cost

    }

}
