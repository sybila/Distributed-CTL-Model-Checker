package cz.muni.fi.ode;

import org.jetbrains.annotations.NotNull;

public class Ramp {

    // dim is index to model.var_names but incremented by 1. Proper using is model.getVariable(dim -1)
    final int dim;		//Warning: index of var_name from Model.h but indexing from 1 (not 0)
    private final double min;
    private final double max;
    private final double min_value;
    private final double max_value;
    private final boolean negative;

    public Ramp(int dim, double min, double max, double min_value, double max_value) {
        this(dim, min, max, min_value, max_value, false);
    }
    public Ramp(int dim, double min, double max, double min_value, double max_value, boolean negative) {
        this.dim = dim;
        this.min = min;
        this.max = max;
        this.min_value = min_value;
        this.max_value = max_value;
        this.negative = negative;
    }

    double value(double x) {
        if (x >= min && x < max) {
            double res = (x - min) / (max - min);
            if (negative) {
                return min_value - res * Math.abs(max_value - min_value);
            } else {
                return min_value + res * Math.abs(max_value - min_value);
            }
        } else {
            return 0;
        }
    }

    @NotNull
    @Override
    public String toString() {
        @NotNull StringBuilder builder = new StringBuilder();
        if (negative) {
            builder.append("R(-)(");
        } else {
            builder.append("R(+)(");
        }
        builder
                .append(dim - 1).append(",")
                .append(min).append(",")
                .append(max).append(",")
                .append(min_value).append(",")
                .append(max_value).append(")");
        return builder.toString();
    }

}
