package cz.muni.fi.frontend;


import cz.muni.fi.ctl.*;
import cz.muni.fi.ode.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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

        Parser parser = new Parser();
        Normalizer normalizer = new Normalizer();
        Formula parsed = parser.parse(new File(args[args.length - 2])).get("pattern");
        final Formula pattern = normalizer.normalize(parsed);
        System.out.println("Checking formula: "+pattern);

        long startLoad = System.currentTimeMillis();

        //read and prepare model
        @NotNull final OdeModel model = new OdeModel(args[args.length - 1]);
        model.load();

        long startComputation = System.currentTimeMillis();

        final ConcurrentHashMap<CoordinateNode, RectParamSpace> results = new ConcurrentHashMap<>();

        final int processor_count = Integer.parseInt(args[args.length - 3]);
        List<Thread> runners = new ArrayList<>();
        for (int i=0; i < processor_count; i++) {
            final int finalI = i;
            Thread runner = new Thread(new Runnable() {
                @Override
                public void run() {
                    processPatterns(processor_count, finalI, args.length > 3, model, pattern, results);
                }
            });
            runner.start();
            runners.add(runner);
        }

        for (int i=0; i < processor_count; i++) {
            runners.get(i).join();
        }

        long startPrinting = System.currentTimeMillis();


        System.out.println(" Results: ");
        for (Map.Entry<CoordinateNode, RectParamSpace> sink : results.entrySet()) {
            System.out.println(model.coordinateString(sink.getKey().coordinates)+" "+sink.getValue());
        }
/*
        System.out.println(" Source nodes: ");
        for (Map.Entry<CoordinateNode, RectParamSpace> source : sources.entrySet()) {
            System.out.println(model.coordinateString(source.getKey().coordinates)+" "+source.getValue());
        }*/
/*
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
        }*/

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
            Formula formula,
            ConcurrentHashMap<CoordinateNode, RectParamSpace> output) {

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
            if (counter % (initial.size() / 100) == 0) {
                p++;
                System.err.println(rank + ": " + p+"% ");
            }

            Map<CoordinateNode, RectParamSpace> successors = factory.successorsFor(entry, null);
            Map<CoordinateNode, RectParamSpace> predecessors = factory.predecessorsFor(entry, null);

            RectParamSpace result = checkProperty(formula, entry, successors, predecessors, model, factory);

            if (!result.isEmpty()) output.put(entry, result);

            //if we have enough edges
            /*if (weak || predecessors.size() >= 2*model.getVariableCount()) {
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


            }*/

        }
    }

    private static RectParamSpace checkProperty(
            Formula property,
            CoordinateNode node,
            Map<CoordinateNode, RectParamSpace> successors,
            Map<CoordinateNode, RectParamSpace> predecessors,
            OdeModel model,
            NodeFactory factory) {
        if (property instanceof DirectionProposition) {
            DirectionProposition dp = (DirectionProposition) property;
            int varIndex = model.getVariableIndexByName(dp.getVariable());
            CoordinateNode target = dp.getFacet() == Facet.POSITIVE ? factory.above(node, varIndex) : factory.below(node, varIndex);
            if (target == null) return RectParamSpace.Companion.empty();
            else {
                RectParamSpace res = dp.getDirection() == Direction.IN ? predecessors.get(target) : successors.get(target);
                return res == null ? RectParamSpace.Companion.empty() : res;
            }
        } else if (property.getOperator() == Op.NEGATION) {
            RectParamSpace s = model.getFullColorSet();
            s.subtract(checkProperty(property.get(0), node, successors, predecessors, model, factory));
            return s;
        } else if (property.getOperator() == Op.AND) {
            RectParamSpace s1 = checkProperty(property.get(0), node, successors, predecessors, model, factory);
            RectParamSpace s2 = checkProperty(property.get(1), node, successors, predecessors, model, factory);
            RectParamSpace result = new RectParamSpace(s1.getItems());
            result.intersect(s2);
            return result;
        } else if (property.getOperator() == Op.OR) {
            RectParamSpace s1 = checkProperty(property.get(0), node, successors, predecessors, model, factory);
            RectParamSpace s2 = checkProperty(property.get(1), node, successors, predecessors, model, factory);
            RectParamSpace result = new RectParamSpace(s1.getItems());
            result.union(s2);
            return result;
        } else {
            throw new IllegalStateException("Unsupported operator: "+property.getOperator());
        }
    }
}
