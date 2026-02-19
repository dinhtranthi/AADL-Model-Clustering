package org.models.xmi.json.graphModel;

import org.jgrapht.graph.DefaultEdge;

public class Connection extends DefaultEdge {


    public enum ConnectionType {
        father,
        feature,
        connection,
    }
    ConnectionType type;
    public Connection(ConnectionType type) {
        this.type = type;
    }
    public ConnectionType getType() {
        return this.type;
    }
    public int getWeight() {
        return 1;
    }

}