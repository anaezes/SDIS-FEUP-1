package Peer.protocols;

import Peer.Peer;

import java.util.Iterator;

public class State {

    private final Peer peer;

    public State(Peer peer) {
        this.peer = peer;
    }

    public String getState() {

        Iterator iterator = peer.getChunkCount().keySet().iterator();

        String text = "";

        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            text += peer.toString() + peer.getChunkCount().get(key).toString();
        }

        if(text.equals(""))
            return "Problems to get STATE of this peer...";

        long capacity = peer.getFreeCapacity() + peer.getUsedCapacity();

        text += "Peer Capacity " ;
        text += "\n     Free capacity: " + peer.getFreeCapacity()/1000.0 + "KB";
        text += "\n     Used capacity: " + peer.getUsedCapacity()/1000.0 + "KB";
        text += "\n     Total Capacity: " + capacity/1000.0 + "KB";

        return text;
    }
}
