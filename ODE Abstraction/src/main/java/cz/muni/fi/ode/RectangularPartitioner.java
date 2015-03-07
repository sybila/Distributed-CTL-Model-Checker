package cz.muni.fi.ode;

import com.google.common.collect.Range;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RectangularPartitioner implements CoordinatePartitioner {

    private final int size;
    private final int rank;
    @NotNull
    private final OdeModel model;
    private final List<Range<Integer>> ranges = new ArrayList<>();

    public RectangularPartitioner(@NotNull OdeModel odeModel, int size, int rank) {
        this.size = size;
        this.rank = rank;
        this.model = odeModel;
        Range<Integer> firstVar = odeModel.getThresholdRanges().get(0);
        if (!firstVar.hasLowerBound() || !firstVar.hasUpperBound()) {
            throw new IllegalArgumentException("Range is unbounded");
        }
        int tokens = firstVar.upperEndpoint() - firstVar.lowerEndpoint();
        @NotNull int[] intSize = new int[size];
        for (int i = 0; tokens > 0; i = (i+1)%size, tokens--) {
            intSize[i]++;
        }
        int lower = firstVar.lowerEndpoint();
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
    @Override
    public List<Range<Integer>> getMyLimit() {
        @NotNull List<Range<Integer>> ret = new ArrayList<>();
        ret.add(ranges.get(rank));
        @NotNull List<Range<Integer>> params = model.getThresholdRanges();
        for (int i=1; i < params.size(); i++) {
            ret.add(params.get(i));
        }
        return ret;
    }

    @Override
    public int getNodeOwner(@NotNull CoordinateNode node) throws IllegalArgumentException {
        for (int i = 0; i < size; i++) {
            Range<Integer> range = ranges.get(i);
            if (range.contains(node.getCoordinate(0))) {
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
