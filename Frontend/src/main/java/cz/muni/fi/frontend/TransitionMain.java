package cz.muni.fi.frontend;

import com.google.common.collect.Range;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.muni.fi.ctl.formula.proposition.Tautology;
import cz.muni.fi.export.*;
import cz.muni.fi.ode.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

/**
 * Created by daemontus on 12/05/15.
 */
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
            Successor.Successors successors = new Successor.Successors();
            successors.origin = convertNode(entry.getKey(), model);
            for (Map.Entry<CoordinateNode, TreeColorSet> succ : factory.successorsFor(entry.getKey(), null).entrySet()) {
                Successor successor = new Successor();
                successor.target = convertNode(succ.getKey(), model);
                for (int i=0; i < succ.getValue().size(); i++) {
                    Parameter parameter = new Parameter();
                    parameter.index = i;
                    Successor.Param param = new Successor.Param();
                    param.parameter = parameter;
                    for (Range<Double> range : succ.getValue().get(i).asRanges()) {
                        Interval<Double> interval = new Interval<>();
                        interval.low = range.lowerEndpoint();
                        interval.high = range.upperEndpoint();
                        param.intervals.add(interval);
                    }
                    successor.colours.add(param);
                }
                successors.transitions.add(successor);
            }
            exported.transitions.add(successors);
        }

        System.out.println(gson.toJson(exported));

    }

    private static Node convertNode(CoordinateNode cn, OdeModel model) {
        Node node = new Node();
        for (int k : cn.coordinates) {
            node.coordinates.add(k);
        }
        for (int i=0; i < cn.coordinates.length; i++) {
            Interval<Double> interval = new Interval<>();
            interval.low = model.getThresholdValueForVariableByIndex(i, cn.coordinates[i]);
            interval.high = model.getThresholdValueForVariableByIndex(i, cn.coordinates[i] + 1);
            node.thresholds.add(interval);
        }
        return node;
    }

}
