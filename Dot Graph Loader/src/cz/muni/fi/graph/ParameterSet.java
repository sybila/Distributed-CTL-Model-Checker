package cz.muni.fi.graph;

import cz.muni.fi.ctl.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParameterSet {

    //We need fast inverse (for negation) (up to some upper limit)
    //Fast intersect - for && and for path+node checking
    //fast empty check
    //fast union

    public static int limit = 10;

    private List<Integer> values = new ArrayList<>();

    public ParameterSet(List<Integer> values) {
        this.values.addAll(values);
    }

    public void intersect(ParameterSet other) {
        List<Integer> intersect = new ArrayList<>();
        int myIter = 0;
        int otherIter = 0;
        while (myIter < values.size() && otherIter < values.size()) {
            if (values.get(myIter) > other.values.get(otherIter)) {
                //skip all smaller intervals
                while (other.values.get(otherIter) < values.get(myIter)) {
                    otherIter++;
                }
                if (otherIter % 2 == 1) {
                    intersect.add(values.get(myIter));
                    intersect.add(other.values.get(otherIter));

                }
            }
        }
        values = intersect;
    }

    public void union(ParameterSet other) {
        List<Integer> union = new ArrayList<>(Math.max(values.size(),other.values.size()));
        int myIter = 0;
        int otherIter = 0;
        while (myIter < values.size() && otherIter < other.values.size()) {
            //if smaller value is in our list, add it to the result
            if (values.get(myIter) < other.values.get(otherIter)) {
                union.add(values.get(myIter));
                myIter++;
                //skip all values from other list that are smaller than the end of interval
                while (otherIter < other.values.size() && other.values.get(otherIter) <= values.get(myIter) ) {
                    otherIter++;
                }
                //if I stopped in the middle of interval, it means I have intersection and I can push interval end
                if (otherIter % 2 == 1) {
                    union.add(other.values.get(otherIter));
                    otherIter++;
                } else {
                    union.add(values.get(myIter));
                }
                myIter++;
            } else {
                union.add(other.values.get(otherIter));
                otherIter++;
                while (myIter < values.size() && values.get(myIter) <= other.values.get(otherIter)) {
                    Log.d("Skip: "+myIter);
                    myIter++;
                }
                if (myIter % 2 == 1) {
                    union.add(values.get(myIter));
                    myIter++;
                } else {
                    union.add(other.values.get(otherIter));
                }
                otherIter++;
            }
        }
        while (myIter < values.size()) {
            union.add(values.get(myIter));
            myIter++;
        }
        while (otherIter < other.values.size()) {
            union.add(other.values.get(otherIter));
            otherIter++;
        }
        values = union;
    }

    public void invert() {
        List<Integer> inversion = new ArrayList<>(values.size());
        if (values.get(0) != 0) {   //handle start of list separately
            inversion.add(0);
            inversion.add(values.get(0) - 1);
        }
        for (int i = 1; i<values.size()-1; i+=2) {  //for all pairs between starting and ending value
            inversion.add(values.get(i)+1);
            inversion.add(values.get(i+1)-1);
        }
        int last = values.size() - 1;
        if (values.get( last ) != limit) {  //handle end of list separately
            inversion.add( values.get(last) + 1 );
            inversion.add( limit );
        }
        values = inversion;
    }

    public boolean isEmpty() {
        return values.size() == 0;
    }

    @Override
    public String toString() {
        return Arrays.toString(values.toArray());
    }
}
