/***************
 * DistanceVectorRouter
 * Author: Christian Duncan
 * Modified by: 
 * Represents a router that uses a Distance Vector Routing algorithm.
 ***************/
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public class DistanceVectorRouter extends Router {
    // A generator for the given DistanceVectorRouter class
    public static class Generator extends Router.Generator {
        public Router createRouter(int id, NetworkInterface nic) {
            return new DistanceVectorRouter(id, nic);
        }
    }

    Map<Integer, Long> routingTable;
    Debug debug;
    public static class Packet {
        // This is how we will store our Packet Header information
        int source;
        int dest;
        int hopCount;  // Maximum hops to get there
        Object payload;  // The payload!
        
        public Packet(int source, int dest, int hopCount, Object payload) {
            this.source = source;
            this.dest = dest;
            this.hopCount = hopCount;
            this.payload = payload;
        }
    }
    public static class PingPacket extends Packet {
        public PingPacket(int source, int dest, int hopCount) {
            // The constructor automatically sets the payload to be the current time
            super(source, dest, hopCount, System.currentTimeMillis());
            // neighbors = new ArrayList<int[]>();
        }
    }
    public static class PongPacket extends Packet {
        public PongPacket(int source, int dest, int hopCount, long pingTime) {
            // The constructor automatically sets the payload to be the delta time
            super(source, dest, hopCount, System.currentTimeMillis() - pingTime);
        }
    }
    
    public DistanceVectorRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        debug = Debug.getInstance();  // For debugging!
    }

    public void DetermineDistances(){
        for (int neighbor : nic.getOutgoingLinks()) {
           routingTable.put(neighbor, Long.MAX_VALUE);
           Packet pingPacket = new PingPacket(this.nsap, neighbor, 1);
           nic.transmit(neighbor, pingPacket);
        }
    }

    private void route(Packet p){
        Packet packet = p;
        int destination = p.dest;
        
    }

    public void run() {
        routingTable = new HashMap<>();

        
        while (true) {
            // See if there is anything to process
            boolean process = false;
            NetworkInterface.TransmitPair toSend = nic.getTransmit();
            if (toSend != null) {
                // There is something to send out
                process = true;
                debug.println(3, "(DistanceVectorRouter.run): I am being asked to transmit: " + toSend.data + " to the destination: " + toSend.destination);
            }

            NetworkInterface.ReceivePair toRoute = nic.getReceived();
            if (toRoute != null) {
                // There is something to route through - or it might have arrived at destination
                process = true;
                if (toRoute.data instanceof Packet) {
                    Packet p = (Packet) toRoute.data;
                    if (p.dest == nsap) {
                        // It made it!  Inform the "network" for statistics tracking purposes
                        debug.println(4, "(DistanceVectorRouter.run): Packet has arrived!  Reporting to the NIC - for accounting purposes!");
                        debug.println(6, "(DistanceVectorRouter.run): Payload: " + p.payload);
                        nic.trackArrivals(p.payload);
                    } else if (p.hopCount > 0) {
                        // Still more routing to do
                        p.hopCount--;
                        //route(toRoute.originator, p);
                    } else {
                        debug.println(5, "Packet has too many hops.  Dropping packet from " + p.source + " to " + p.dest + " by router " + nsap);
                    }
                } else {
                    debug.println(0, "Error.  The packet being tranmitted is not a recognized Flood Packet.  Not processing");
                }
            }

            if (!process) {
                // Didn't do anything, so sleep a bit
                try { Thread.sleep(1); } catch (InterruptedException e) { }
            }
        }
    }
}
