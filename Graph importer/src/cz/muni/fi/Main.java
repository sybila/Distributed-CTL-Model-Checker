package cz.muni.fi;

import cz.muni.fi.dot.DotParser;
import cz.muni.fi.graph.Graph;
import cz.muni.fi.graph.Node;
import cz.muni.fi.graph.Path;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by daemontus on 23/09/14.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        Graph graph = DotParser.parse(new File("/Users/daemontus/Downloads/tcbb1.bio.prop1.bio.dot"));
        System.out.println("End: "+ Arrays.toString(graph.endNodes().toArray()));
    }

    private static void printRecursive(Node node, int k) {
        if (k<=1) return;
        System.out.println(node.toString());
        for (Path path : node.getAfter()) {
            Node n = path.getTo();
            printRecursive(n, k-1);
        }
    }

}
