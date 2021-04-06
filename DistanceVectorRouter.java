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

    @Override
    protected void saveDistance(PongPacket pong) {
        long time=pong.pongTime/2;
        super.neighborCosts.put(pong.source,time);
        debug.println(0,"Time: " + time);
//super.neighborTables
    }

}
