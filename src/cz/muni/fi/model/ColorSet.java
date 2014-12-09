package cz.muni.fi.model;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

import java.util.List;

public interface ColorSet extends List<RangeSet<Double>> {

    public void union(ColorSet set);
    public void intersect(ColorSet set);
    public void subtract(ColorSet set);
    public boolean encloses(ColorSet set);
    public Range<Double>[] asArrayForParam(int i);
}
