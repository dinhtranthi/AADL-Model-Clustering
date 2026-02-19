package org.models.xmi.json.graphModel;

import org.jgrapht.Graph;

import java.util.Objects;

public class Model {
    private String name;
    private Graph<Node, Connection> g;

    public Model(String name, Graph<Node, Connection> g) {
        this.name = name;
        this.g = g;
    }

    @Override
    public String toString() {
        return "Model{" +
                "name='" + name + '\'' +
                ", g=" + g +
                '}';
    }

    public String getName() {
        return name;
    }

    public Graph<Node, Connection> getGraph() {
        return g;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Model model = (Model) o;
        return Objects.equals(name, model.name) && Objects.equals(g, model.g);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, g);
    }
}
