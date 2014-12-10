package cz.muni.fi.graph;

import cz.muni.fi.ctl.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class ParameterSet {

    //We need fast inverse (for negation) (up to some upper limit)
    //Fast intersect - for && and for path+node checking
    //We need removeAll
    //fast empty check
    //fast union

    public static int limit = 10;

    @NotNull
    private List<Integer> values = new ArrayList<>();

    public ParameterSet(@NotNull List<Integer> values) {
        this.values.addAll(values);
    }

    //should be good
    //TODO: move size check before value checks
    public void intersect(@NotNull ParameterSet other) {
        @NotNull List<Integer> intersect = new ArrayList<>();
        int myIter = 0;
        int otherIter = 0;
        while (myIter < values.size() && otherIter < other.values.size()) {
            Log.d("Process: "+values.get(myIter)+" "+other.values.get(otherIter));
            if (values.get(myIter) > other.values.get(otherIter)) {
                //skip all smaller intervals
                while (other.values.get(otherIter) < values.get(myIter) && otherIter < other.values.size()) {
                    Log.d("Skipping: "+other.values.get(otherIter));
                    otherIter++;
                }
                //if I end up in the middle of interval, I have intersection
                while (otherIter % 2 == 1 && myIter < values.size()) {
                    intersect.add(values.get(myIter));
                    if (other.values.get(otherIter) <= values.get(myIter+1)) {
                        intersect.add(other.values.get(otherIter));
                        otherIter++;
                    } else {
                        myIter++;
                        intersect.add(values.get(myIter));
                        myIter++;
                        if (values.get(myIter) > other.values.get(otherIter)) {
                            otherIter++;
                        }
                    }
                }
            } else if (values.get(myIter) < other.values.get(otherIter)) {
                while (values.get(myIter) < other.values.get(otherIter) && myIter < this.values.size()) {
                    Log.d("Skipping: "+values.get(myIter));
                    myIter++;
                }
                while (myIter % 2 == 1 && otherIter < other.values.size()) {
                    intersect.add(other.values.get(otherIter));
                    if (this.values.get(myIter) <= other.values.get(otherIter+1)) {
                        intersect.add(values.get(myIter));
                        myIter++;
                    } else {
                        otherIter++;
                        intersect.add(other.values.get(otherIter));
                        otherIter++;
                        if (other.values.get(otherIter) > this.values.get(myIter)) {
                            myIter++;
                        }
                    }
                }
            } else {    //equal
                if (values.get(myIter+1) <= other.values.get(otherIter+1)) {
                    intersect.add(values.get(myIter));
                    myIter++;
                    intersect.add(values.get(myIter));
                    myIter++;
                } else {
                    intersect.add(other.values.get(otherIter));
                    otherIter++;
                    intersect.add(other.values.get(otherIter));
                    otherIter++;
                }
            }
        }
        //no need to scan remaining, its intersection, so they won't be there
        values = intersect;
    }

    //scrap this whole thing
    //1. Find start
    //2. Follow intervals till end
    //3. Write interval
    //4. repeat
    public void union(@NotNull ParameterSet other) {
        @NotNull List<Integer> union = new ArrayList<>(Math.max(values.size(),other.values.size()));
        int myIter = 0;
        int otherIter = 0;
        int start = -1;
        int end = -1;
        while (myIter < values.size() || otherIter < other.values.size()) {
//            Log.d("Process: "+values.get(myIter)+" "+other.values.get(otherIter));
            //if smaller value is in our list, add it to the result
            if (otherIter >= other.values.size() || values.get(myIter) < other.values.get(otherIter)) {
                if (start == -1) {
                    start = values.get(myIter);
                    myIter++;
                    end = values.get(myIter);
                    myIter++;
                } else {
                    if (values.get(myIter) > end) {
                        union.add(start);
                        union.add(end);
                        start = -1;
                        end = -1;
                    } else {
                        myIter++;
                        end = Math.max(values.get(myIter), end);
                        myIter++;
                    }
                }
            } else {
                if (start == -1) {
                    start = other.values.get(otherIter);
                    otherIter++;
                    end = other.values.get(otherIter);
                    otherIter++;
                } else  {
                    if (other.values.get(otherIter) > end) {
                        union.add(start);
                        union.add(end);
                        start = -1;
                        end = -1;
                    } else {
                        otherIter++;
                        end = Math.max(other.values.get(otherIter), end);
                        otherIter++;
                    }
                }
            }
        }
        if (end != -1) {
            union.add(start);
            union.add(end);
        }
        values = union;
    }

    //should be good
    public void invert() {
        @NotNull List<Integer> inversion = new ArrayList<>(values.size());
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

    @NotNull
    @Override
    public String toString() {
        return Arrays.toString(values.toArray());
    }
}
