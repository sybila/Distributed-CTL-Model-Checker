package cz.muni.fi.ode;

import com.google.common.collect.Range;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RectangularPartitioner implements StateSpacePartitioner<CoordinateNode> {

    private final int size;
    private final int rank;
    private final OdeModel model;
    private final List<Range<Double>> ranges = new ArrayList<>();

    public RectangularPartitioner(@NotNull OdeModel odeModel, int size, int rank) {
        this.size = size;
        this.rank = rank;
        this.model = odeModel;
        Range<Double> firstVar = odeModel.getVariableRange().get(0);
        if (!firstVar.hasLowerBound() || !firstVar.hasUpperBound()) {
            throw new IllegalArgumentException("Range is unbounded");
        }
        int interval = (int) (firstVar.upperEndpoint() - firstVar.lowerEndpoint());
        interval /= size;
        for (int i = 0; i<size; i++) {
            if (i == size - 1) {
                ranges.add(Range.closed(firstVar.lowerEndpoint() + interval * i, firstVar.upperEndpoint()));
            } else {
                ranges.add(Range.closedOpen(firstVar.lowerEndpoint() + interval * i, firstVar.lowerEndpoint() + interval * (i+1)));
            }
        }
    }

    @NotNull
    public List<Range<Double>> getMyLimit() {
        @NotNull List<Range<Double>> ret = new ArrayList<>();
        ret.add(ranges.get(rank));
        @NotNull List<Range<Double>> params = model.getVariableRange();
        for (int i=1; i < params.size(); i++) {
            ret.add(params.get(i));
        }
        return ret;
    }

    @Override
    public int getNodeOwner(@NotNull CoordinateNode node) throws IllegalArgumentException {
        for (int i = 0; i < size; i++) {
            Range<Double> range = ranges.get(i);
            if (range.contains((double) node.getCoordinate(0))) {
                return i;
            }
        }
        throw new IllegalStateException("Node "+node.getCoordinate(0)+" does not have a parent graph");
    }
}
