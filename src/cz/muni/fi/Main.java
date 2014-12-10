package cz.muni.fi;

import com.google.common.collect.Range;
import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.parser.CTLParser;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.dot.DotParser;
import cz.muni.fi.distributed.graph.Graph;
import cz.muni.fi.distributed.graph.Node;
import cz.muni.fi.graph.Path;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    /*
    Performance when using HashSet (TreeSet is much much worse...)
    End time: 404 Params: 1
    End time: 427 Params: 101
    End time: 638 Params: 201
    End time: 943 Params: 301
    End time: 1204 Params: 401
    End time: 1462 Params: 501
    End time: 1846 Params: 601
    End time: 2057 Params: 701
    End time: 2313 Params: 801
    End time: 2538 Params: 901
     */

    public static void main(String[] args) throws IOException, InterruptedException {
/*
        File folder = new File("/Users/daemontus/Downloads/doty/");
        //Graph graph = DotParser.parse(folder.listFiles());
        //Formula formula = CTLParser.parse("E ( { a < 7.0 } U { b > 5.0 } )");
        Formula formula = CTLParser.parse("AF { a > 25.0 } && AF { b < 85.0 }");
        long time = System.currentTimeMillis();
        //Log.d("Checking started.");
        //ModelChecker.check(graph, formula);
        Log.d("End time: "+(System.currentTimeMillis()-time)+" Params: "+folder.listFiles().length);
/*        for (int i=1; i<1000; i+=100) {
            test(i);
        }*/
        /*for (Node node : graph.getInitialNodes()) {
            printRecursive(node, 5);
        }*/
    }

    private static void test(int count) throws IOException {
        @NotNull File folder = new File("/Users/daemontus/Downloads/doty/");
        @NotNull File f = new File("/Users/daemontus/Downloads/doty/enzyme_1complex.0.dot");
        @NotNull File[] input = new File[count];
        for (int i=0; i<input.length; i++) {
            input[i] = f;
        }
     //   Graph graph = DotParser.parse(folder.listFiles());
        Formula formula = CTLParser.parse("E ( { a < 7.0 } U { b > 5.0 } )");
        long time = System.currentTimeMillis();
        //Log.d("Checking started.");
    //    ModelChecker.check(graph, formula);
        Log.d("End time: "+(System.currentTimeMillis()-time)+" Params: "+input.length);
    }


    private static void printRecursive(@NotNull Node node, int k) {
        if (k<=0) return;
        System.out.println(node.toString());
        //System.out.println(node.formulas.size()+" "+Arrays.toString(node.formulas.keySet().toArray()));
    /*    for (Path path : node.getAfter()) {
            Node n = path.getTo();
            printRecursive(n, k-1);
        }*/
    }

}
