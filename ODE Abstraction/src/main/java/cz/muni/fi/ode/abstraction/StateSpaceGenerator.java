package cz.muni.fi.ode.abstraction;

import com.google.common.collect.Range;
import cz.muni.fi.ode.CoordinateNode;
import cz.muni.fi.ode.OdeModel;
import cz.muni.fi.ode.TreeColorSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by daemontus on 09/01/15.
 */
public class StateSpaceGenerator {

    private final OdeModel model;
    private final boolean parametrized;

    public StateSpaceGenerator(OdeModel model, boolean parametrized) {
        this.model = model;
        this.parametrized = parametrized;
    }

    public Map<CoordinateNode, TreeColorSet> getPredecessors(CoordinateNode from, TreeColorSet borders) {
        return getDirectedEdges(from, borders, false, true);
    }

    public Map<CoordinateNode, TreeColorSet> getSuccessors(CoordinateNode from, TreeColorSet borders) {
        return getDirectedEdges(from, borders, true, true);
    }

    private Map<CoordinateNode, TreeColorSet> getDirectedEdges(CoordinateNode from, TreeColorSet border, boolean successors, boolean biggestConvexHullOfParamSubspace) {
        Map<CoordinateNode, TreeColorSet> results = new HashMap<>();

        boolean hasSelfLoop = true;

        for (int v = 0; v < model.variableCount(); v++) {

            boolean lowerPositiveDirection = false;
            boolean lowerNegativeDirection = false;
            boolean upperPositiveDirection = false;
            boolean upperNegativeDirection = false;

            int[][] vertices = getRightVertices(from, v, true);

            List<Double> paramValues;
            int paramIndex = -1;

            double mostRightOneValue = Double.MAX_VALUE;
            double mostLeftOneValue = Double.MIN_VALUE;
            double derivationValue = 0.0;

            // cycle for every vertices in lower (n-1)-dimensional facet of this state
            for(int i = 0; i < vertices.length; i++) {

//                paramIndex = -1;
                double denom = 0.0;

                derivationValue = value(vertices[i],v,paramIndex,denom);

                if(paramIndex != -1) {

                    if(Math.abs(denom) != 0) {

                        paramValues.add(derivationValue/(-denom) == -0 ? 0 : derivationValue/(-denom));
                        //cerr << dataModel.getParamName(paramIndex) << " = " << derivationValue << "/" << -denom << " = " << paramValues.back() << endl;

                        if(border.get(paramIndex).isEmpty()) {
                            throw new IllegalStateException("Error: no interval for parameter "+paramIndex);
                        }

                        // lowest and highest values of parameter space for chosen variable
                        //TODO: find what span is
                        Range<Double> paramBounds = border.get(paramIndex).span();

                        // works for (paramValues.back() < 0),  (paramValues.back() > 0) and (paramValues.back() == 0)
                        if(denom < 0) {
                            Double lastParamValue = paramValues.get(paramValues.size()-1);
                            if(!successors && paramBounds.lowerEndpoint() <= lastParamValue) {
                                lowerPositiveDirection = true;
                                if( mostRightOneValue == Double.MAX_VALUE ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue) )
                                mostRightOneValue = lastParamValue;
                            }
                            if(successors && paramBounds.upperEndpoint() >=lastParamValue) {
                                lowerNegativeDirection = true;

                                if( mostLeftOneValue == numeric_limits<double>::lowest() ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > paramValues.back()) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < paramValues.back()) )
                                mostLeftOneValue = paramValues.back();
                            }
                        } else { // denom > 0
                            if(isSucc && lowestParamValue <= paramValues.back()) {
                                lowerNegativeDirection = true;

                                if( mostRightOneValue == numeric_limits<double>::max() ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < paramValues.back()) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > paramValues.back()) )
                                mostRightOneValue = paramValues.back();
                            }
                            if(!isSucc && highestParamValue >= paramValues.back()) {
                                lowerPositiveDirection = true;

                                if( mostLeftOneValue == numeric_limits<double>::lowest() ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > paramValues.back()) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < paramValues.back()) )
                                mostLeftOneValue = paramValues.back();
                            }
                        }

                    } else {	// abs(denom) == 0 (ERGO: it might be at border of state space)
                        //cerr << "derivation = " << derivationValue << " --> parameter unknown" << endl;
                        if(derivationValue < 0) {
                            lowerNegativeDirection = true;

                            if(isSucc) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : numeric_limits<double>::lowest()); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : numeric_limits<double>::max()); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        } else {
                            lowerPositiveDirection = true;

                            if(!isSucc) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : numeric_limits<double>::lowest()); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : numeric_limits<double>::max()); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        }
                    }
                } else {	// paramIndex == -1 (ERGO: no unknown parameter in equation)
                    //cerr << "derivation = " << derivationValue << endl;
                    if(derivationValue < 0) {
                        lowerNegativeDirection = true;
                    } else {
                        lowerPositiveDirection = true;
                    }
                }

            }
        }
    }

    private double value(int[] vertex, int dim, int paramIndex, double denom) {
        double sum = 0;

        paramIndex = -1;

        List<SumMember> equation = model.getEquationForVariable(dim);
        for (SumMember sumMember : equation) {
            //adding value of constant in actual summember 's' of equation for variable 'dim'
            double underSum = sumMember.getConstant();

            //if(dbg) std::cerr << "\tconstant is " << underSum << "\n";

            //adding values of variables in actual summember 's' of equation for variable 'dim' in given point
            List<Integer> vars = sumMember.getVars();
            for (Integer var : vars) {
                int actualVarIndex = var - 1;
                double thres = model.getThresholdForVarByIndex(actualVarIndex, vertex[actualVarIndex]);
                //if(dbg) std::cerr << "\tthres for var " << actualVarIndex << " is " << thres << "\n";
                underSum *= thres;
            }

            //cerr << "start of evalueting of ramps\n";

            //adding enumerated ramps for actual summember 's' of equation for variable 'dim'
            for (SumMember.Ramp ramp : sumMember.getRamps()) {
                //cerr << "ramp: " << dataModel.getSumForVarByIndex(dim, s).GetRamps().at(r) << endl;
                int rampVarIndex = ramp.dim - 1;
                //cerr << "ramp var index: " << rampVarIndex << endl;
                double thres = model.getThresholdForVarByIndex(rampVarIndex, vertex[rampVarIndex]);
                //cerr << "thres for this var: " << thres << endl;
                underSum *= ramp.value(thres);
                //cerr << "local underSum = " << underSum << endl;
            }

            //adding enumerated step functions for actual summember 's' of equation for variable 'dim'
            for (SumMember.Step step : sumMember.getSteps()) {
                int stepVarIndex = step.dim - 1;
                double thres = model.getThresholdForVarByIndex(stepVarIndex, vertex[stepVarIndex]);
                underSum *= step.value(thres);
            }

            //adding average value of actual summember's parameter, if any exists
            if (sumMember.hasParam()) {
                if (!parametrized) {
                    Range<Double> param = model.getParameterRange().get(sumMember.getParam() - 1);
                    underSum *= (param.lowerEndpoint() + param.upperEndpoint()) * 0.5;
                    //adding enumerated summember 's' to sum
                    sum += underSum;
                } else {
                    denom += underSum;
                }

            } else {
                //adding enumerated summember 's' to sum
                sum += underSum;
            }

        }
        //if ( dbg ) std::cerr << "final value = " << sum << std::endl;
        return sum;
    }

    private int[][] getRightVertices(CoordinateNode from, int dim, boolean lower) {
        List<List<Integer>> vertices = new ArrayList<>();
        List<Integer> coors = new ArrayList<>();

        getRightVerticesRecursive(from, dim, lower, vertices, coors, 0);

        int[][] res = new int[vertices.size()][coors.size()];
        for (int i = 0; i < vertices.size(); i++) {
            List<Integer> vertex = vertices.get(i);
            for (int j = 0; j < vertex.size(); j++) {
                res[i][j] = vertex.get(j);
            }
        }
        return res;
    }

    private void getRightVerticesRecursive(CoordinateNode from, int dim, boolean lower, List<List<Integer>> vertices, List<Integer> coors, int actIndex) {
        if (actIndex == model.variableCount()) {
            vertices.add(new ArrayList<>(coors));
            return;
        }

        if (actIndex == dim) {
            if (lower) {
                coors.set(actIndex, from.coordinates[actIndex]);
            } else {
                coors.set(actIndex, from.coordinates[actIndex] + 1);
            }
            getRightVerticesRecursive(from, dim, lower, vertices, coors, actIndex+1);
        } else {
            for(int i = from.coordinates[actIndex]; i < from.coordinates[actIndex] + 2; ++i) {
                coors.set(actIndex, i);
                getRightVerticesRecursive(from, dim, lower, vertices, coors, actIndex+1);
            }
        }
    }
}
