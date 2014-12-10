package cz.muni.fi.distributed.graph;


import com.google.common.collect.Range;
import cz.muni.fi.model.ColorSet;
import cz.muni.fi.model.Model;
import cz.muni.fi.model.StateSpacePartitioner;
import cz.muni.fi.model.TreeColorSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Graph {
    
    @NotNull
    public final StateSpacePartitioner partitioner;
    @NotNull
    public final NodeFactory factory;
    @NotNull
    public final Model model;
    public final int id;

    public Graph(@NotNull Model model, int size, int rank) {
        this.model = model;
        partitioner = new StateSpacePartitioner(model, size, rank);
        factory = new NodeFactory(this);
        this.id = rank;
    }

    public boolean isMyNode(@NotNull Node node) {
        return partitioner.getGraphId(node) == id;
    }

    @NotNull
    public List<Range<Double>> getLimit() {
        return partitioner.getMyLimit(this);
    }

    @NotNull
    public ColorSet getFullParamRange() {
        @NotNull List<Range<Double>> list = model.getParameterRange();
        @NotNull ColorSet colorSet = TreeColorSet.createEmpty(model.variableCount());
        for (int i = 0; i < list.size(); i++) {
            Range<Double> item = list.get(i);
            colorSet.get(i).add(item);
        }
        return colorSet;
    }

}
