/***************
 * LinkStateRouter
 * Author: Christian Duncan
 * Modified by: 
 * Represents a router that uses a Link State Routing algorithm.
 ***************/
import java.util.ArrayList;

public class LinkStateRouter extends Router {
    // A generator for the given LinkStateRouter class
    public static class Generator extends Router.Generator {
        public Router createRouter(int id, NetworkInterface nic) {
            return new LinkStateRouter(id, nic);
        }
    }

    Debug debug;
    Map<Integer, Long> costs; // Stores the cost metric of each router's neighbors
    
    public LinkStateRouter(int nsap, NetworkInterface nic) {
        super(nsap, nic);
        debug = Debug.getInstance();  // For debugging!
        costs = new HashMap<>();
    }

    private Map<Integer, Long> costs;

    private void findCosts() {
        for (int id : nic.getOutgoingLinks()) {
            long start = System.currentTimeMillis();
            // TODO: Send packet and wait for response
            long end = System.currentTimeMillis();
            costs.put(id, end - start);
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
        while (true) {
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
                debug.println(3, "(LinkStateRouter.run): I am being asked to transmit: " + toSend.data + " to the destination: " + toSend.destination);
            }

            if (!process) {
                // Didn't do anything, so sleep a bit
                try { Thread.sleep(1); } catch (InterruptedException e) { }
            }
        }
    }
}
