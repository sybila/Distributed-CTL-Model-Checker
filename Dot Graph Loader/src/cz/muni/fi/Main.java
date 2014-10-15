package cz.muni.fi;

import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.dot.DotParser;
import cz.muni.fi.graph.Graph;
import cz.muni.fi.graph.Node;
import cz.muni.fi.graph.ParameterSet;
import cz.muni.fi.graph.Path;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by daemontus on 23/09/14.
 */
public class Main {

    public static void main(String[] args) throws IOException {
       /* File[] input = new File[] {new File("/Users/daemontus/Downloads/tcbb1.bio.prop1.bio.dot"), new File("/Users/daemontus/Downloads/tcbb1.bio.prop1.bio.dot")};
        Graph graph = DotParser.parse(input);
        //System.out.println("Count: "+graph.getTerminalNodes().size()+" End: "+ Arrays.toString(graph.getTerminalNodes().toArray()));
        //System.out.println("End: "+ Arrays.toString(graph.getInitialNodes().toArray()));
        for (Node node : graph.getInitialNodes()) {
            printRecursive(node, 5);
        }*/
        ParameterSet.limit = 20;
        ArrayList<Integer> a = new ArrayList<>();
        a.add(1);
        a.add(1);
        a.add(3);
        a.add(3);
        a.add(5);
        a.add(6);
        a.add(11);
        a.add(11);
        a.add(13);
        a.add(15);
        ArrayList<Integer> b = new ArrayList<>();
        b.add(2);
        b.add(5);
        ParameterSet A = new ParameterSet(a);
        ParameterSet B = new ParameterSet(b);
        B.union(A);
        Log.d(B.toString());
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
