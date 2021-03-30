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
        }
    }

    public static class PongPacket extends Packet {
        public PongPacket(int source, int dest, int hopCount, long pingTime) {
            // The constructor automatically sets the payload to be the delta time
            super(source, dest, hopCount, System.currentTimeMillis() - pingTime);
        }
    }

    Debug debug;
    Map<Integer, Long> costs; // Stores the cost metric of each router's neighbors
    
    public LinkStateRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        debug = Debug.getInstance();  // For debugging!
        costs = new HashMap<>();
    }

    private void findCosts() {
        for (int neighbor : nic.getOutgoingLinks()) {
            Packet pingPacket = new PingPacket(this.nsap, neighbor, 1);
            nic.transmit(neighbor, pingPacket);
        }
    }

    /* TODO: Construct the Link State Packet
    Packet includes:
    - Identity of sender
    - Sequence number (32-bit) and age (decrement age after each second)
    - List of neighbors
    */

    // Determining when the packet is constructed (Important)
    


    public void run() {
        // TODO: call this method at a regular interval instead
        findCosts();

        while (true) {
            // See if there is anything to process
            boolean process = false;
            NetworkInterface.TransmitPair toSend = nic.getTransmit();
            if (toSend != null) {
                // There is something to send out
                process = true;
                debug.println(3, "(LinkStateRouter.run): I am being asked to transmit: " + toSend.data + " to the destination: " + toSend.destination);

                // TODO: this is a placeholder until we really figure out routing
                if (toSend.data instanceof PingPacket || toSend.data instanceof PongPacket) {
                    int dest = ((Packet) toSend.data).dest;
                    int link = nic.getOutgoingLinks().indexOf(dest);
                    nic.sendOnLink(link, toSend.data);
                }
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
                    long pingTime = (long) packet.payload;
                    PongPacket pong = new PongPacket(this.nsap, source, 1, pingTime);
                    nic.transmit(source, pong);
                } else if (toRoute.data instanceof PongPacket) {
                    debug.println(4, "Received a PongPacket");
                    // If we receive a pong packet, use it to store the cost we previously requested
                    PongPacket packet = (PongPacket) toRoute.data;
                    int source = packet.source;
                    long cost = (long) packet.payload;
                    costs.put(source, cost);
                    debug.println(5, "Cost(" + this.nsap + ", " + source + ") = " + cost);
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
