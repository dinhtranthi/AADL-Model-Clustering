package org.models.xmi.json.graphModel;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Node {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    public Node(Node node) {
        this.id = node.id;
        this.name = node.name;
        this.category = node.category;
        this.type = node.type;
    }

    public Node(Category category) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = category.toString();
        this.category = category;
        this.type = setTypeFromCategory(category);
    }

    public void setType(Type feature) {
        this.type = feature;
    }

    public enum Category {
        process,
        thread,
        thread_group,
        data,
        subprogram,
        subprogram_group,
        processor,
        memory,
        bus,
        device,
        system,
        virtual_bus,
        virtual_processor,
        busAccess,
        dataPort,
        dataPortIn,
        dataPortOut,
        eventPort,
        eventDataPort,
        featureGroup,
        dataAccess,
        abstractFeature,
        subprogramAccess,
        subprogramGroupAccess,
        unknown;

        public static int compare(Category category, Category category1) {
            return category.compareTo(category1);
        }
    }

    public enum Type {
        software,
        hardware,
        system,
        feature,
        unknown
    }

    private final int id;
    private String name;
    private Category category;
    private Type type;

    private static final Logger logger = Logger.getLogger(Node.class.getName());

    // Constructor
    public Node(String name, Category category) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = name + " (" + id + ")";
        this.category = category;
        this.type = setTypeFromCategory(category);
    }

    private void setTypeFromCategory() {
        switch (category) {
            case process:
            case virtual_processor:
            case thread:
            case thread_group:
            case data:
            case subprogram:
            case subprogram_group:
                this.type = Type.software;
                break;
            case processor:
            case memory:
            case bus:
            case device:
            case virtual_bus:
                this.type = Type.hardware;
                break;
            case system:
                this.type = Type.system;
                break;
            case busAccess:
            case dataPortIn:
            case dataPortOut:
            case dataPort:
            case eventPort:
            case eventDataPort:
            case featureGroup:
            case dataAccess:
            case abstractFeature:
            case subprogramAccess:
            case subprogramGroupAccess:
                this.type = Type.feature;
                break;
            default:
                this.type = Type.unknown;
        }
    }
    private Type setTypeFromCategory(Category category) {
        Type type;
        switch (category) {
            case process:
            case virtual_processor:
            case thread:
            case thread_group:
            case data:
            case subprogram:
            case subprogram_group:
                type = Type.software;
                break;
            case processor:
            case memory:
            case bus:
            case device:
            case virtual_bus:
                type = Type.hardware;
                break;
            case system:
                type = Type.system;
                break;
            case busAccess:
            case dataPortIn:
            case dataPortOut:
            case dataPort:
            case eventPort:
            case eventDataPort:
            case featureGroup:
            case dataAccess:
            case abstractFeature:
            case subprogramAccess:
            case subprogramGroupAccess:
                type = Type.feature;
                break;
            default:
                type = Type.unknown;
        }
        return type;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
        setTypeFromCategory();
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", type=" + type +
                '}';
    }

    public int compare(Node o1, Node o2) {
        return Category.compare(o1.getCategory(), o2.getCategory());


    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return id == node.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Type getType() {
        return type;
    }


}
