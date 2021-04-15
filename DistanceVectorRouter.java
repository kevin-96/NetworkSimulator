import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/***************
 * DistanceVectorRouter Author: Christian Duncan Modified by: Represents a
 * router that uses a Distance Vector Routing algorithm.
 ***************/

public class DistanceVectorRouter extends AbstractDynamicRouter {
    Map<Integer, Long> distances; // Change to routingTable
    Map<Integer, Integer> routingTable;
    ArrayList<Map<Integer, Long>> neighborTables; // Tables that are being recieved from neighbors

    public DistanceVectorRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        distances = new HashMap<>();
        routingTable = new HashMap<>();
        int size = nic.getOutgoingLinks().size(); // number of links
        
        neighborTables = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            neighborTables.add(null);
        }


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
        for (int i = 0; i< neighborTables.size(); i++){
            int nsap = nic.getOutgoingLinks().get(i);
            Map<Integer,Long> table = neighborTables.get(i);
            if (table != null){
                for (Integer dest: table.keySet()) {
                    Long distance = table.get(dest);
                    if(tempTableDistances.get(dest) == null){
                        tempTableDistances.put(dest,neighborCosts.get(nsap)+ distance);
                        distances.put(dest,neighborCosts.get(nsap)+ distance);
                        tempTableIndex.put(dest, nsap);
                    } else if(tempTableDistances.get(dest) > distance + neighborCosts.get(nsap)){
                        tempTableDistances.put(dest,neighborCosts.get(nsap)+ distance);
                        distances.put(dest,neighborCosts.get(nsap)+ distance);
                        tempTableIndex.put(dest, nsap);
                    }
                }
            }
            System.out.println();
         }
        
        //transmit tableDistance to all neighbors
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();

        for (int i = 0; i < neighbors.size(); i++) {
            TablePacket p = new TablePacket(this.nsap, tempTableDistances);
            nic.sendOnLink(i, p);
        }
    
        //Make temp tableIndex the routingTableIndex
        this.routingTable = tempTableIndex;    
    }

}
