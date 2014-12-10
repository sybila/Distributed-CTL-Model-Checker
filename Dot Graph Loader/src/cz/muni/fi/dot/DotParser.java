package cz.muni.fi.dot;

import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import cz.muni.fi.graph.Graph;
import cz.muni.fi.graph.Node;
import cz.muni.fi.graph.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DotParser {

    @NotNull
    public static Graph parse(@NotNull File[] inputs) throws IOException {
        //create new graph
        @NotNull Set<Integer> params = new HashSet<>();
        for (int i=0; i<inputs.length; i++) {
            params.add(i);
        }
        @NotNull Graph graph = new Graph(params);
        for (int i = 0; i < inputs.length; i++) {
            parse(graph, i, inputs[i]);
        }
        return graph;
    }

    public static void parse(@NotNull Graph graph, int color, @NotNull File input) throws IOException {

        //holds all discovered nodes
        @NotNull Map<String, Node> nodeCache = new HashMap<>();

        //pattern for classic node line
        @NotNull Pattern linePattern = Pattern.compile("\"\\[(.*-PP:[01])\\]\"->\"\\[(.*-PP:[01])\\]\"");
        //pattern for line containing pre initial node
        @NotNull Pattern preInitialPattern = Pattern.compile("\"\\[pre-initial\\]\"->\"\\[(.*-PP:[01])\\]\"");

        try(@NotNull BufferedReader br = new BufferedReader(new FileReader(input))) {    //open input file
            for(String line; (line = br.readLine()) != null; ) {    //read every line
                if (line.contains("pre-initial")) {
                    //read initial node or skip
                    @NotNull Matcher matcher = preInitialPattern.matcher(line);
                    if (!matcher.matches()) continue;
                    String id = matcher.group(1);
                    Node dest = nodeCache.get(id);
                    if (dest == null) {
                        dest = stringToNode(graph, id);
                        nodeCache.put(id, dest);
                    }
                    graph.addInitialNode(dest);
                } else {
                    @NotNull Matcher matcher = linePattern.matcher(line);
                    if (matcher.matches()) {
                        String sourceId = matcher.group(1);
                        String destId = matcher.group(2);
                        Node source = nodeCache.get(sourceId);
                        Node dest = nodeCache.get(destId);
                        if (source == null) {
                            source = stringToNode(graph, sourceId);
                            nodeCache.put(sourceId, source);
                            graph.addInitialNode(source);
                        }
                        if (dest == null) {
                            dest = stringToNode(graph, destId);
                            nodeCache.put(destId, dest);
                        }
                        @Nullable Path path = null;
                        for (@NotNull Path p : source.getAfter()) {
                            if (p.getTo().equals(dest)) {
                                path = p;
                            }
                        }
                        if (path == null) {
                            path = new Path(source, dest);
                        }
                        path.addColor(color);
                        //dest.addBefore(path);
                        source.addAfter(path);
                    }
                }
            }
        }
        //search graph for loops to determine terminal nodes
        @NotNull Set<Node> discovered = new HashSet<>();
        for (Node init : graph.getInitialNodes()) {
            discover(init, graph, discovered);
        }
    }

    private static void discover(Node init, @NotNull Graph graph, @NotNull Set<Node> discovered) {
        @NotNull Queue<Node> queue = new LinkedList<>();
        queue.add(init);
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            if (discovered.contains(node)) {
                boolean all = true;
                for (@NotNull Path path : node.getAfter()) {
                    if (!discovered.contains(path.getTo())) {
                        all = false;
                    }
                }
                if (all) {
                    graph.addTerminalNode(node);
                }
            } else {
                discovered.add(node);
                for (@NotNull Path path : node.getAfter()) {
                    queue.add(path.getTo());
                }
            }
        }
    }

    @NotNull
    private static Node stringToNode(@NotNull Graph graph, @NotNull String string) {
        @NotNull Matcher matcher = Pattern.compile("([.0-9]+)\\(\\d+\\),([.0-9]+)\\(\\d+\\),([.0-9]+)\\(\\d+\\),([.0-9]+)\\(\\d+\\)-PP:([01])").matcher(string);
        if (!matcher.matches()) {
            throw new IllegalStateException("Invalid node string: "+string);
        }
        @NotNull Map<String, Float> values = new HashMap<>();
        values.put("a", Float.parseFloat(matcher.group(1)));
        values.put("b", Float.parseFloat(matcher.group(2)));
        values.put("c", Float.parseFloat(matcher.group(3)));
        values.put("d", Float.parseFloat(matcher.group(4)));
        values.put("e", Float.parseFloat(matcher.group(5)));

        @NotNull Node node = new Node(values, string);
        //Set<Integer> colors = new HashSet<>();
        //colors.add(0);
        for (@NotNull Map.Entry<String, Float> pair : values.entrySet()) {
            node.addFormula(new FloatProposition(pair.getValue(), pair.getKey(), FloatProposition.Operator.EQ), graph.getColors());
        }
        return node;
    }
}
