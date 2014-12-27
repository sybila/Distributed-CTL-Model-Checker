package cz.muni.fi.ode;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import cz.muni.fi.modelchecker.graph.ColorSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class TreeColorSet extends ArrayList<RangeSet<Double>> implements ColorSet {

    @NotNull
    public static TreeColorSet createEmpty(int dimensions) {
        @NotNull TreeColorSet set = new TreeColorSet(dimensions);
        for (int i=0; i<dimensions; i++) {
            @NotNull RangeSet<Double> range = TreeRangeSet.create();
            set.add(range);
        }
        return set;
    }

    @NotNull
    public static TreeColorSet createCopy(@NotNull TreeColorSet set) {
        @NotNull TreeColorSet newSet = new TreeColorSet(set.size());
        for (RangeSet<Double> aSet : set) {
            @NotNull RangeSet<Double> range = TreeRangeSet.create(aSet);
            newSet.add(range);
        }
        return newSet;
    }

    public TreeColorSet(int initialCapacity) {
        super(initialCapacity);
    }

    public TreeColorSet() {
    }

    public TreeColorSet(@NotNull Collection<? extends RangeSet<Double>> c) {
        super(c);
    }

    public void union(@NotNull TreeColorSet set) {
        for (int i=0; i<size(); i++) {
            get(i).addAll(set.get(i));
        }
    }

    @NotNull
    public static TreeColorSet createFromBuffer(@NotNull int[] lengths, double[] data) {
        @NotNull TreeColorSet newSet = new TreeColorSet(lengths.length);
        int total = 0;
        for (int length : lengths) {
            @NotNull RangeSet<Double> ranges = TreeRangeSet.create();
            for (int i = 0; i < length; i+=2) {
                ranges.add(Range.closed(data[total+i], data[total+i+1]));
            }
            newSet.add(ranges);
            total += length;
        }
        return newSet;
    }

    public void subtract(@NotNull TreeColorSet set) {
        for (int i=0; i<size(); i++) {
            get(i).removeAll(set.get(i));
        }
    }

    @Override
    public boolean isEmpty() {
        for (@NotNull RangeSet<Double> doubleRangeSet : this) {
            if (doubleRangeSet.isEmpty()) return true;
        }
        return false;
    }

    public boolean encloses(@NotNull TreeColorSet set) {
        for (int i=0; i<size(); i++) {
            if (!get(i).enclosesAll(set.get(i))) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    @SuppressWarnings("UnusedDeclaration")
    public Range<Double>[] asArrayForParam(int i) {
        RangeSet<Double> set = get(i);
        Set<Range<Double>> data = set.asRanges();
        return data.toArray(new Range[data.size()]);
    }


    @NotNull
    @Override
    public String toString() {
        @NotNull StringBuilder builder = new StringBuilder();
        builder.append("ColorSet (");
        for (@NotNull RangeSet<Double> ranges : this) {
            builder.append("[");
            for (@NotNull Range<Double> range : ranges.asRanges()) {
                builder.append("<").append(range.lowerEndpoint()).append(",").append(range.upperEndpoint()).append(">");
            }
            builder.append("] ");
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public void intersect(ColorSet set1) {
        TreeColorSet set = (TreeColorSet) set1;
        for (int i=0; i<size(); i++) {
            @NotNull RangeSet<Double> diff = TreeRangeSet.create(get(i));
            diff.removeAll(set.get(i));
            get(i).removeAll(diff);
        }
    }
}
