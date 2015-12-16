package cz.muni.fi.ode;

import com.google.common.collect.Range;
import com.google.common.math.IntMath;
import com.microsoft.z3.*;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import cz.muni.fi.modelchecker.graph.ColorSet;
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
    @NotNull private final int[][] results;

    //helper array that holds incomplete coordinates during computation
    @NotNull private final int[] coordinateBuffer;

    //helper array that specifies whether the dimension is fully computed
    @NotNull private final boolean[] needsMoreWork;


    //global data used by calculateValue function
    //global variables are ugly, but we need this to be really fast
    //(more info in constructor)
    private Double[] equationConsts;

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

        // equation constants are evaluated constants each for one parameter (regardless of being part of particular
        // equation - in that case the constant will be zero) and one (the last one) without parameter
        this.equationConsts = new Double[model.parameterCount()+1];
        for(int i = 0 ; i < model.parameterCount()+1; i++) {
            this.equationConsts[i] = 0.0;
        }
/*
        // aliases for all parameters in model will be used in Z3 SMT-solver as variables ( ctx.mkRealConst(paramNames[i]) )
        this.paramNames = new String[model.parameterCount()];
        for(int i = 0; i < model.parameterCount(); i++) {
            this.paramNames[i] = "p"+i;
        }
*/
    }

    /**
     * Compute predecessors of given node. All transitions are bounded by given parameter set.
     * @param from Source node.
     * @param borders Parametric borders.
     * @return A set of transitions in form of a map with "predecessor->transition borders" pairs.
     */
    @NotNull
    public Map<CoordinateNode, ColorFormulae> getPredecessors(@NotNull CoordinateNode from, @NotNull ColorFormulae borders) {
        return getDirectedEdges(from, borders, false);
    }

    /**
     * Compute successors of given node. All transitions are bounded by given parameter set.
     * @param from Source node.
     * @param borders Parametric borders.
     * @return A set of transitions in form of a map with "successor->transition borders" pairs.
     */
    @NotNull
    public Map<CoordinateNode, ColorFormulae> getSuccessors(@NotNull CoordinateNode from, @NotNull ColorFormulae borders) {
        return getDirectedEdges(from, borders, true);
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

    @NotNull
    private Map<CoordinateNode, ColorFormulae> getDirectedEdges(@NotNull CoordinateNode from, @NotNull ColorFormulae border, boolean successors) {

        // temporary - finally it will be replaced by input parameter called border of type ColorFormulae
        ColorFormulae color = new ColorFormulae(model.getDefaultContext());

        //TODO: remove after renaming
        @NotNull Map<CoordinateNode, ColorFormulae> results2 = new HashMap<>();
        @NotNull Map<CoordinateNode, ColorFormulae> results = new HashMap<>();

        boolean hasSelfLoop = true;

        //go through all dimension of model and compute results for each of them separately
        for (int dimension = 0; dimension < model.getVariableCount(); dimension++) {

            boolean lowerOutgoingDirection = false;
            boolean lowerIncomingDirection = false;
            boolean upperOutgoingDirection = false;
            boolean upperIncomingDirection = false;

            @NotNull int[][] vertices = computeBorderVerticesForState(from, dimension, true);

            //ColorFormulae outgoingDirectionExpressions = new ColorFormulae(color.getContext());
            //ColorFormulae incomingDirectionExpressions = new ColorFormulae(color.getContext());
            Expr outgoingDirectionExpression = color.getContext().mkFalse();
            Expr incomingDirectionExpression = color.getContext().mkFalse();

            // cycle for every vertices in lower (n-1)-dimensional facet of this state
            for (int[] vertex : vertices) {

                calculateValue(vertex, dimension);

                if(model.parameterCount() > 0) {
                    // In this case model equations
                    Context ctx = model.getDefaultContext(); //OR color.getContext();
                    RealExpr zero = ctx.mkReal("0");
                    RealExpr[] consts = new RealExpr[model.parameterCount()+1];
                    int i = 0;
                    for( ; i < model.parameterCount(); i++) {
                        consts[i] = ctx.mkReal(equationConsts[i].toString());
                    }
                    consts[i] = ctx.mkReal(equationConsts[i].toString());

                    Expr expr = ctx.mkMul(model.getContextParameter(0),consts[0]).simplify();
                    for(i = 1; i < model.parameterCount(); i++) {
                        expr = ctx.mkAdd((ArithExpr) expr,(ArithExpr) ctx.mkMul(model.getContextParameter(i),consts[i]).simplify()).simplify();
                    }
                    expr = ctx.mkAdd((ArithExpr) expr, consts[i]).simplify();

                    // on lower facet outgoing direction means equation's value is less or equal to zero and incoming means value is greater or equal to zero
                    outgoingDirectionExpression = ctx.mkOr((BoolExpr) outgoingDirectionExpression, (BoolExpr) ctx.mkGe(zero, (ArithExpr) expr).simplify()).simplify();
                    incomingDirectionExpression = ctx.mkOr((BoolExpr) incomingDirectionExpression, (BoolExpr) ctx.mkLe(zero, (ArithExpr) expr).simplify()).simplify();
                    //outgoingDirectionExpressions.addAssertion(ctx.mkGe(zero, (ArithExpr) expr).simplify());
                    //incomingDirectionExpressions.addAssertion(ctx.mkLe(zero, (ArithExpr) expr).simplify());

                } else {
                    // The case when model doesn't contain any parameter - whole equation becomes form f(x): 0 = c
                    Context ctx = model.getDefaultContext(); //OR color.getContext();
                    RealExpr zero = ctx.mkReal("0");
                    RealExpr consts = ctx.mkReal(equationConsts[0].toString());

                    // on lower facet outgoing direction means equation's value is less or equal to zero and incoming means value is greater or equal to zero
                    outgoingDirectionExpression = ctx.mkOr((BoolExpr) outgoingDirectionExpression, (BoolExpr) ctx.mkGe(zero, consts).simplify()).simplify();
                    incomingDirectionExpression = ctx.mkOr((BoolExpr) incomingDirectionExpression, (BoolExpr) ctx.mkLe(zero, consts).simplify()).simplify();
                    //outgoingDirectionExpressions.addAssertion(ctx.mkGe(zero, consts).simplify());
                    //incomingDirectionExpressions.addAssertion(ctx.mkLe(zero, consts).simplify());
                }
            }

            // Checking part - using of SMT-solver
            ColorFormulae outgoingDirectionSolver = (ColorFormulae) ColorFormulae.createCopy(color);
            outgoingDirectionSolver.addAssertion(outgoingDirectionExpression);
            lowerOutgoingDirection = outgoingDirectionSolver.check().equals(ColorFormulae.SAT);

            ColorFormulae incomingDirectionSolver = (ColorFormulae) ColorFormulae.createCopy(color);
            incomingDirectionSolver.addAssertion(incomingDirectionExpression);
            lowerIncomingDirection = incomingDirectionSolver.check().equals(ColorFormulae.SAT);

            System.out.println("Directions: "+lowerOutgoingDirection+" "+lowerIncomingDirection);

            if(from.coordinates[dimension] != 0)	{

                if((successors && lowerOutgoingDirection) || (!successors && lowerIncomingDirection)) {

                    @NotNull int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                    newStateCoors[dimension] = newStateCoors[dimension] - 1;

                    System.out.println("Adding");
                    if(successors)
                        results.put(factory.getNode(newStateCoors), outgoingDirectionSolver);
                    else
                        results.put(factory.getNode(newStateCoors), incomingDirectionSolver);
                }

            }

            vertices = computeBorderVerticesForState(from, dimension, false);
            outgoingDirectionExpression = color.getContext().mkFalse();
            incomingDirectionExpression = color.getContext().mkFalse();

            // cycle for every vertices in higher (n-1)-dimensional facet of this state
            for (int[] vertex : vertices) {

                calculateValue(vertex, dimension);

                if(model.parameterCount() > 0) {
                    // In this case model equations
                    Context ctx = model.getDefaultContext(); //OR color.getContext();
                    RealExpr zero = ctx.mkReal("0");
                    RealExpr[] consts = new RealExpr[model.parameterCount()+1];
                    int i = 0;
                    for( ; i < model.parameterCount(); i++) {
                        consts[i] = ctx.mkReal(equationConsts[i].toString());
                    }
                    consts[i] = ctx.mkReal(equationConsts[i].toString());

                    Expr expr = ctx.mkMul(model.getContextParameter(0),consts[0]).simplify();
                    for(i = 1; i < model.parameterCount(); i++) {
                        expr = ctx.mkAdd((ArithExpr) expr,(ArithExpr) ctx.mkMul(model.getContextParameter(i),consts[i]).simplify()).simplify();
                    }
                    expr = ctx.mkAdd((ArithExpr) expr, consts[i]).simplify();

                    // on upper facet outgoing direction means equation's value is greater or equal to zero and incoming means value is less or equal to zero
                    outgoingDirectionExpression = ctx.mkOr((BoolExpr) outgoingDirectionExpression, (BoolExpr) ctx.mkLe(zero, (ArithExpr) expr).simplify()).simplify();
                    incomingDirectionExpression = ctx.mkOr((BoolExpr) incomingDirectionExpression, (BoolExpr) ctx.mkGe(zero, (ArithExpr) expr).simplify()).simplify();

                } else {
                    // The case when model doesn't contain any parameter - whole equation becomes form f(x): 0 = c
                    Context ctx = model.getDefaultContext(); //OR color.getContext();
                    RealExpr zero = ctx.mkReal("0");
                    RealExpr consts = ctx.mkReal(equationConsts[0].toString());

                    // on upper facet outgoing direction means equation's value is greater or equal to zero and incoming means value is less or equal to zero
                    outgoingDirectionExpression = ctx.mkOr((BoolExpr) outgoingDirectionExpression, (BoolExpr) ctx.mkLe(zero, consts).simplify()).simplify();
                    incomingDirectionExpression = ctx.mkOr((BoolExpr) incomingDirectionExpression, (BoolExpr) ctx.mkGe(zero, consts).simplify()).simplify();
                }
            }

            // Checking part - using of SMT-solver
            outgoingDirectionSolver = (ColorFormulae) ColorFormulae.createCopy(color);
            outgoingDirectionSolver.addAssertion(outgoingDirectionExpression);
            upperOutgoingDirection = outgoingDirectionSolver.check().equals(ColorFormulae.SAT);

            incomingDirectionSolver = (ColorFormulae) ColorFormulae.createCopy(color);
            incomingDirectionSolver.addAssertion(incomingDirectionExpression);
            upperIncomingDirection = incomingDirectionSolver.check().equals(ColorFormulae.SAT);

            System.out.println("Directions: "+upperOutgoingDirection+" "+upperIncomingDirection);

            if(from.coordinates[dimension] != model.getThresholdRanges().get(dimension).upperEndpoint() - 1) {

                if((successors && upperOutgoingDirection) || (!successors && upperIncomingDirection)) {

                    @NotNull int[] newStateCoors = Arrays.copyOf(from.coordinates, from.coordinates.length);
                    newStateCoors[dimension] = newStateCoors[dimension] + 1;

                    System.out.println("Adding");
                    if(successors)
                        results.put(factory.getNode(newStateCoors), outgoingDirectionSolver);
                    else
                        results.put(factory.getNode(newStateCoors), incomingDirectionSolver);
                }

            }

            if(hasSelfLoop) {
                if(lowerOutgoingDirection && upperOutgoingDirection && !lowerIncomingDirection && !upperIncomingDirection ||
                        !lowerOutgoingDirection && !upperOutgoingDirection && lowerIncomingDirection && upperIncomingDirection) {

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
     * Modifies global variables to contain Derivation of specified variable in given vertex as given by function specified in the model.
     * @param vertex Coordinates of thresholds where the computation should be performed.
     * @param dim Index of variable whose function should be evaluated.
     */
    private void calculateValue(int[] vertex, int dim) {
        //derivationValue = 0;
        //denominator = 0;
        //parameterIndex = -1;
        for(int i = 0; i < model.parameterCount()+1; i++) {
            equationConsts[i] = 0.0;
        }

        for (@NotNull SumMember sumMember : model.getEquationForVariable(dim)) {
            //init partial sum to constant part of the equation member
            double partialSum = sumMember.getConstant();

            //multiply partialSum by values of all ramps relevant to this sum member
            for (@NotNull Ramp ramp : sumMember.getRamps()) {
                //ramp, as sum member, indexes variables from 1, therefore -1
                partialSum *= ramp.value(model.getThresholdValueForVariableByIndex(ramp.dim - 1, vertex[ramp.dim - 1]));
                if (partialSum == 0) break;
            }

            if (partialSum != 0) {
                //multiply partialSum by actual value of every threshold relevant to this sum member
                for (Integer variableIndex : sumMember.getVars()) {
                    //sum member indexes it's variables from 1, so we need to subtract 1 to fit model
                    partialSum *= model.getThresholdValueForVariableByIndex(variableIndex - 1, vertex[variableIndex - 1]);
                }

                //multiply partialSum by values of all step functions relevant to this sum member
                for (@NotNull Step step : sumMember.getSteps()) {
                    //step, as sum member, indexes variables from 1, therefore -1
                    partialSum *= step.value(model.getThresholdValueForVariableByIndex(step.dim - 1, vertex[step.dim - 1]));
                }
            }

            if (sumMember.hasParam()) {
                //OLD WAY
                //we set values for following parameter splitting computation
                //parameterIndex = sumMember.getParam() - 1;
                //denominator += partialSum;

                //NEW WAY - summember value is added into global array of equation constants for particular parameter
                // given by right index, if summember doesn't contain particular param its constant stays zero
                equationConsts[sumMember.getParam() - 1] += partialSum;
            } else {
                //OLD WAY
                //if sum member does not have a parameter, just add all of this to the final sum
                //derivationValue += partialSum;

                //NEW WAY - the case when summember has no parameter therefore the value is added to lonely constant
                equationConsts[model.parameterCount()] += partialSum;
            }
        }

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

        if (upperBound < lowerBound) {
            return results;
        }

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
                    remainingWork[activeIndex] = upperBound;
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
                    remainingWork[activeIndex] = coordinateBounds.get(activeIndex).upperEndpoint();
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
