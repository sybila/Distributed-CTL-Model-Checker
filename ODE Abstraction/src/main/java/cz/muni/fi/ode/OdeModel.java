package cz.muni.fi.ode;

import com.google.common.collect.Range;
import com.microsoft.z3.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents one ODE model
 */
public class OdeModel {

    private final String filename;

    //Do not touch -- used in jni
    @NotNull
    private final List<Range<Integer>> variableRange = new ArrayList<>();
    @NotNull
    private final List<Range<Double>> parameterRange = new ArrayList<>();
    @NotNull
    private final List<String> variableOrder = new ArrayList<>();
    @NotNull
    private final List<List<Double>> thresholds = new ArrayList<>();
    @NotNull
    private final List<List<SumMember>> equations = new ArrayList<>();

    final List<Range<Integer>> nodeIndexRange = new ArrayList<>();

    //local support data
    private long[] dimensionMultipliers;
    private long stateCount;

    private Context defaultContext = new Context();
    private RealExpr[] contextParameters;
    private Solver defaultSolver = defaultContext.mkSolver();
    private Tactic defaultTactic = defaultContext.mkTactic("ctx-solver-simplify");
    private Goal  defaultGoal = defaultContext.mkGoal(false, false, false);

    private String smtParamDefinition = "";

    public OdeModel(String filename) {
        this.filename = filename;
    }

    public void load() {
        cppLoad(filename);

        for (Range<Integer> range : variableRange) {
            nodeIndexRange.add(Range.closed(range.lowerEndpoint(), range.upperEndpoint() - 1));
        }

        dimensionMultipliers = new long[getVariableCount()];
        stateCount = 1;
        //count all states and prepare ordering
        for (int i=0; i < getVariableCount(); i++) {
            dimensionMultipliers[i] = stateCount;
            Range<Integer> range = nodeIndexRange.get(i);
            stateCount *= range.upperEndpoint() - range.lowerEndpoint() + 1;
        }
        System.err.println("Multipliers: "+ Arrays.toString(dimensionMultipliers));

        if(parameterCount() > 0) {
            contextParameters = new RealExpr[parameterCount()];
            for(int i = 0; i < parameterCount(); i++) {
                contextParameters[i] = defaultContext.mkRealConst("p" + i);
                smtParamDefinition += " ( declare-const p"+i+" Real ) ";
            }
        } else contextParameters = null;

    }

    public String getSmtParamDefinition() {
        return smtParamDefinition;
    }

    public long nodeHash(@NotNull int[] nodeCoordinates) {
        long res = 0;
        for (int i=0; i < dimensionMultipliers.length; i++) {
            res += dimensionMultipliers[i] * nodeCoordinates[i];
        }
        return res;
    }

    public String coordinateString(int[] coordinates) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<coordinates.length; i++) {
            sb
                    .append("[")
                    .append(thresholds.get(i).get(coordinates[i]))
                    .append(",")
                    .append(thresholds.get(i).get(coordinates[i] + 1))
                    .append("]");
            if ( i != coordinates.length - 1 ) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public long getDimensionMultiplier(int dim) {
        return dimensionMultipliers[dim];
    }

    public long getStateCount() {
        return stateCount;
    }

    private native void cppLoad(String filename);

    @NotNull
    public List<Range<Integer>> getThresholdRanges() {
        return variableRange;
    }

    @NotNull
    public List<Range<Double>> getParameterRange() {
        return parameterRange;
    }

    @NotNull
    public ColorFormulae getFullColorSet() {
        // creation of new ColorFormulae with defaultContext as initial parameter and initial constrains
        BoolExpr[] exprs = new BoolExpr[parameterCount()];
        for(int i = 0; i < parameterCount(); i++) {
            Range<Double> range = getParameterRange().get(i);
            RealExpr lower = defaultContext.mkReal(range.lowerEndpoint().toString());
            RealExpr upper = defaultContext.mkReal(range.upperEndpoint().toString());

            // setting bounds on parameter space as intervals
            exprs[i] = defaultContext.mkAnd(defaultContext.mkGe(getContextParameter(i),lower),defaultContext.mkLe(getContextParameter(i),upper));
        }
        return new ColorFormulae(defaultContext, defaultSolver, defaultGoal, defaultTactic, defaultContext.mkAnd(exprs));
    }

    @NotNull
    public ColorFormulae getEmptyColorSet() {

        // creation of new ColorFormulae instance with initial defaultContext parameter with unsatisfiable constrain
        @NotNull ColorFormulae set = new ColorFormulae(defaultContext, defaultSolver, defaultGoal, defaultTactic, defaultContext.mkFalse());
        return set;
    }

    @NotNull
    public Context getDefaultContext() {
        return this.defaultContext;
    }

    public Goal getDefaultGoal() {
        return defaultGoal;
    }

    public Tactic getDefaultTactic() {
        return defaultTactic;
    }

    public Solver getDefaultSolver() {
        return defaultSolver;
    }

    @NotNull
    public RealExpr[] getContextParameters() {
        return this.contextParameters;
    }

    @NotNull
    public RealExpr getContextParameter(int index) {
        return this.contextParameters[index];
    }

    public int getVariableIndexByName(String var) {
        for (int i=0; i<variableOrder.size(); i++) {
            if (var.equals(variableOrder.get(i))) {
                return i;
            }
        }
        throw new IllegalArgumentException(var+" is not a variable of this model. ");
    }

    public String getVariableNameByIndex(int index) {
        return variableOrder.get(index);
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

    public int getThresholdCountForVariable(int varIndex) {
        return thresholds.get(varIndex).size();
    }

    public double getThresholdValueForVariableByIndex(int actualVarIndex, int i) {
        return thresholds.get(actualVarIndex).get(i);
    }
}
