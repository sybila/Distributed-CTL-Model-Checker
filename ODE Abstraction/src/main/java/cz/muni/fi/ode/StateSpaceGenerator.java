package cz.muni.fi.ode;

import com.google.common.collect.Range;
import com.google.common.math.IntMath;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Generates the state space from provided ODE model.
 */
public class StateSpaceGenerator {

    private final OdeModel model;
    private final boolean parametrized;
    private final NodeFactory factory;

    public StateSpaceGenerator(OdeModel model, boolean parametrized, NodeFactory factory) {
        this.model = model;
        this.parametrized = parametrized;
        this.factory = factory;
      /*  for (int dimension = 0; dimension < model.getVariableCount(); dimension++) {
            System.out.println("EQ 1: "+dimension);
            for (@NotNull SumMember sumMember : model.getEquationForVariable(dimension)) {
                System.out.println(sumMember.toString());
            }
        }*/
    }

    @NotNull
    public Map<CoordinateNode, TreeColorSet> getPredecessors(@NotNull CoordinateNode from, @NotNull TreeColorSet borders) {
        Map<CoordinateNode, TreeColorSet> map = getDirectedEdges(from, borders, false, true);
       // System.out.println("Pred for "+from+" are: "+Arrays.toString(map.entrySet().toArray()));
        //return getDirectedEdges(from, borders, false, true);
        return map;
    }

    @NotNull
    public Map<CoordinateNode, TreeColorSet> getSuccessors(@NotNull CoordinateNode from, @NotNull TreeColorSet borders) {
        return getDirectedEdges(from, borders, true, true);
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

            @NotNull int[][] vertices = getRightVertices(from, dimension, true);

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

            vertices = getRightVertices(from, dimension, false);
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
     * Compute derivation in given vertex for specified dimension.
     * @param vertex
     * @param dim
     * @return
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
                partialSum *= model.getThresholdForVarByIndex(variableIndex - 1, vertex[variableIndex - 1]);
            }

            //multiply partialSum by values of all ramps relevant to this sum member
            for (@NotNull Ramp ramp : sumMember.getRamps()) {
                //ramp, as sum member, indexes variables from 1, therefore -1
                partialSum *= ramp.value(model.getThresholdForVarByIndex(ramp.dim - 1, vertex[ramp.dim - 1]));
            }

            //multiply partialSum by values of all step functions relevant to this sum member
            for (@NotNull Step step : sumMember.getSteps()) {
                //step, as sum member, indexes variables from 1, therefore -1
                partialSum *= step.value(model.getThresholdForVarByIndex(step.dim - 1, vertex[step.dim - 1]));
            }

            if (sumMember.hasParam()) {
                if (!parametrized) {    //in this model checker, parametrized is exclusively true
                    //if we want to ignore parameters, we just add there an average for lower and upper parameter limit
                    Range<Double> param = model.getParameterRange().get(sumMember.getParam() - 1);
                    partialSum *= (param.lowerEndpoint() + param.upperEndpoint()) * 0.5;
                    sum += partialSum;
                } else {
                    //else, we set values for following parameter splitting computation
                    paramIndex = sumMember.getParam() - 1;
                    denom += partialSum;
                }

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
    private int[][] getRightVertices(@NotNull CoordinateNode node, int fixedDimension, boolean lowerThreshold) {
        //TODO: Optimization tip: Computation is done on one thread and all array sizes are fixed based on model,
        //therefore all these arrays don't need to be allocated all over again in every call of this function.

        //number of border vertices that we need to consider in computation
        //n-dimensional node has 2^n vertices, but we have one dimension fixed, therefore -1
        int numberOfAdjacentNodes = IntMath.pow(2, model.getVariableCount() - 1);

        //array of vertex coordinates that we wanted to compute
        @NotNull int[][] results = new int[numberOfAdjacentNodes][model.getVariableCount()];

        //helper array that holds incomplete coordinates during computation
        @NotNull int[] coordinateBuffer = new int[model.getVariableCount()];

        //helper array that specifies whether the dimension is fully computed
        @NotNull boolean[] needsMoreWork = new boolean[model.getVariableCount()];

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

    Map<CoordinateNode, TreeColorSet> initial(FloatProposition proposition, List<Range<Double>> limit) {
        System.out.println("Get initial nodes for: " + proposition);
        int varIndex = model.getVarIndex(proposition.getVariable());
        System.out.println("Var index: "+varIndex);

        int thresIndex = 0;
        for(int i = 0; i < model.getThresholdsForVarCount(varIndex); i++) {
            if(model.getThresholdForVarByIndex(varIndex, i) > proposition.getThreshold()) {
                switch (proposition.getFloatOperator()) {
                    case GT:
                    case LT_EQ:
                        thresIndex = i;
                        break;
                    case LT:
                    case GT_EQ:
                        thresIndex = i-1;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported operator");
                }
                break;
            }
            if(model.getThresholdForVarByIndex(varIndex, i) == proposition.getThreshold()) {
                thresIndex = i;
                break;
            }
        }

        System.out.println("Thresh index: "+thresIndex);

        switch (proposition.getFloatOperator()) {
            case GT_EQ:
            case GT:
                return getRightInitStates(varIndex, thresIndex, model.getThresholdsForVarCount(varIndex) - 1, limit);
            case LT_EQ:
            case LT:
                return getRightInitStates(varIndex, 0, thresIndex, limit);
            default:
                throw new IllegalArgumentException("Unsupported operator");
        }
    }

    private Map<CoordinateNode, TreeColorSet> getRightInitStates(int varIndex, int begin, int end, List<Range<Double>> limit) {

        int[] coors = new int[model.getVariableCount()];
        int[][] thresholdsIndexis = new int[model.getVariableCount()][2];
        //vector<pair<size_t,size_t> > thresholdsIndexis(dataModel.getDims());

        Map<CoordinateNode, TreeColorSet> ss = new HashMap<>();

        if(varIndex != -1) {
            for(int i = 0; i < model.getVariableCount(); i++) {
                if(varIndex == i) {

                    if(limit.get(i).upperEndpoint() < begin || limit.get(i).lowerEndpoint() > end)
                        return ss;

                    if(limit.get(i).lowerEndpoint() < begin)
                        thresholdsIndexis[i][0] = begin;
                    else
                        thresholdsIndexis[i][0] = (int) (double) limit.get(i).lowerEndpoint();

                    if(limit.get(i).upperEndpoint() > end)
                        thresholdsIndexis[i][1] = end;
                    else
                        thresholdsIndexis[i][1] = (int) (double) limit.get(i).upperEndpoint();
                } else {
                    thresholdsIndexis[i][0] = (int) (double) limit.get(i).lowerEndpoint();//0;
                    thresholdsIndexis[i][1] = (int) (double) limit.get(i).upperEndpoint();//dataModel.getThresholdsForVariable(i).size() - 1;
                }
            }
            getRightStatesRecursive(thresholdsIndexis, ss, coors, 0);
        }


        return ss;
    }

    private void getRightStatesRecursive(int[][] thresholdsIndexis, Map<CoordinateNode, TreeColorSet> states, int[] coors, int actIndex) {

        int dims = model.getVariableCount();

        if(actIndex == dims) {
            states.put(factory.getNode(coors), model.getFullColorSet());
            return;
        }

        for(int i = thresholdsIndexis[actIndex][0]; i < thresholdsIndexis[actIndex][1]; ++i) {
            coors[actIndex] = i;
            getRightStatesRecursive(thresholdsIndexis, states, coors, actIndex+1);
        }
    }
}
