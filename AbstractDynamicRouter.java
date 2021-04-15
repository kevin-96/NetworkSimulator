
/***************
 * Abstract Dynamic Router
 * Author: Joey Germain, Phillip Nam, Kevin Sangurima, Brian Carballo, James Jacobson, Ryan Clark
 * An abstract class that represents a dynamic router
 ***************/
import java.util.Map;
import java.util.HashMap;

public abstract class AbstractDynamicRouter extends Router {
    protected static final int DEFAULT_HOP_COUNT = 5;

    public static class Packet {
        // This is how we will store our Packet Header information
        int source;
        int dest;
        int hopCount; // Maximum hops to get there
        Object payload; // The payload!
        int sourceVD;
        int destVD;
        Map<Integer, Long> costs;

        public Packet(int source, int dest, int hopCount) {
            this(source, dest, hopCount, null);
        }

        public Packet(int source, int dest, int hopCount, Object payload) {
            this.source = source;
            this.dest = dest;
            this.hopCount = hopCount;
            this.payload = payload;
        }

        public Packet(int sourceVD, int destVD, Map<Integer, Long> costs) {
            this.sourceVD = sourceVD;
            this.destVD = destVD;
            this.costs = costs;

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

    // Packet class that contains the table distances
    public static class TablePacket extends Packet {
        Map<Integer, Long> tableDistances;

        public TablePacket(int source, Map<Integer, Long> tableDistances) {
            // The constructor automatically sets the payload to be the current time
            super(source, -1, 1);
            this.tableDistances = tableDistances;
        }
    }

    Debug debug; // For debugging
    Map<Integer, Long> neighborCosts; // Stores the costs of each router's neighbors (Between neighbors)

    public AbstractDynamicRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        debug = Debug.getInstance(); // For debugging!
        neighborCosts = new HashMap<>(); // Each router knows the costs of its neighbors
    }

    protected abstract void route(Packet p);
    protected abstract void findCosts();

    // Time in ms in between finding costs/shortest paths again
    int costDelay = 10000;

    public void run() {
        long nextFindCost = System.currentTimeMillis() + 1000; // Initially it will wait 1 sec before finging costs
        while (true) {
            // Piece of code in charge of running findCost() every costDelay mseconds
            if (System.currentTimeMillis() > nextFindCost) {
                // System.out.println("finding costs");
                nextFindCost = System.currentTimeMillis() + costDelay;
                findCosts();
            }
            // See if there is anything to process
            boolean process = false;
            NetworkInterface.TransmitPair toSend = nic.getTransmit();
            if (toSend != null) {
                // There is something to send out
                process = true;
                debug.println(3, "(AbstractDynamicRouter.run): I am being asked to transmit: " + toSend.data
                        + " to the destination: " + toSend.destination);
                // Create new packet and routes it
                Packet packet = new Packet(nsap, toSend.destination, DEFAULT_HOP_COUNT, toSend.data);
                route(packet);
            }

            NetworkInterface.ReceivePair toRoute = nic.getReceived();
            if (toRoute != null) {
                // There is something to route through - or it might have arrived at destination
                process = true;
                debug.println(3, "(AbstractDynamicRouter.run): I received: " + toRoute.data + " from source: "
                        + toRoute.originator);

                if (toRoute.data instanceof PingPacket) {
                    debug.println(4, "Received a PingPacket");
                    // If we receive a ping packet, respond with a pong
                    PingPacket packet = (PingPacket) toRoute.data;
                    int source = packet.source;
                    long pingTime = packet.pingTime;
                    PongPacket pong = new PongPacket(this.nsap, source, 1, pingTime);
                    nic.sendOnLink(nic.getOutgoingLinks().indexOf(source), pong);
                } else if (toRoute.data instanceof PongPacket) {
                    debug.println(4, "Received a PongPacket");
                    // If we receive a pong packet, use it to store the cost we previously requested
                    PongPacket packet = (PongPacket) toRoute.data;
                    int source = packet.source; // Source of the packet is the destination of the ping packet
                    long cost = packet.pongTime;
                    neighborCosts.put(source, cost); // Stores the cost/link in the neighborCosts map
                    // adds this step to the debug console
                    debug.println(5, "Cost(" + this.nsap + ", " + source + ") = " + cost);
                } else if (toRoute.data instanceof TablePacket) {

                } else if (toRoute.data instanceof Packet) {
                    // Routing something other than ping/pong is dependent on which algorithm is
                    // used
                    Packet packet = (Packet) toRoute.data;
                    // Reduce the hop count by one
                    packet.hopCount--;
                    if (packet.hopCount >= 0) {
                        route(packet);
                    } else {
                        debug.println(4, "Too many hops!");
                    }
                } else {
                    debug.println(4, "Tried to route something that wasn't a packet");
                }
            }

            if (!process) {
                // Didn't do anything, so sleep a bit
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
