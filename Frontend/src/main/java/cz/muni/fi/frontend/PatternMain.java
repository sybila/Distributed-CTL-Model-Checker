package cz.muni.fi.frontend;


import cz.muni.fi.ode.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PatternMain {

    static {
        NativeUtils.loadLibrary("ODE");
    }

    public static void main(@NotNull final String[] args) throws InterruptedException, IOException {

        long startLoad = System.currentTimeMillis();

        //read and prepare model
        @NotNull final OdeModel model = new OdeModel(args[args.length - 1]);
        model.load();

        long startComputation = System.currentTimeMillis();

        final ConcurrentHashMap<CoordinateNode, RectParamSpace> sinks = new ConcurrentHashMap<>();
        final ConcurrentHashMap<CoordinateNode, RectParamSpace> sources = new ConcurrentHashMap<>();

        final int processor_count = Integer.parseInt(args[args.length - 2]);
        List<Thread> runners = new ArrayList<>();
        for (int i=0; i < processor_count; i++) {
            final int finalI = i;
            Thread runner = new Thread(new Runnable() {
                @Override
                public void run() {
                    processPatterns(processor_count, finalI, args.length >= 1, model, sinks, sources);
                }
            });
            runner.start();
            runners.add(runner);
        }

        for (int i=0; i < processor_count; i++) {
            runners.get(i).join();
        }

        long startPrinting = System.currentTimeMillis();


        System.out.println(" Sink nodes: ");
        for (Map.Entry<CoordinateNode, RectParamSpace> sink : sinks.entrySet()) {
            System.out.println(model.coordinateString(sink.getKey().coordinates)+" "+sink.getValue());
        }

        System.out.println(" Source nodes: ");
        for (Map.Entry<CoordinateNode, RectParamSpace> source : sources.entrySet()) {
            System.out.println(model.coordinateString(source.getKey().coordinates)+" "+source.getValue());
        }

        System.out.println(" Multi-sinks: ");
        List<Map.Entry<CoordinateNode, RectParamSpace>> nodes = new ArrayList<>(sinks.entrySet());
        for (int i=0; i<nodes.size(); i++) {
            for (int j=i+1; j<nodes.size(); j++) {
                RectParamSpace common = new RectParamSpace(nodes.get(i).getValue().getItems());
                common.intersect(nodes.get(j).getValue());
                if (!common.isEmpty()) {
                    System.out.println(
                            common + " - " +
                            "First state: " + model.coordinateString(nodes.get(i).getKey().coordinates) + " Second state: " +
                            model.coordinateString(nodes.get(j).getKey().coordinates)
                    );
                }
            }
        }

        long end = System.currentTimeMillis();

        System.out.println("Abstraction time: " + (startComputation - startLoad)+"ms");
        System.out.println("Computation time: " + (startPrinting - startComputation)+"ms");
        System.out.println("Printing time: " + (end - startPrinting)+"ms");
        System.out.println("Total: " + (end - startLoad)+"ms");

    }

    private static void processPatterns(
            int size,
            int rank,
            boolean weak,
            OdeModel model,
            ConcurrentHashMap<CoordinateNode, RectParamSpace> sinkOutput,
            ConcurrentHashMap<CoordinateNode, RectParamSpace> sourceOutput) {

        @NotNull HashPartitioner partitioner = new HashPartitioner(model, size, rank);
        @NotNull NodeFactory factory = new NodeFactory(model, partitioner);
        @NotNull StateSpaceGenerator generator = new StateSpaceGenerator(model, factory, partitioner.getMyLimit());
        factory.setGenerator(generator);

        int counter = 0;
        int p = 0;
        //System.out.println(rank + ": Caching...");
        generator.cacheAllNodes();
        //System.out.println(rank + ": Cached");
        Collection<CoordinateNode> initial = factory.getNodes();
        for (CoordinateNode entry : initial) {
            counter++;
            if (counter > initial.size() / 100) {
                p++;
                counter = 0;
                System.err.println(rank + ": " + p+"% ");
            }

            Map<CoordinateNode, RectParamSpace> successors = factory.successorsFor(entry, null);
            Map<CoordinateNode, RectParamSpace> predecessors = factory.predecessorsFor(entry, null);

            //if we have enough edges
            if (weak || predecessors.size() >= 2*model.getVariableCount()) {
                RectParamSpace sinkColours = model.getFullColorSet();

                for (Map.Entry<CoordinateNode, RectParamSpace> predecessor : predecessors.entrySet()) {
                    if (predecessor.getKey().equals(entry)) continue;    //skip self loops
                    sinkColours.intersect(predecessor.getValue());
                    if (sinkColours.isEmpty()) break;
                }

                if (!sinkColours.isEmpty()) {
                    for (Map.Entry<CoordinateNode, RectParamSpace> successor : successors.entrySet()) {
                        if (successor.getKey().equals(entry)) continue;    //skip self loops
                        sinkColours.subtract(successor.getValue());
                        if (sinkColours.isEmpty()) break;
                    }

                    if (!sinkColours.isEmpty()) {
                        sinkOutput.put(entry, sinkColours);
                    }

                }

            }

            if (weak || successors.size() >= 2*model.getVariableCount()) {
                RectParamSpace sourceColours = model.getFullColorSet();

                for (Map.Entry<CoordinateNode, RectParamSpace> successor : successors.entrySet()) {
                    if (successor.getKey().equals(entry)) continue;    //skip self loops
                    sourceColours.intersect(successor.getValue());
                    if (sourceColours.isEmpty()) break;
                }

                if (!sourceColours.isEmpty()) {

                    for (Map.Entry<CoordinateNode, RectParamSpace> predecessor : predecessors.entrySet()) {
                        if (predecessor.getKey().equals(entry)) continue;    //skip self loops
                        sourceColours.subtract(predecessor.getValue());
                        if (sourceColours.isEmpty()) break;
                    }

                    if (!sourceColours.isEmpty()) {
                        sourceOutput.put(entry, sourceColours);
                    }

                }


            }

        }
    }

}
