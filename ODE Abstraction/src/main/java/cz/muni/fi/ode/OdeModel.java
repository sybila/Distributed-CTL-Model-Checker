package cz.muni.fi.ode;

import com.google.common.collect.Range;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents one ODE model
 */
public class OdeModel {

    private final String filename;

    //Do not touch -- used in jni
    @NotNull
    private final List<Range<Double>> variableRange = new ArrayList<>();
    @NotNull
    private final List<Range<Double>> parameterRange = new ArrayList<>();
    @NotNull
    private final List<String> variableOrder = new ArrayList<>();
    @NotNull
    private final List<List<Double>> thresholds = new ArrayList<>();
    @NotNull
    private final List<List<SumMember>> equations = new ArrayList<>();

    //local support data
    private long[] dimensionMultipliers;
    private long stateCount;

    public OdeModel(String filename) {
        this.filename = filename;
    }

    public void load() {
        cppLoad(filename);
        dimensionMultipliers = new long[getVariableCount()];
        stateCount = 1;
        //count all states and prepare ordering
        for (int i=0; i < getVariableCount(); i++) {
            dimensionMultipliers[i] = stateCount;
            Range<Double> range = getThresholdRanges().get(i);
            stateCount *= (int) (range.upperEndpoint() - range.lowerEndpoint());
        }
    }

    public long nodeHash(@NotNull int[] nodeCoordinates) {
        long res = 0;
        for (int i=0; i < dimensionMultipliers.length; i++) {
            res += dimensionMultipliers[i] * nodeCoordinates[i];
        }
        return res;
    }

    public long getDimensionMultiplier(int dim) {
        return dimensionMultipliers[dim];
    }

    public long getStateCount() {
        return stateCount;
    }

    private native void cppLoad(String filename);

    @NotNull
    public List<Range<Double>> getThresholdRanges() {
        return variableRange;
    }

    @NotNull
    public List<Range<Double>> getParameterRange() {
        return parameterRange;
    }

    @NotNull
    public TreeColorSet getFullColorSet() {
        @NotNull TreeColorSet set = TreeColorSet.createEmpty(parameterRange.size());
        for (int i = 0; i < set.size(); i++) {
            set.get(i).add(parameterRange.get(i));
        }
        return set;
    }

    public int getVarIndex(String var) {
        for (int i=0; i<variableOrder.size(); i++) {
            if (var.equals(variableOrder.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public int getVariableCount() {
        return variableRange.size();
    }

    public int parameterCount() {
        return parameterRange.size();
    }

    public List<SumMember> getEquationForVariable(int dim) {
        return Collections.unmodifiableList(equations.get(dim));
    }

    public int getThresholdsForVarCount(int varIndex) {
        return thresholds.get(varIndex).size();
    }

    public double getThresholdForVarByIndex(int actualVarIndex, int i) {
        return thresholds.get(actualVarIndex).get(i);
    }
}
