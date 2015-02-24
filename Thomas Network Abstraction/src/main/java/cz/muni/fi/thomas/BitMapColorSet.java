package cz.muni.fi.thomas;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import cz.muni.fi.modelchecker.graph.ColorSet;

/**
 * This set stores a set of parameters in compressed bitmap.
 */
public class BitMapColorSet implements ColorSet {

    private EWAHCompressedBitmap values = new EWAHCompressedBitmap();

    public static BitMapColorSet createCopy(BitMapColorSet other) {
        BitMapColorSet result = new BitMapColorSet();
        try {
            result.values = other.values.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static BitMapColorSet createFull(int size) {
        BitMapColorSet result = new BitMapColorSet();
        result.values.setSizeInBits(size, true);
        return result;
    }


    @Override
    public void intersect(ColorSet set1) {
        BitMapColorSet set = (BitMapColorSet) set1;
        values = values.and(set.values);
    }

    @Override
    public void subtract(ColorSet set1) {
        BitMapColorSet set = (BitMapColorSet) set1;
        values = values.andNot(set.values);
    }

    @Override
    public void union(ColorSet set1) {
        BitMapColorSet set = (BitMapColorSet) set1;
        values = values.or(set.values);
    }

    //used by native initializer
    public void unset(int i) {
        values.clear(i);
    }

    @Override
    public String toString() {
        return (isFull() ? "ANY" : values.toString()) + " " + values.sizeInBytes() + " " + values.sizeInBits();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean isFull() {
        return values.cardinality() == values.sizeInBits();
    }

    public boolean encloses(BitMapColorSet set) {
        //if or does not add any new elements, we are super set of given parameter
        return values.cardinality() == values.orCardinality(set.values);
    }
}
