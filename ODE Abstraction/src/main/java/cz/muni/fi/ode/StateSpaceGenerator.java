package cz.muni.fi.ode;

import com.google.common.collect.Range;
import com.google.common.math.IntMath;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Generates the state space from provided ODE model.
 * The provided node factory is used as a means of caching and organizing nodes.
 * The generated state space is restrained by given bounds.
 */
public class StateSpaceGenerator {

    private final OdeModel model;
    private final NodeFactory factory;
    private final List<Range<Integer>> coordinateBounds;

    /**
     * These arrays are used in every successor/predecessor computation with fixed size.
     * We don't want them to be allocated every time all over again, so we just init them in constructor.
     */

    //array of vertex coordinates that we wanted to compute
    @NotNull int[][] results;

    //helper array that holds incomplete coordinates during computation
    @NotNull int[] coordinateBuffer;

    //helper array that specifies whether the dimension is fully computed
    @NotNull boolean[] needsMoreWork;


    /**
     * @param model ODE model with proper abstraction used as a source of data.
     * @param factory Node factory used as storage for new nodes.
     * @param coordinateBounds Bounds for state space generator represented by list of !threshold indexes. (Last index is therefore considered as upper bound of last state)
     */
    public StateSpaceGenerator(OdeModel model, NodeFactory factory, List<Range<Integer>> coordinateBounds) {
        this.model = model;
        this.factory = factory;
        this.coordinateBounds = coordinateBounds;

        //number of border vertices that we need to consider in computation
        //n-dimensional node has 2^n vertices, but we have one dimension fixed, therefore -1
        int numberOfAdjacentNodes = IntMath.pow(2, model.getVariableCount() - 1);
        results = new int[numberOfAdjacentNodes][model.getVariableCount()];
        coordinateBuffer = new int[model.getVariableCount()];
        needsMoreWork = new boolean[model.getVariableCount()];

    }

    /**
     * Compute predecessors of given node. All transitions are bounded by given parameter set.
     * @param from Source node.
     * @param borders Parametric borders.
     * @return A set of transitions in form of a map with "predecessor->transition borders" pairs.
     */
    @NotNull
    public Map<CoordinateNode, TreeColorSet> getPredecessors(@NotNull CoordinateNode from, @NotNull TreeColorSet borders) {
        return getDirectedEdges(from, borders, false, true);
    }

    /**
     * Compute successors of given node. All transitions are bounded by given parameter set.
     * @param from Source node.
     * @param borders Parametric borders.
     * @return A set of transitions in form of a map with "successor->transition borders" pairs.
     */
    @NotNull
    public Map<CoordinateNode, TreeColorSet> getSuccessors(@NotNull CoordinateNode from, @NotNull TreeColorSet borders) {
        return getDirectedEdges(from, borders, true, true);
    }

    /**
     * Compute initial states constrained by coordinates given in constructor and float condition in form of proposition.
     * @param proposition A proposition for which the initial nodes should be calculated.
     * @return A collection of nodes where given float proposition is satisfied.
     */
    public @NotNull Collection<CoordinateNode> initial(@NotNull FloatProposition proposition) {

        //translate the variable name from proposition into model variable index
        int variableIndex = model.getVariableIndexByName(proposition.getVariable());

        //find index of threshold where proposition valuation changes
        int thresholdIndex = 0;
        for(int i = 0; i < model.getThresholdCountForVariable(variableIndex); i++) {
            if(model.getThresholdValueForVariableByIndex(variableIndex, i) > proposition.getThreshold()) {
                switch (proposition.getFloatOperator()) {
                    case GT:
                    case LT_EQ:
                        thresholdIndex = i;
                        break;
                    case LT:
                    case GT_EQ:
                        thresholdIndex = i-1;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported operator.");
                }
                break;
            }
            if(model.getThresholdValueForVariableByIndex(variableIndex, i) == proposition.getThreshold()) {
                thresholdIndex = i;
                break;
            }
        }

        //Return nodes with right combination of thresholds.
        switch (proposition.getFloatOperator()) {
            case GT_EQ:
            case GT:
                return enumerateStates(variableIndex, thresholdIndex, model.getThresholdCountForVariable(variableIndex) - 1);
            case LT_EQ:
            case LT:
                return enumerateStates(variableIndex, 0, thresholdIndex);
            default:
                throw new IllegalArgumentException("Unsupported operator.");
        }
    }

    /**
     * Place all nodes given by global constrains into the storage.
     */
    public void cacheAllNodes() {
        enumerateStates(0, 0, model.getThresholdCountForVariable(0) - 1);
    }

