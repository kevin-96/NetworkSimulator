import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/***************
 * DistanceVectorRouter Author: Christian Duncan Modified by: Represents a
 * router that uses a Distance Vector Routing algorithm.
 ***************/

public class DistanceVectorRouter extends AbstractDynamicRouter {
    Map<Integer, Long> routingTable; // Change to distances
    Map<Integer, Integer> routingTableIndex;
    long[] distances; // Distances of each link
    ArrayList<Map<Integer, Long>> neighborTables; // Tables that are being recieved from neighbors

    public DistanceVectorRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        routingTable = new HashMap<>();
        routingTableIndex = new HashMap<>();
        int size = nic.getOutgoingLinks().size(); // number of links
        distances = new long[size];
        neighborTables = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            neighborTables.add(null);
            distances[i] = Long.MAX_VALUE;
        }
        System.out.println(super.neighborCosts);

    }

    public static class Generator extends Router.Generator {
        public Router createRouter(int id, NetworkInterface nic) {
            return new DistanceVectorRouter(id, nic);
        }
    } 

    public static class DistanceVectorStatePacket extends Packet {
        Map<Integer,Long> costs; // Link state packet contains all of the information it has learned from its neighbors
        public DistanceVectorStatePacket(int source, int dest,Map<Integer,Long> costs) {
            // The constructor automatically sets the payload to be the delta time
            super(source, dest,costs);
            this.costs = costs;
        }
    }

    @Override
    protected void route(Packet p) {

    }

    @Override
    protected void findCosts() {
        ArrayList<Integer> neighbors = nic.getOutgoingLinks();
        for (int i = 0; i < neighbors.size(); i++) {
            int neighbor = neighbors.get(i);
            Packet pingPacket = new PingPacket(super.nsap, neighbor, 1);
            nic.sendOnLink(i, pingPacket);
        }
    }

    protected void findShortestPaths() {
        int source=super.nsap;
        Map<Integer, Long> neighborCosts = super.neighborCosts;// RouterId=Node, Distance=Edge
        int ncs=neighborCosts.size();
        long distances[] = new long[ncs];
        for (int i = 0; i < distances.length; i++) {
            distances[i]=Long.MAX_VALUE;
        }
        distances[source]=0;
        ArrayList<Integer> links=nic.getOutgoingLinks();
        for(int j=1;j<ncs;j++)
        {
            for(int k=0;j<links.size();k++)
            {
                int srcLink=links.get(k);
                int desLink=(int)neighborCosts.keySet().toArray()[k];
                long linkDis=neighborCosts.get(k);
                if(distances[srcLink]!= Long.MAX_VALUE && distances[srcLink] + linkDis < distances[desLink])
                {
                    distances[desLink]=distances[srcLink] + linkDis;
                }
            }

              
        }   

//  Print when packets arrives and the cost
    }

}
