package cz.muni.fi.ode;

import com.google.common.collect.Range;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Is responsible for splitting state space to several chunks that can be processed on each machine separately.
 * Also provides a minimal orthogonal enclosure around assigned state space for purposes of proposition evaluation.
 */
public class HashPartitioner implements StateSpacePartitioner<CoordinateNode> {


    private final int rank;
    private int statesPerMachine;

    @NotNull
    private final OdeModel model;

    private List<Range<Integer>> limit = new ArrayList<>();

    public HashPartitioner(@NotNull OdeModel odeModel, int size, int rank) {
        this.rank = rank;
        this.model = odeModel;

        int stateCount = (int) model.getStateCount();
        //find out how much states a machine will be dealing with
        statesPerMachine = stateCount / size;
        while (statesPerMachine * size <= stateCount) { //guarantee whole space is covered
            statesPerMachine++;
        }
        //compute my variable limits (or something close to them)
        long myLowestState = statesPerMachine * rank;
        long myHighestState = myLowestState + statesPerMachine - 1; // -1 because sPM * (rank + 1) belongs to higher machine
        if (myHighestState > stateCount) {
            myHighestState = stateCount - 1;
        }
        for (int i=0; i < odeModel.getVariableCount(); i++) {
            Range<Integer> range = odeModel.getThresholdRanges().get(i);
            int varRange = range.upperEndpoint() - range.lowerEndpoint();
            if (varRange * model.getDimensionMultiplier(i) <= statesPerMachine) {    //this covers situations when var i is insignificant with respect to current partitioning
                limit.add(range);
            } else {    //we will get here only if number of indexes this variable covers is smaller than her actual span / short: if upperBound > lowerBound, there is less than varRange items between them
                int lowerBound = (int) ((myLowestState / model.getDimensionMultiplier(i)) % varRange);
                int upperBound = (int) ((myHighestState / model.getDimensionMultiplier(i)) % varRange);
                upperBound++;
                //System.out.println(rank+" "+lowerBound+" "+upperBound);
                if (upperBound < lowerBound) {  //sadly, this is how it works
                    limit.add(range);
                } else {
                    if (upperBound >= range.upperEndpoint()) {
                        limit.add(Range.closed(lowerBound, range.upperEndpoint()));
                    } else {
                        limit.add(Range.closed(lowerBound, upperBound));
                    }
                }
            }
        }
        System.out.println(rank+" Partitioner: Total states: "+stateCount+" States per machine: "+statesPerMachine+" My Ranges: "+ Arrays.toString(limit.toArray(new Range[limit.size()])));
    }

    @Override
    public int getNodeOwner(@NotNull CoordinateNode node) throws IllegalArgumentException {
        if (node.getOwner() == -1) {
            int owner = (int) (node.getHash() / statesPerMachine);
            node.setOwner(owner);
            return owner;
        } else {
            return node.getOwner();
        }
    }

    @Override
    public int getMyId() {
        return rank;
    }

    public List<Range<Integer>> getMyLimit() {
        return new ArrayList<>(limit);
    }
}
