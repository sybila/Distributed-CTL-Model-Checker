package cz.muni.fi.model;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class TreeColorSet extends ArrayList<RangeSet<Double>> implements ColorSet {

    public static TreeColorSet createEmpty(int dimensions) {
        TreeColorSet set = new TreeColorSet(dimensions);
        for (int i=0; i<dimensions; i++) {
            RangeSet<Double> range = TreeRangeSet.create();
            set.add(range);
        }
        return set;
    }

    public static TreeColorSet createCopy(ColorSet set) {
        TreeColorSet newSet = new TreeColorSet(set.size());
        for (RangeSet<Double> aSet : set) {
            RangeSet<Double> range = TreeRangeSet.create(aSet);
            newSet.add(range);
        }
        return newSet;
    }

    public TreeColorSet(int initialCapacity) {
        super(initialCapacity);
    }

    public TreeColorSet() {
    }

    public TreeColorSet(Collection<? extends RangeSet<Double>> c) {
        super(c);
    }

    @Override
    public void union(ColorSet set) {
        for (int i=0; i<size(); i++) {
            get(i).addAll(set.get(i));
        }
    }

    public static TreeColorSet createFromBuffer(int[] lengths, double[] data) {
        TreeColorSet newSet = new TreeColorSet(lengths.length);
        int total = 0;
        for (int length : lengths) {
            RangeSet<Double> ranges = TreeRangeSet.create();
            for (int i = 0; i < length; i+=2) {
                ranges.add(Range.open(data[total+i], data[total+i+1]));
            }
            newSet.add(ranges);
            total += length;
        }
        return newSet;
    }

    @Override
    public void intersect(ColorSet set) {
        for (int i=0; i<size(); i++) {
            RangeSet<Double> diff = TreeRangeSet.create(get(i));
            diff.removeAll(set.get(i));
            get(i).removeAll(diff);
        }
    }

    @Override
    public void subtract(ColorSet set) {
        for (int i=0; i<size(); i++) {
            get(i).removeAll(set.get(i));
        }
    }

    @Override
    public boolean isEmpty() {
        for (RangeSet<Double> doubleRangeSet : this) {
            if (doubleRangeSet.isEmpty()) return true;
        }
        return false;
    }

    @Override
    public boolean encloses(ColorSet set) {
        for (int i=0; i<size(); i++) {
            if (!get(i).enclosesAll(set.get(i))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Override
    public Range<Double>[] asArrayForParam(int i) {
        RangeSet<Double> set = get(i);
        Set<Range<Double>> data = set.asRanges();
        return data.toArray(new Range[data.size()]);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ColorSet (");
        for (RangeSet<Double> ranges : this) {
            builder.append("[");
            for (Range<Double> range : ranges.asRanges()) {
                builder.append("<").append(range.lowerEndpoint()).append(",").append(range.upperEndpoint()).append(">");
            }
            builder.append("] ");
        }
        builder.append(")");
        return builder.toString();
    }

}
