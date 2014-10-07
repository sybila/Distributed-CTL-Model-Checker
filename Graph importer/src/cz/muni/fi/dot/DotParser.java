package cz.muni.fi.dot;

import cz.muni.fi.ctl.formula.proposition.ValueEquals;
import cz.muni.fi.graph.Graph;
import cz.muni.fi.graph.Node;
import cz.muni.fi.graph.Path;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DotParser {

    public static Graph parse(File input) throws IOException {
        HashMap<String, Node> nodeCache = new HashMap<>();
        Graph graph = null;
        Node preInitial = new Node(null);
        nodeCache.put("pre-initial", preInitial);
        graph = new Graph(preInitial);
        Pattern linePattern = Pattern.compile("\"\\[(.*)-PP:[01]\\]\"->\"\\[(.*)-PP:[01]\\]\"");
        Pattern preInitialPattern = Pattern.compile("\"\\[pre-initial\\]\"->\"\\[(.*)-PP:[01]\\]\"");
        try(BufferedReader br = new BufferedReader(new FileReader(input))) {
            for(String line; (line = br.readLine()) != null; ) {
                if (line.contains("pre-initial")) {
                    Matcher matcher = preInitialPattern.matcher(line);
                    if (!matcher.matches()) continue;
                    String d = matcher.group(1);
                    Node dest = nodeCache.get(d);
                    if (dest == null) {
                        dest = stringToNode(d);
                        nodeCache.put(d, dest);
                    }
                    Path p = new Path(preInitial, dest, 0);
                    dest.addBefore(p);
                    preInitial.addAfter(p);
                } else {
                    Matcher matcher = linePattern.matcher(line);
                    if (matcher.matches()) {
                        String s = matcher.group(1);
                        String d = matcher.group(2);
                        Node source = nodeCache.get(s);
                        Node dest = nodeCache.get(d);
                        if (source == null) {
                            source = stringToNode(s);
                            nodeCache.put(s, source);
                        }
                        if (dest == null) {
                            dest = stringToNode(d);
                            nodeCache.put(d, dest);
                        }
                        if (nodeCache.containsValue(dest)) {
                            graph.addEnd(source);
                            //continue;
                        }
                        //System.out.println("S: "+source+" D: "+dest);
                        Path p = new Path(source, dest, 0);
                        dest.addBefore(p);
                        source.addAfter(p);
                    }
                }
            }
        }
        /*for (Node node : nodeCache.values()) {
            if (node.getAfter().size() == 0) {
                graph.addEnd(node);
            }
        }*/
        return graph;
    }

    private static Node stringToNode(String string) {
        Matcher matcher = Pattern.compile("([.0-9]+)\\(\\d+\\),([.0-9]+)\\(\\d+\\)").matcher(string);
        if (!matcher.matches()) {
            throw new IllegalStateException("Invalid node string: "+string);
        }
        float a = Float.parseFloat(matcher.group(1));
        float b = Float.parseFloat(matcher.group(2));
        Node n = new Node(new float[]{a, b});
        List<Integer> colors = new ArrayList<>();
        colors.add(0);
        n.correct.put(new ValueEquals(0, a), colors);
        n.correct.put(new ValueEquals(1, b), colors);
        //System.out.println("New node "+n.correct.size());
        return n;
    }
}