    private int paramIndex;
    private double denom;

    @NotNull
    private Map<CoordinateNode, TreeColorSet> getDirectedEdges(@NotNull CoordinateNode from, @NotNull TreeColorSet border, boolean successors, boolean biggestConvexHullOfParamSubspace) {
        @NotNull Map<CoordinateNode, TreeColorSet> results = new HashMap<>();

        boolean hasSelfLoop = true;

        //go through all dimension of model and compute results for each of them separately
        for (int dimension = 0; dimension < model.getVariableCount(); dimension++) {

            boolean lowerPositiveDirection = false;
            boolean lowerNegativeDirection = false;
            boolean upperPositiveDirection = false;
            boolean upperNegativeDirection = false;

            @NotNull int[][] vertices = computeBorderVerticesForState(from, dimension, true);

          /*  System.out.println("Vertices: ");
            for (int[] vertex : vertices) {
                for (int a : vertex) {
                    System.out.print(a + " ");
                }
                System.out.println();
            }*/

            @NotNull List<Double> paramValues = new ArrayList<>();
            paramIndex = -1;

            double mostRightOneValue = Double.POSITIVE_INFINITY;
            double mostLeftOneValue = Double.NEGATIVE_INFINITY;
            double derivationValue = 0.0;

            // cycle for every vertices in lower (n-1)-dimensional facet of this state
            for (int[] vertex : vertices) {

//                paramIndex = -1;
                denom = 0.0;

                //TODO: Don't we need to "normalize" the derivation value by old value?
                derivationValue = value(vertex, dimension);
              //  System.out.println(Arrays.toString(vertex)+"/"+dimension+": "+(-derivationValue/denom));
               // System.out.println("Derivation " + derivationValue);
                if (paramIndex != -1) {

                    if (Math.abs(denom) != 0) {

                        paramValues.add((-derivationValue/denom) == -0 ? 0 : (-derivationValue/denom));
                        //cerr << dataModel.getParamName(paramIndex) << " = " << derivationValue << "/" << -denom << " = " << paramValues.back() << endl;

                        if (border.get(paramIndex).isEmpty()) {
                            throw new IllegalStateException("Error: no interval for parameter " + paramIndex);
                        }

                        // lowest and highest values of parameter space for chosen variable
                        Range<Double> paramBounds = border.get(paramIndex).span();
                       // System.out.println("Bounds: "+paramBounds.lowerEndpoint()+" "+paramBounds.upperEndpoint());
                        // works for (paramValues.back() < 0),  (paramValues.back() > 0) and (paramValues.back() == 0)
                        Double lastParamValue = paramValues.get(paramValues.size() - 1);
                        if(successors) {
                            if(denom < 0 && paramBounds.upperEndpoint() >= lastParamValue) {
                                lowerNegativeDirection = true;

                                if( mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue) )
                                mostLeftOneValue = lastParamValue;
                            }
                            if(derivationValue < 0 && denom > 0 && paramBounds.lowerEndpoint() <= lastParamValue) {
                                lowerNegativeDirection = true;

                                if( mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue) )
                                mostRightOneValue = lastParamValue;
                            }
                        } else {	// ! isSucc
                            if(denom > 0 && paramBounds.upperEndpoint() >= lastParamValue) {
                                lowerPositiveDirection = true;

                                if( mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue) )
                                mostLeftOneValue = lastParamValue;
                            }
                            if(derivationValue > 0 && denom < 0 && paramBounds.lowerEndpoint() <= lastParamValue) {
                                lowerPositiveDirection = true;

                                if( mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue) )
                                mostRightOneValue = lastParamValue;
                            }
                        }
                       /* if (denom < 0) {
                            if (!successors && paramBounds.lowerEndpoint() <= lastParamValue) {
                                lowerPositiveDirection = true;
                                if (mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue))
                                    mostRightOneValue = lastParamValue;
                            }
                            if (successors && paramBounds.upperEndpoint() >= lastParamValue) {
                                lowerNegativeDirection = true;

                                if (mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue))
                                    mostLeftOneValue = lastParamValue;
                            }
                        } else { // denom > 0
                            if (successors && paramBounds.lowerEndpoint() <= lastParamValue) {
                                lowerNegativeDirection = true;

                                if (mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue))
                                    mostRightOneValue = lastParamValue;
                            }
                            if (!successors && paramBounds.upperEndpoint() >= lastParamValue) {
                                lowerPositiveDirection = true;

                                if (mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue))
                                    mostLeftOneValue = lastParamValue;
                            }
                        }*/

                    } else {    // abs(denom) == 0 (ERGO: it might be at border of state space)
                        //cerr << "derivation = " << derivationValue << " --> parameter unknown" << endl;

                        if(successors) {
                            if(derivationValue < 0) {
                                lowerNegativeDirection = true;
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY);
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY);
                            }
                        } else {	// ! isSucc
                            if(derivationValue > 0) {
                                lowerPositiveDirection = true;
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY);
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY);
                            }
                        }
                        /*if (derivationValue < 0) {
                            lowerNegativeDirection = true;

                            if (successors) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        } else {
                            lowerPositiveDirection = true;

                            if (!successors) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        }*/
                    }
                } else {    // paramIndex == -1 (ERGO: no unknown parameter in equation)
                    //cerr << "derivation = " << derivationValue << endl;
                    if (derivationValue < 0) {
                        lowerNegativeDirection = true;
                    } else {
                        lowerPositiveDirection = true;
                    }
                }

            }


            //cerr << "most left  param value on lower facet: " << mostLeftOneValue << endl;
            //cerr << "most right param value on lower facet: " << mostRightOneValue << endl;

            if(from.coordinates[dimension] != 0)	{
                if(!successors) {
                    //If I want predecessors of state 's'

                    if(lowerPositiveDirection) {
                        //There exists edge from lower state to state 's'
                        @NotNull int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                        newStateCoors[dimension] = newStateCoors[dimension] - 1;

                        TreeColorSet newPS;
                        if(paramIndex != -1) {
                            //Parameter space needs to be cut for this edge
                            //						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
                            newPS = TreeColorSet.derivedColorSet(border, paramIndex, mostLeftOneValue, mostRightOneValue);
                        } else {
                            //Edge is for whole parameter space
                            newPS = TreeColorSet.createCopy(border);
                        }

                        results.put(factory.getNode(newStateCoors), newPS);
                    }
                } else {
                    //If I want successors of state 's'
                    if(lowerNegativeDirection) {
                        //There exists edge from lower state to state 's'

                        @NotNull int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                        newStateCoors[dimension] = newStateCoors[dimension] - 1;

                        TreeColorSet newPS;
                        if(paramIndex != -1) {
                            //Parameter space needs to be cut for this edge
                            //						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
                            newPS = TreeColorSet.derivedColorSet(border, paramIndex, mostLeftOneValue, mostRightOneValue);
                        } else {
                            //Edge is for whole parameter space
                            newPS = TreeColorSet.createCopy(border);
                        }

                        results.put(factory.getNode(newStateCoors), newPS);
                    }
                }
            }

            //I want to check upper state in this dimension only if state 's' is not at the top in this dimension

            vertices = computeBorderVerticesForState(from, dimension, false);
           /* System.out.println("Vertices: ");
            for (int[] vertex : vertices) {
                for (int a : vertex) {
                    System.out.print(a + " ");
                }
                System.out.println();
            }*/

            paramValues = new ArrayList<>();
            paramIndex = -1;
            mostRightOneValue = Double.POSITIVE_INFINITY;
            mostLeftOneValue = Double.NEGATIVE_INFINITY;
            derivationValue = 0.0;

            // cycle for every vertices in higher (n-1)-dimensional facet of this state
            for (int[] vertex : vertices) {

//				paramIndex = -1;
                denom = 0.0;

                derivationValue = value(vertex, dimension);
                //System.out.println(Arrays.toString(vertex)+"/"+dimension+": "+(-derivationValue/denom));
                if (paramIndex != -1) {

                    if (Math.abs(denom) != 0) {
                        paramValues.add((-derivationValue/denom) == -0 ? 0 : -derivationValue/denom);
                        //cerr << dataModel.getParamName(paramIndex) << " = " << derivationValue << "/" << -denom << " = " << paramValues.back() << endl;

                        if (border.get(paramIndex).isEmpty()) {
                            throw new IllegalStateException("Error: no interval for parameter " + paramIndex);
                        }

                        // lowest and highest values of parameter space for chosen variable
                        Range<Double> paramBounds = border.get(paramIndex).span();

                        // works for (paramValues.back() < 0),  (paramValues.back() > 0) and (paramValues.back() == 0)
                        Double lastParamValue = paramValues.get(paramValues.size() - 1);
                        if(!successors) {
                            if(denom < 0 && paramBounds.upperEndpoint() >= lastParamValue) {
                                upperNegativeDirection = true;

                                if( mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue) )
                                mostLeftOneValue = lastParamValue;
                            }
                            if(derivationValue < 0 && denom > 0 && paramBounds.lowerEndpoint() <= lastParamValue) {
                                upperNegativeDirection = true;

                                if( mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue) )
                                mostRightOneValue = lastParamValue;
                            }
                        } else {	// isSucc
                            if(denom > 0 && paramBounds.upperEndpoint() >= lastParamValue) {
                                upperPositiveDirection = true;

                                if( mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue) )
                                mostLeftOneValue = lastParamValue;
                            }
                            if(derivationValue > 0 && denom < 0 && paramBounds.lowerEndpoint() <= lastParamValue) {
                                upperPositiveDirection = true;

                                if( mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue) )
                                mostRightOneValue = lastParamValue;
                            }
                        }
                        /*if (denom < 0) {
                            if (successors && paramBounds.lowerEndpoint() <= lastParamValue) {
                                upperPositiveDirection = true;

                                if (mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue))
                                mostRightOneValue = lastParamValue;
                            }
                            if (!successors && paramBounds.upperEndpoint() >= lastParamValue) {
                                upperNegativeDirection = true;

                                if (mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue))
                                mostLeftOneValue = lastParamValue;
                            }
                        } else { // denom > 0
                            if (!successors && paramBounds.lowerEndpoint() <= lastParamValue) {
                                upperNegativeDirection = true;

                                if (mostRightOneValue == Double.POSITIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostRightOneValue < lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostRightOneValue > lastParamValue))
                                mostRightOneValue = lastParamValue;
                            }
                            if (successors && paramBounds.upperEndpoint() >= lastParamValue) {
                                upperPositiveDirection = true;

                                if (mostLeftOneValue == Double.NEGATIVE_INFINITY ||
                                        (biggestConvexHullOfParamSubspace && mostLeftOneValue > lastParamValue) ||
                                        (!biggestConvexHullOfParamSubspace && mostLeftOneValue < lastParamValue))
                                mostLeftOneValue = lastParamValue;
                            }
                        }*/


                    } else {    // abs(denom) == 0 (ERGO: it might be at border of state space)

                        if(!successors) {
                            if(derivationValue < 0) {
                                upperNegativeDirection = true;
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY);
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY);
                            }
                        } else {	// ! isSucc
                            if(derivationValue > 0) {
                                upperPositiveDirection = true;
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY);
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY);
                            }
                        }

                        //	cerr << "derivation = " << derivationValue << " --> parameter unknown" << endl;
                        /*if (derivationValue < 0) {
                            upperNegativeDirection = true;

                            if (!successors) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        } else {
                            upperPositiveDirection = true;

                            if (successors) {
                                mostLeftOneValue = (biggestConvexHullOfParamSubspace ? -100000.0 : Double.NEGATIVE_INFINITY); //TODO: mozno nahradit (-100000.0) za (numeric_limits<double>::lowest())+1)
                                mostRightOneValue = (biggestConvexHullOfParamSubspace ? 100000.0 : Double.POSITIVE_INFINITY); //TODO: mozno nahradit (100000.0) za (numeric_limits<double>::max())-1)
                            }
                        }*/
                    }

                } else {    // paramIndex == -1 (ERGO: no unknown parameter in equation)
                    //	cerr << "derivation = " << derivationValue << endl;
                    if (derivationValue < 0) {
                        upperNegativeDirection = true;
                    } else {
                        upperPositiveDirection = true;
                    }
                }
            }

            //cerr << "most left  param value on upper facet: " << mostLeftOneValue << endl;
            //cerr << "most right param value on upper facet: " << mostRightOneValue << endl;

            if(from.coordinates[dimension] != model.getThresholdRanges().get(dimension).upperEndpoint() - 1) {
                if(!successors) {
                    //If I want predecessors of state 's'

                    if(upperNegativeDirection) {
                        //There exists edge from lower state to state 's'
                        @NotNull int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                        newStateCoors[dimension] = newStateCoors[dimension] + 1;

                        TreeColorSet newPS;
                        if(paramIndex != -1) {
                            //Parameter space needs to be cut for this edge
                            //						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
                            newPS = TreeColorSet.derivedColorSet(border, paramIndex, mostLeftOneValue, mostRightOneValue);
                        } else {
                            //Edge is for whole parameter space
                            newPS = TreeColorSet.createCopy(border);
                        }

                        results.put(factory.getNode(newStateCoors), newPS);

                    }
                } else {
                    //If I want successors of state 's'

                    if(upperPositiveDirection) {
                        //There exists edge from lower state to state 's'

                        @NotNull int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                        newStateCoors[dimension] = newStateCoors[dimension] + 1;

                        TreeColorSet newPS;
                        if(paramIndex != -1) {
                            //Parameter space needs to be cut for this edge
                            //						newPS = ParameterSpace::derivedParamSpace(s.getColors(),paramIndex,oneParamValue);
                            newPS = TreeColorSet.derivedColorSet(border, paramIndex, mostLeftOneValue, mostRightOneValue);
                        } else {
                            //Edge is for whole parameter space
                            newPS = TreeColorSet.createCopy(border);
                        }

                        results.put(factory.getNode(newStateCoors), newPS);

                    }
                }
            }

            if(hasSelfLoop) {
                if(lowerPositiveDirection && upperPositiveDirection && !lowerNegativeDirection && !upperNegativeDirection ||
                        !lowerPositiveDirection && !upperPositiveDirection && lowerNegativeDirection && upperNegativeDirection) {

                    hasSelfLoop = false;
                }
            }
        }

        if(hasSelfLoop) {
            results.put(from, border);
        }

        return results;
    }

    /**
     * Compute derivation in given vertex for specified dimension by evaluating the function for given variable.
     * TODO: This function has side effect of modifying paramIndex and denom variables. Remove this.
     * @param vertex Coordinates of thresholds where the computation should be performed.
     * @param dim Index of variable whose function should be evaluated.
     * @return Derivation of specified variable in given vertex as given by function specified in the model.
     */
    private double value(int[] vertex, int dim) {
        double sum = 0;

        paramIndex = -1;

        for (@NotNull SumMember sumMember : model.getEquationForVariable(dim)) {
            //init partial sum to constant part of the equation member
            double partialSum = sumMember.getConstant();

            //multiply partialSum by actual value of every threshold relevant to this sum member
            for (Integer variableIndex : sumMember.getVars()) {
                //sum member indexes it's variables from 1, so we need to subtract 1 to fit model
                partialSum *= model.getThresholdValueForVariableByIndex(variableIndex - 1, vertex[variableIndex - 1]);
            }

            //multiply partialSum by values of all ramps relevant to this sum member
            for (@NotNull Ramp ramp : sumMember.getRamps()) {
                //ramp, as sum member, indexes variables from 1, therefore -1
                partialSum *= ramp.value(model.getThresholdValueForVariableByIndex(ramp.dim - 1, vertex[ramp.dim - 1]));
            }

            //multiply partialSum by values of all step functions relevant to this sum member
            for (@NotNull Step step : sumMember.getSteps()) {
                //step, as sum member, indexes variables from 1, therefore -1
                partialSum *= step.value(model.getThresholdValueForVariableByIndex(step.dim - 1, vertex[step.dim - 1]));
            }

            if (sumMember.hasParam()) {
                //we set values for following parameter splitting computation
                paramIndex = sumMember.getParam() - 1;
                denom += partialSum;
            } else {
                //if sum member does not have a parameter, just add all of this to the final sum
                sum += partialSum;
            }

        }

        return sum;
    }

    /**
     * Compute coordinates of all border vertices of given node when one dimension is fixed
     * to only lower or higher threshold.
     * (Vertex - coordinates of thresholds; Node - coordinates of "space" between several vertices.)
     *
     * @param node Questioned node.
     * @param fixedDimension Index of fixed dimension.
     * @param lowerThreshold If true, only lower threshold is considered on fixed dimension. Higher otherwise.
     * @return Array of vertex coordinates that match given constrains.
     */
    @NotNull
    private int[][] computeBorderVerticesForState(@NotNull CoordinateNode node, int fixedDimension, boolean lowerThreshold) {

        int activeIndex = 0;        //which dimension of coordinates we are working on
        int resultCounter = 0;      //index in results where we want to write final coordinates
        //this is basically simulating recursion using the needsMoreWork array.
        //magic begins here
        while (activeIndex >= 0) {
            if (activeIndex == model.getVariableCount()) {
                //if all dimensions are processed, copy buffer into results and move to lower dimension
                System.arraycopy(coordinateBuffer, 0, results[resultCounter], 0, model.getVariableCount());
                resultCounter++;
                activeIndex--;
                //skip dimensions that are already completed
                while (activeIndex >= 0 && !needsMoreWork[activeIndex]) {
                    activeIndex--;
                }
            } else if (activeIndex == fixedDimension) {
                //if we are working on fixed dimension, we do not process both lower and higher thresholds,
                //we decide based on parameter whether we want strictly lower or higher
                if (lowerThreshold) {
                    //index of lower node threshold
                    coordinateBuffer[activeIndex] = node.coordinates[activeIndex];
                } else {
                    //index of higher node threshold
                    coordinateBuffer[activeIndex] = node.coordinates[activeIndex] + 1;
                }
                needsMoreWork[activeIndex] = false;
                activeIndex++;  //move to higher dimension
            } else {
                //if we are working on any general dimension, we first process all lower thresholds and
                //mark dimension as not completed. When all lower thresholds are prepared, we process all higher
                //thresholds and only after that mark the dimension as done.
                if (!needsMoreWork[activeIndex]) {
                    needsMoreWork[activeIndex] = true;
                    coordinateBuffer[activeIndex] = node.coordinates[activeIndex];
                } else {
                    needsMoreWork[activeIndex] = false;
                    coordinateBuffer[activeIndex] = node.coordinates[activeIndex] + 1;
                }
                activeIndex++; //move to higher dimension
            }
        }

        return results;
    }

    /**
     * Return all states of the model restricted on given dimension by threshold index interval.
     * On all other dimensions, every possible threshold is combined in result.
     * (With respect to global coordinate bounds)
     * @param variableIndex Index of variable where state space is split.
     * @param lowerBound Index of lowest threshold that should be returned.
     * @param upperBound Index of highest threshold that should be returned.
     * @return List of nodes matching requested criteria.
     */
    private Collection<CoordinateNode> enumerateStates(int variableIndex, int lowerBound, int upperBound) {

        //compared to computeBorderVerticesForState() which is called for every state multiple times,
        //this method is called only once when proposition is evaluated.
        //So any performance aspect of buffer allocation is not really a concern here.

        List<CoordinateNode> results = new ArrayList<>();

        //account the global bounds in given threshold interval so that it does not have to be considered in future computations.
        lowerBound = Math.max(lowerBound, coordinateBounds.get(variableIndex).lowerEndpoint());
        upperBound = Math.min(upperBound, coordinateBounds.get(variableIndex).upperEndpoint());

        //helper array that holds incomplete coordinates during computation
        int[] coordinateBuffer = new int[model.getVariableCount()];

        //helper array that specifies whether the dimension is fully computed
        //A non negative number is a index that still needs to be processed.
        //Negative number means everything is done.
        int[] remainingWork = new int[model.getVariableCount()];
        Arrays.fill(remainingWork, -1);

        int activeIndex = 0;    //dimension of model being currently explored
        while (activeIndex >= 0) {
            if (activeIndex == model.getVariableCount()) {
                //if all dimensions are processed, put resulting node in the collection
                results.add(factory.getNode(coordinateBuffer));
                activeIndex--;
                //skip dimensions that are already completed
                while (activeIndex >= 0 && remainingWork[activeIndex] < 0) {
                    activeIndex--;
                }
            } else if (activeIndex == variableIndex) {
                //if we are working on restricted interval, we do not want to process all thresholds, just
                //the ones within restricted bounds, so we add extra conditions to account for that.

                if (remainingWork[activeIndex] < 0) {
                    remainingWork[activeIndex] = upperBound - 1; //-1 for upper node threshold
                }

                coordinateBuffer[activeIndex] = remainingWork[activeIndex];
                remainingWork[activeIndex] -= 1;
                if (remainingWork[activeIndex] < lowerBound) {
                    remainingWork[activeIndex] = -1;
                }

                activeIndex++;  //move to higher dimension
            } else {
                //if we are working on any general dimension, we start from highest threshold index and work
                //toward zero or global bound. After that, dimension is done and is marked accordingly (-1)

                if (remainingWork[activeIndex] < 0) {   //if this is true, we are coming from lower dimension and we need to init new search
                    remainingWork[activeIndex] = coordinateBounds.get(activeIndex).upperEndpoint() - 1; //-1 for upper node threshold
                }

                coordinateBuffer[activeIndex] = remainingWork[activeIndex];
                remainingWork[activeIndex] -= 1;
                if (remainingWork[activeIndex] < coordinateBounds.get(activeIndex).lowerEndpoint()) {
                    remainingWork[activeIndex] = -1;
                }

                activeIndex++; //move to higher dimension
            }
        }

        return results;
    }

}
