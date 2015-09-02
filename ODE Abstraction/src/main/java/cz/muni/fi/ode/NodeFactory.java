package cz.muni.fi.ode;

import cz.muni.fi.ctl.CtlPackage;
import cz.muni.fi.ctl.FloatProposition;
import cz.muni.fi.ctl.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import org.antlr.v4.runtime.misc.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NodeFactory implements ModelAdapter<CoordinateNode, RectParamSpace> {


    @NotNull
    private final Set<FloatProposition> revealedPropositions = new HashSet<>();

    private final Map<Long, CoordinateNode> nodeCache = new HashMap<>();
    private final Map<Long, CoordinateNode> borderNodes = new HashMap<>();
    private final OdeModel model;
    @NotNull
    private final HashPartitioner partitioner;
    private final int myId;
    private boolean hasAllNodes = false;
    private StateSpaceGenerator generator;

    public NodeFactory(OdeModel model, @NotNull HashPartitioner partitioner) {
        this.model = model;
        this.partitioner = partitioner;
        this.myId = partitioner.getMyId();
    }

    public CoordinateNode above(CoordinateNode node, int dim) {
        int[] newCoords = Arrays.copyOf(node.coordinates, node.coordinates.length);
        newCoords[dim]++;
        if (model.isValidNode(newCoords)) {
            return getNode(newCoords);
        } else return null;
    }

    public CoordinateNode below(CoordinateNode node, int dim) {
        int[] newCoords = Arrays.copyOf(node.coordinates, node.coordinates.length);
        newCoords[dim]--;
        if (model.isValidNode(newCoords)) {
            return getNode(newCoords);
        } else return null;
    }

    @NotNull
    public Collection<CoordinateNode> getNodes() {
        return nodeCache.values();
    }

    public synchronized CoordinateNode getNode(@NotNull int[] coordinates) {
        long hash = model.nodeHash(coordinates);
        if (nodeCache.containsKey(hash)) {
            return nodeCache.get(hash);
        } else if (borderNodes.containsKey(hash)) {
            return borderNodes.get(hash);
        } else {
            @NotNull CoordinateNode n = new CoordinateNode(coordinates, hash);
            if (partitioner.getNodeOwner(n) == myId) {
                nodeCache.put(hash, n);
            } else {
                borderNodes.put(hash, n);
            }
            return n;
        }
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, RectParamSpace> predecessorsFor(@NotNull CoordinateNode to, @org.jetbrains.annotations.Nullable @Nullable RectParamSpace borders) {
        if (borders == null) {
            borders = model.getFullColorSet();
        }
        if (!to.hasPredecessorsFor()) {
            Map<CoordinateNode, RectParamSpace> results = generator.getPredecessors(to, model.getFullTreeColorSet());//getNativePredecessors(to.coordinates, model.getFullColorSet(), new HashMap<CoordinateNode, TreeColorSet>());
            to.savePredecessors(results);
        }
        return to.getPredecessors(borders);
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, RectParamSpace> successorsFor(@NotNull CoordinateNode from, @org.jetbrains.annotations.Nullable @Nullable RectParamSpace borders) {
        if (borders == null) {
            borders = model.getFullColorSet();
        }
        Map<CoordinateNode, RectParamSpace> full = generator.getSuccessors(from, model.getFullTreeColorSet()); //getNativeSuccessors(from.coordinates, borders, new HashMap<CoordinateNode, TreeColorSet>());
        for (Map.Entry<CoordinateNode, RectParamSpace> entry : full.entrySet()) {
            entry.getValue().intersect(borders);
        }
        return full;
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, RectParamSpace> initialNodes(@NotNull Formula formula) {
        if (formula == CtlPackage.getTrue()) {
            if (!hasAllNodes) {
                generator.cacheAllNodes();
                hasAllNodes = true;
            }
            @NotNull Map<CoordinateNode, RectParamSpace> results = new HashMap<>();
            for (CoordinateNode node : nodeCache.values()) {
                results.put(node, model.getFullColorSet());
            }
            return results;
        }
        if (formula == CtlPackage.getFalse()) {
            return new HashMap<>();
        }
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            @NotNull FloatProposition proposition = (FloatProposition) formula;
            revealedPropositions.add(proposition);
            for (CoordinateNode node : generator.initial(proposition)) {
                //proposition is invariant to parameters
                node.addFormula(proposition, model.getFullColorSet());
            }
            //values are not exclusively our inner nodes, so we can't return them directly.
            //return values;
        }
        @NotNull Map<CoordinateNode, RectParamSpace> results = new HashMap<>();
        for (CoordinateNode node : nodeCache.values()) {
            RectParamSpace validColors = node.getValidColors(formula);
            if (validColors != null && !validColors.isEmpty()) {
                results.put(node, new RectParamSpace(validColors.getItems()));
            }
        }
        return results;
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, RectParamSpace> invertNodeSet(@NotNull Map<CoordinateNode, RectParamSpace> nodes) {
        if (!hasAllNodes) {
            generator.cacheAllNodes();
            hasAllNodes = true;
        }
        @NotNull Map<CoordinateNode, RectParamSpace> results = new HashMap<>();
        for (CoordinateNode n : nodeCache.values()) {
            @NotNull RectParamSpace full = model.getFullColorSet();
            RectParamSpace anti = nodes.get(n);
            if (anti != null) {
                full.subtract(anti);
            }
            if (!full.isEmpty()) {
                results.put(n, full);
            }
        }
        return results;
    }

    @Override
    public synchronized boolean addFormula(@NotNull CoordinateNode node, @NotNull Formula formula, @NotNull RectParamSpace parameters) {
        return node.addFormula(formula, parameters);
    }

    @NotNull
    @Override
    public synchronized RectParamSpace validColorsFor(@NotNull CoordinateNode node, @NotNull Formula formula) {
        if (formula == CtlPackage.getTrue()) return model.getFullColorSet();
        if (formula == CtlPackage.getFalse()) return RectParamSpace.Companion.empty();
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            @NotNull FloatProposition proposition = (FloatProposition) formula;
            revealedPropositions.add(proposition);
            for (CoordinateNode n : generator.initial(proposition)) {
                n.addFormula(proposition, model.getFullColorSet());
            }
        }
        RectParamSpace colorSet = node.getValidColors(formula);
        if (colorSet == null) return RectParamSpace.Companion.empty();
        return new RectParamSpace(colorSet.getItems());
    }

    @Override
    public void purge(Formula formula) {
        for (CoordinateNode node : nodeCache.values()) {
            node.purgeFormula(formula);
        }
    }

    public void setGenerator(StateSpaceGenerator generator) {
        this.generator = generator;
    }
}
