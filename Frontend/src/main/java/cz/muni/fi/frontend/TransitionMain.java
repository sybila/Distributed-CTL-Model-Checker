package cz.muni.fi.frontend;

import com.google.common.collect.Range;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.muni.fi.ctl.formula.proposition.Tautology;
import cz.muni.fi.export.Model;
import cz.muni.fi.export.Transition;
import cz.muni.fi.export.Variable;
import cz.muni.fi.ode.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class TransitionMain {

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

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Model exported = new Model();

        for (int i=0; i<model.getVariableCount(); i++) {
            Variable variable = new Variable();
            variable.name = model.getVariableNameByIndex(i);
            for (int j=0; j<model.getThresholdCountForVariable(i); j++) {
                variable.thresholds.add(model.getThresholdValueForVariableByIndex(i, j));
            }
            exported.variables.add(variable);
        }

        for (Map.Entry<CoordinateNode, TreeColorSet> entry : factory.initialNodes(Tautology.INSTANCE).entrySet()) {
            for (Map.Entry<CoordinateNode, TreeColorSet> succ : factory.successorsFor(entry.getKey(), null).entrySet()) {
                Transition transition = new Transition();
                transition.source = convertNode(entry.getKey());
                transition.destination = convertNode(succ.getKey());
                transition.colours = new double[succ.getValue().size()][2];
                for (int i=0; i < succ.getValue().size(); i++) {
                    Set<Range<Double>> ranges = succ.getValue().get(i).asRanges();
                    if (ranges.size() != 1) {
                        throw new IllegalStateException(" A transition has more than one interval per parameter ");
                    }
                    Range<Double> range = ranges.iterator().next();
                    transition.colours[i][0] = range.lowerEndpoint();
                    transition.colours[i][1] = range.upperEndpoint();
                }
                exported.transitions.add(transition);
            }
        }

        System.out.println(gson.toJson(exported));

    }

    private static int[] convertNode(CoordinateNode cn) {
        return Arrays.copyOf(cn.coordinates, cn.coordinates.length);
    }

}
