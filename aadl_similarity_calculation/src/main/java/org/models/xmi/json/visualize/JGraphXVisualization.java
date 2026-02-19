package org.models.xmi.json.visualize;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.swing.mxGraphComponent;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.models.xmi.json.graphModel.Connection;
import org.models.xmi.json.graphModel.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class JGraphXVisualization extends JApplet {
    private static final long serialVersionUID = 2202072534703043194L;
    private static final Dimension DEFAULT_SIZE = new Dimension(530, 320);
    private JGraphXAdapter<String, DefaultEdge> jgxAdapter;
    private static final HashMap<DefaultEdge, Connection> defaultEdgeToConnectionMap = new HashMap<>();

    public enum LayoutType {
        CIRCLE, HIERARCHICAL
    }
    LayoutType layoutType = LayoutType.HIERARCHICAL;

    public static void VisualizeGraph(Graph<Node, Connection> g, String filename) {
        JGraphXVisualization applet = new JGraphXVisualization();
        Graph<String, DefaultEdge> g1 = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (Node node : g.vertexSet()) {
            g1.addVertex(node.getName() + " (" + node.getCategory() + ")");
        }
        for (Connection edge : g.edgeSet()) {
            String g1Name = g.getEdgeSource(edge).getName() + " (" + g.getEdgeSource(edge).getCategory() + ")";
            String g2Name = g.getEdgeTarget(edge).getName() + " (" + g.getEdgeTarget(edge).getCategory() + ")";
            DefaultEdge e = g1.addEdge(g1Name, g2Name);
            defaultEdgeToConnectionMap.put(e, edge);
        }

        applet.init(g1, defaultEdgeToConnectionMap);
        JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(applet, BorderLayout.CENTER);
        frame.getContentPane().add(createLegendPanel(), BorderLayout.SOUTH);
        frame.setTitle(filename);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void init(Graph<String, DefaultEdge> g, HashMap<DefaultEdge, Connection> edgeToConnectionMap) {
        jgxAdapter = new JGraphXAdapter<>(g);
        jgxAdapter.getEdgeToCellMap().forEach((edge, cell) -> cell.setValue(null));

        setPreferredSize(DEFAULT_SIZE);
        mxGraphComponent component = new mxGraphComponent(jgxAdapter);
        component.setConnectable(false);
        component.getGraph().setAllowDanglingEdges(false);
        getContentPane().add(component);
        resize(DEFAULT_SIZE);

        switch (layoutType) {
            case CIRCLE -> {
                mxCircleLayout layout = new mxCircleLayout(jgxAdapter);
                int radius = 100;
                layout.setX0((DEFAULT_SIZE.width / 2.0) - radius);
                layout.setY0((DEFAULT_SIZE.height / 2.0) - radius);
                layout.setRadius(radius);
                layout.setMoveCircle(true);
                layout.execute(jgxAdapter.getDefaultParent());
            }
            case HIERARCHICAL -> {
                mxHierarchicalLayout layout = new mxHierarchicalLayout(jgxAdapter);
                layout.setUseBoundingBox(false);
                layout.setInterHierarchySpacing(10);
                layout.setInterRankCellSpacing(200);
                layout.execute(jgxAdapter.getDefaultParent());
            }
        }

        jgxAdapter.refresh();

        for (Map.Entry<DefaultEdge, com.mxgraph.model.mxICell> entry : jgxAdapter.getEdgeToCellMap().entrySet()) {
            com.mxgraph.model.mxICell cell = entry.getValue();
            Connection connection = edgeToConnectionMap.get(entry.getKey());
            if (connection.getType() == Connection.ConnectionType.father) {
                jgxAdapter.setCellStyle("strokeColor=red", new Object[]{cell});
            } else if (connection.getType() == Connection.ConnectionType.feature) {
                jgxAdapter.setCellStyle("strokeColor=green", new Object[]{cell});
            }
        }

        addInteractivity(component, edgeToConnectionMap);
    }

    private static JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Legend:");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        legendPanel.add(title);

        legendPanel.add(createLegendItem(Color.RED, "Father"));
        legendPanel.add(createLegendItem(Color.GREEN, "Feature"));
        legendPanel.add(createLegendItem(Color.BLUE, "Connection"));

        return legendPanel;
    }

    private static JPanel createLegendItem(Color color, String description) {
        JPanel legendItem = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel colorLabel = new JLabel(" ");
        colorLabel.setOpaque(true);
        colorLabel.setBackground(color);
        colorLabel.setPreferredSize(new Dimension(15, 15));
        JLabel descriptionLabel = new JLabel(description);
        legendItem.add(colorLabel);
        legendItem.add(descriptionLabel);
        return legendItem;
    }

    private void addInteractivity(mxGraphComponent component, HashMap<DefaultEdge, Connection> edgeToConnectionMap) {
        component.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Object cell = component.getCellAt(e.getX(), e.getY());
                if (cell != null) {
                    if (jgxAdapter.getModel().isVertex(cell)) {
                        String vertexName = (String) jgxAdapter.getModel().getValue(cell);
                        JOptionPane.showMessageDialog(component, "Node: " + vertexName);
                    } else if (jgxAdapter.getModel().isEdge(cell)) {
                        DefaultEdge edge = (DefaultEdge) jgxAdapter.getModel().getValue(cell);
                        Connection connection = edgeToConnectionMap.get(edge);


                    }
                }
            }
        });

        component.getGraphControl().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Object cell = component.getCellAt(e.getX(), e.getY());
                if (cell != null) {
                    if (jgxAdapter.getModel().isVertex(cell)) {
                        String vertexName = (String) jgxAdapter.getModel().getValue(cell);
                        component.setToolTipText("Node: " + vertexName);
                    } else if (jgxAdapter.getModel().isEdge(cell)) {
                        DefaultEdge edge = (DefaultEdge) jgxAdapter.getModel().getValue(cell);
                        Connection connection = edgeToConnectionMap.get(edge);

                    }
                } else {
                    component.setToolTipText(null);
                }
            }
        });
    }
}
