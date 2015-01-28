package cz.muni.fi.ode;

import org.jetbrains.annotations.NotNull;

public class Step {

    final int dim;		//Warning: index of var_name from Model.h but indexing from 1 (not 0)
    private final double theta;
    private final double a;
    private final double b;
    private final boolean positive;

    public Step(int dim, double theta, double a, double b) {
        this(dim, theta, a, b, true);
    }
    public Step(int dim, double theta, double a, double b, boolean positive) {
        this.dim = dim;
        this.theta = theta;
        this.positive = positive;
        if( (positive && a > b) || (!positive && a < b) ) {
            this.a = b;
            this.b = a;
        } else {
            this.a = a;
            this.b = b;
        }
    }

    double value(double value) {
        return value < theta ? a : b;
    }

    @NotNull
    @Override
    public String toString() {
        @NotNull StringBuilder builder = new StringBuilder();
        if (positive) {
            builder.append("H(-)(");
        } else {
            builder.append("H(+)(");
        }
        builder
                .append(dim - 1)
                .append(theta).append(",")
                .append(a).append(",")
                .append(b).append(")");
        return builder.toString();
    }

}