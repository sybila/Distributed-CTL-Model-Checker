package cz.muni.fi.frontend;


import cz.muni.fi.ctl.formula.proposition.Tautology;
import cz.muni.fi.ode.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatternMain {

    static {
        NativeUtils.loadLibrary("ODE");
    }

    public static void main(@NotNull String[] args) throws InterruptedException, IOException {

        //read and prepare model
        @NotNull OdeModel model = new OdeModel(args[args.length - 1]);
        model.load();

        //@NotNull CoordinatePartitioner partitioner = new RectangularPartitioner(model, MPI.COMM_WORLD.Size(), MPI.COMM_WORLD.Rank());
        @NotNull HashPartitioner partitioner = new HashPartitioner(model, 1, 0);
        @NotNull NodeFactory factory = new NodeFactory(model, partitioner);
        @NotNull StateSpaceGenerator generator = new StateSpaceGenerator(model, factory, partitioner.getMyLimit());
        factory.setGenerator(generator);

        Map<CoordinateNode, ColorFormulae> sinks = new HashMap<>();
        Map<CoordinateNode, ColorFormulae> sources = new HashMap<>();

        Map<CoordinateNode, ColorFormulae> initial = factory.initialNodes(Tautology.INSTANCE);
        for (Map.Entry<CoordinateNode, ColorFormulae> entry : initial.entrySet()) {


            CoordinateNode node = entry.getKey();

            Map<CoordinateNode, ColorFormulae> successors = factory.successorsFor(node, null);
            Map<CoordinateNode, ColorFormulae> predecessors = factory.predecessorsFor(node, null);

            //if we have enough edges
            if (args.length > 1 || predecessors.size() >= 2*model.getVariableCount()) {
                ColorFormulae sinkColours = model.getFullColorSet();

                for (Map.Entry<CoordinateNode, ColorFormulae> predecessor : predecessors.entrySet()) {
                    if (predecessor.getKey().equals(node)) continue;    //skip self loops
                    sinkColours.intersect(predecessor.getValue());
                    if (sinkColours.isEmpty()) break;
                }

                if (!sinkColours.isEmpty()) {
                    for (Map.Entry<CoordinateNode, ColorFormulae> successor : successors.entrySet()) {
                        if (successor.getKey().equals(node)) continue;    //skip self loops
                        sinkColours.subtract(successor.getValue());
                        if (sinkColours.isEmpty()) break;
                    }

                    if (!sinkColours.isEmpty()) {
                        sinks.put(node, sinkColours);
                    }

                }

            }

            if (args.length > 1 || successors.size() >= 2*model.getVariableCount()) {
                ColorFormulae sourceColours = model.getFullColorSet();

                for (Map.Entry<CoordinateNode, ColorFormulae> successor : successors.entrySet()) {
                    if (successor.getKey().equals(node)) continue;    //skip self loops
                    sourceColours.intersect(successor.getValue());
                    if (sourceColours.isEmpty()) break;
                }

                if (!sourceColours.isEmpty()) {

                    for (Map.Entry<CoordinateNode, ColorFormulae> predecessor : predecessors.entrySet()) {
                        if (predecessor.getKey().equals(node)) continue;    //skip self loops
                        sourceColours.subtract(predecessor.getValue());
                        if (sourceColours.isEmpty()) break;
                    }

                    if (!sourceColours.isEmpty()) {
                        sources.put(node, sourceColours);

                    }

                }


            }

        }

        System.out.println(" Sink nodes: ");
        for (Map.Entry<CoordinateNode, ColorFormulae> sink : sinks.entrySet()) {
            System.out.println(model.coordinateString(sink.getKey().coordinates)+" "+sink.getValue());
        }

        System.out.println(" Source nodes: ");
        for (Map.Entry<CoordinateNode, ColorFormulae> source : sources.entrySet()) {
            System.out.println(model.coordinateString(source.getKey().coordinates)+" "+source.getValue());
        }

        System.out.println(" Multi-sinks: ");
        List<Map.Entry<CoordinateNode, ColorFormulae>> nodes = new ArrayList<>(sinks.entrySet());
        for (int i=0; i<nodes.size(); i++) {
            for (int j=i+1; j<nodes.size(); j++) {
                ColorFormulae common = (ColorFormulae) ColorFormulae.createCopy(nodes.get(i).getValue());
                common.intersect(nodes.get(j).getValue());
                if (!common.isEmpty()) {
                    System.out.println(
                            common + ": " +
                            model.coordinateString(nodes.get(i).getKey().coordinates) + " " +
                            model.coordinateString(nodes.get(j).getKey().coordinates)
                    );
                }
            }
        }
    }



}
