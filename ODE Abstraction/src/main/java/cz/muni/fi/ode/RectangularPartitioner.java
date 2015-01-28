package cz.muni.fi.ode;

import com.google.common.collect.Range;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
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
        Range<Double> firstVar = odeModel.getThresholdRanges().get(0);
        if (!firstVar.hasLowerBound() || !firstVar.hasUpperBound()) {
            throw new IllegalArgumentException("Range is unbounded");
        }
        int tokens = (int) (firstVar.upperEndpoint() - firstVar.lowerEndpoint());
        int[] intSize = new int[size];
        for (int i = 0; tokens > 0; i = (i+1)%size, tokens--) {
            intSize[i]++;
        }
        double lower = firstVar.lowerEndpoint();
        for (int i = 0; i<size; i++) {
            if (i == size - 1) {
                ranges.add(Range.closed(lower, firstVar.upperEndpoint()));
            } else {
                ranges.add(Range.closedOpen(lower, lower + intSize[i]));
            }
            lower += intSize[i];
        }
        System.out.println(rank+" "+Arrays.toString(ranges.toArray()));
    }

    @NotNull
    public List<Range<Double>> getMyLimit() {
        @NotNull List<Range<Double>> ret = new ArrayList<>();
        ret.add(ranges.get(rank));
        @NotNull List<Range<Double>> params = model.getThresholdRanges();
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

    @Override
    public int getMyId() {
        return rank;
    }
}
