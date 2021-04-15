import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/***************
 * DistanceVectorRouter 
 * Authors: Brian Carballo, Ryan Clark, James Jacobson 
 * Represents a router that uses a Distance Vector Routing algorithm.
 ***************/

public class DistanceVectorRouter extends AbstractDynamicRouter {
    Map<Integer, Integer> routingTable; //Hashmap that stores routes for packet to take. Each route is stored under its destination key
    ArrayList<Map<Integer, Long>> neighborTables; // Tables that are being recieved from neighbors

    public DistanceVectorRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        //Instantiate tables
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

    // Special packet used to send tables to other nodes
    public static class TablePacket extends Packet {
        Map<Integer, Long> tableDistances;

        public TablePacket(int source, Map<Integer, Long> tableDistances) {
            super(source, -1, 1);
            this.tableDistances = tableDistances;

        }
    }

    //Handles packets not handled by AbstractDynamicRouter
    @Override
    protected void route(Packet p) {
        int source = p.source;
        //Checks if packet is Table Packet and processes it
        if (p instanceof TablePacket) {
            //Find the source of packet
            int sourceIndex = nic.getOutgoingLinks().indexOf(source);
            //Saves table from source using source NSAP as key
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
        }
    }

    //Function called periodically to update distances to other nodes
    @Override
    protected void findCosts() {
        //Grab list of immediate neighbors
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        //Send a ping packet to each neighbor to estimate distances
        for (int i = 0; i < neighbors.size(); i++) {
            int neighbor = neighbors.get(i);
            Packet pingPacket = new PingPacket(super.nsap, neighbor, 1);
            nic.sendOnLink(i, pingPacket);
        }
        //Build table based on information recieved between ping packet sending
        buildTableIndex();
    }

    protected void buildTableIndex() {
        //Create a temp empty table index to hold changes to routing table and empty distance table to hold new distances before sending
        Map<Integer,Integer> tempTableIndex = new HashMap<>(); //NSAP of destination used as key, route saved as value
        Map<Integer, Long> tempTableDistances = new HashMap<>(); //Table with distances to be sent to other nodes
        
        //Insert node with distance 0 and index -1 to represent this node
        tempTableIndex.put(this.nsap, -1);
        tempTableDistances.put(this.nsap,0L);
         
        //go through all neighbor tables to update table index and table distances 
        for (int i = 0; i< neighborTables.size(); i++){
            //Grab the NSAP of neighbor
            int nsap = nic.getOutgoingLinks().get(i);
            //Lookup neighbor table
            Map<Integer,Long> table = neighborTables.get(i);
            if (table != null){
                //For each destination saved in the table
                for (Integer dest: table.keySet()) {
                    //Save distance from neighbor to destination
                    Long distance = table.get(dest);
                    //Save new distance if destination does not yet exist in table or a faster route is found
                    if(tempTableDistances.get(dest) == null || tempTableDistances.get(dest) > distance + neighborCosts.get(nsap)){
                        tempTableDistances.put(dest,neighborCosts.get(nsap)+ distance);
                        tempTableIndex.put(dest, nsap);
                    }
                }
            }
         }
        
        //transmit tableDistance to all neighbors
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        for (int i = 0; i < neighbors.size(); i++) {
            TablePacket p = new TablePacket(this.nsap, tempTableDistances);
            nic.sendOnLink(i, p);
        }
    
        //Saves any final changes to routing table
        this.routingTable = tempTableIndex;    
    }

}
