package cz.muni.fi.ode;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SumMember {

    private final double constant;
    //Warning: param contains index to Model.param_names and Model.param_ranges vectors, BUT incremented by 1
    //(Proper using is Model.getParamName(param-1))
    private final int param;
    //Warning: vars contains indexes to Model.var_names vector, BUT incremented by 1 (Proper using is Model.getVariable(vars.at(i)-1))
    private final List<Integer> vars;
    private final List<Ramp> ramps;
    //Warning: sigmoids contains indexes to Model.sigmoids vector, BUT incremented by 1 (Proper using is Model.getSigmoids().at(sigmoids.at(i)-1))
    //private List<Integer> sigmoids;
    private final List<Step> steps;
    //Warning: hills contains indexes to Model.hills vector, BUT incremented by 1 (Proper using is Model.getHills().at(hills.at(i)-1))
    //private List<Integer> hills;

    public SumMember(double constant, int param, List<Integer> vars, List<Ramp> ramps, List<Step> steps) {
        this.constant = constant;
        this.param = param;
        this.vars = vars;
        this.ramps = ramps;
        this.steps = steps;
    }

    public double getConstant() {
        return constant;
    }

    public List<Integer> getVars() {
        return vars;
    }

    public List<Ramp> getRamps() {
        return ramps;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public boolean hasParam() {
        return param != 0;
    }

    public int getParam() {
        return param;
    }

    @NotNull
    @Override
    public String toString() {
        @NotNull StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(constant);
        if (hasParam()) {
            stringBuilder.append("*Param(").append(getParam() - 1).append(")");
        }
        for (@NotNull Ramp ramp : ramps) {
            stringBuilder.append("*").append(ramp.toString());
        }
        for (@NotNull Step step : steps) {
            stringBuilder.append("*").append(step.toString());
        }
        for (Integer var : vars) {
            stringBuilder.append("*Var(").append(var - 1).append(")");
        }
        return stringBuilder.toString();
    }
}
