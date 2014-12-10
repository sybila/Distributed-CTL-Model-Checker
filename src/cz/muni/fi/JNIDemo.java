package cz.muni.fi;

import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.distributed.graph.Graph;
import cz.muni.fi.distributed.graph.Node;
import cz.muni.fi.model.ColorSet;
import cz.muni.fi.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.StringTokenizer;

public class JNIDemo {

    public static void main(String[] args) {
        @NotNull FloatProposition proposition = new FloatProposition(10f, "pRB", FloatProposition.Operator.LT_EQ);
        @NotNull Model model = new Model("/Users/daemontus/Synced/Java Development/Parametrized CTL Model Checker/native/examples/tcbb1param.bio");
        model.load();
        @NotNull Graph graph = new Graph(model, 1, 0);
        @NotNull Map<Node, ColorSet> nodeSet = graph.factory.getAllValidNodes(proposition);
        Log.d("Nodes: "+nodeSet.size());
        for (@NotNull Map.Entry<Node, ColorSet> entry: nodeSet.entrySet()) {
            /*Log.d("Node "+entry.getKey().coordinates[0]+" "+entry.getKey().coordinates[1]);
            if (entry.getKey().coordinates[1] < 5) {
                Map<Node, ColorSet> successors = graph.factory.computePredecessors(entry.getKey(), entry.getValue());
                Log.d(" Predecessors: "+successors.size());
            }*/
            printSucc(entry.getKey(), graph, entry.getValue(), 5);
        }
        /*Log.d("Node Set: "+nodeSet.size());
        for (Map.Entry<Node, ColorSet> entry : nodeSet.entrySet()) {
            Log.d("Coords: "+entry.getKey().coordinates[0]+" "+entry.getKey().coordinates[1]);
        }*/
    }

    private static void printSucc(@NotNull Node node, @NotNull Graph graph, ColorSet set, int depth) {
        if (depth > 0) {
            @NotNull Map<Node, ColorSet> successors = graph.factory.computePredecessors(node, set);
            Log.d("Node "+node.coordinates[0]+" "+node.coordinates[1]+": "+successors.size());
            for (@NotNull Map.Entry<Node, ColorSet> entry : successors.entrySet()) {
                Log.d("     S "+entry.getKey().coordinates[0]+" "+entry.getKey().coordinates[1]+" "+entry.getValue());
                printSucc(entry.getKey(), graph, entry.getValue(), depth-1);
            }
            Log.d("------------- Up -------------");

        }
    }
    /*static {
        try {
            System.loadLibrary("ode-graph-generator"); // used for tests. This library in classpath only
        } catch (UnsatisfiedLinkError e) {
            try {
                NativeUtils.loadLibraryFromJar("/ode-graph-generator.jnilib"); // during runtime. .DLL within .JAR
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
    }*/

    static {
        try {
            System.loadLibrary("generator"); // used for tests. This library in classpath only
        } catch (UnsatisfiedLinkError e) {
            String property = System.getProperty("java.library.path");
            @NotNull StringTokenizer parser = new StringTokenizer(property, ";");
            while (parser.hasMoreTokens()) {
                System.err.println(parser.nextToken());
            }
            System.exit(1);
        }
    }
}
