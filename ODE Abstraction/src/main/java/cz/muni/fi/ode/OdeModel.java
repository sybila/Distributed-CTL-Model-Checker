package cz.muni.fi.ode;

import com.google.common.collect.Range;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

    private final List<List<Double>> thresholds = new ArrayList<>();

    private final List<List<SumMember>> equations = new ArrayList<>();

    public OdeModel(String filename) {
        this.filename = filename;
    }

    public void load() {
        cppLoad(filename);
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

    public int variableCount() {
        return variableRange.size();
    }

    public int parameterCount() {
        return parameterRange.size();
    }

    public List<SumMember> getEquationForVariable(int dim) {
        return equations.get(dim);
    }

    public double getThresholdForVarByIndex(int actualVarIndex, int i) {
        return thresholds.get(actualVarIndex).get(i);
    }
}
