package cz.muni.fi.ode;

import cz.muni.fi.ctl.Formula;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * One node of transition system represented by its hash.
 * Hash is computed by ordering nodes by dimensions and assigning each a unique ID:
 * (c[0] * 1 + c[1] * |c[0]| + c[2] * |c[0-1]| + ...)
 * At this point, only integer hash is supported. So maximum number of nodes we can work with is 2^31.
 *
 * Each node also manages formulae that hold in it and list of it's predecessors.
 *
 * Two hash nodes are equal if their hash matches. The owner does not have to be equal,
 * because inequality would imply we are using nodes from different models, which should not happen.
 *
 * This class is thread safe.
 */
public class HashNode implements Node {

    public final int hash;
    public final short owner;

    //these are package private so we can use other classes to print them
    final Map<Formula, TreeColorSet> formulae = new HashMap<>();
    Map<HashNode, TreeColorSet> predecessors;

    /**
     * @param hash Hash ID of the node.
     * @param owner ID of machine that owns this node.
     */
    public HashNode(int hash, short owner) {
        this.hash = hash;
        this.owner = owner;
    }

    /**
     * @return True if predecessors for this node has already been computed.
     */
    public synchronized boolean hasPredecessorsFor() {
        return predecessors != null;
    }

    /**
     * @param predecessors Predecessors that should be saved in this node.
     */
    public synchronized void savePredecessors(@NotNull Map<HashNode, TreeColorSet> predecessors) {
        if (this.predecessors != null) {
            throw new IllegalStateException("Adding predecessors to state which already has them: "+toString());
        }
        this.predecessors = new HashMap<>();
        this.predecessors.putAll(predecessors);
    }

    /**
     * Return predecessors of this state (if they are available) constrained by given borders.
     * @param borders Parametric set that constrains the relevant edges.
     * @return Predecessors of this node that satisfy given constrain.
     */
    @NotNull public synchronized Map<HashNode, TreeColorSet> getPredecessors(@NotNull TreeColorSet borders) {
        if (predecessors == null) {
            throw new IllegalStateException("Reading predecessors of state which does not have them: "+toString());
        }
        @NotNull Map<HashNode, TreeColorSet> results = new HashMap<>();
        for (@NotNull Map.Entry<HashNode, TreeColorSet> entry : predecessors.entrySet()) {
            @NotNull TreeColorSet colorSet = TreeColorSet.createCopy(entry.getValue());
            colorSet.intersect(borders);
            if (!colorSet.isEmpty()) {
                results.put(entry.getKey(), colorSet);
            }
        }
        return results;
    }

    /**
     * Remove all saved data about specified formula. (This is mainly for memory usage optimization with deep formulas)
     */
    public synchronized void purgeFormula(@NotNull Formula formula) {
        formulae.remove(formula);
    }

    /**
     * Return colors for which given formula holds in this node.
     * @return Null if formula does not hold in this node.
     */
    @Nullable public synchronized TreeColorSet getValidColors(@NotNull Formula formula) {
        return formulae.get(formula);
    }

    /**
     * Mark given formula as satisfied in given color set. This information is automatically merged
     * with any previous data regarding this formula.
     * @param formula Formula that should be marked as satisfied.
     * @param colors Color set for which formula holds.
     * @return True if any new information has been obtained, false otherwise.
     */
    public synchronized boolean addFormula(Formula formula, @NotNull TreeColorSet colors) {
        if (colors.isEmpty()) return false;
        TreeColorSet colorSet = formulae.get(formula);
        if (colorSet == null) {
            formulae.put(formula, TreeColorSet.createCopy(colors));
            return true;
        } else {
            if (colorSet.encloses(colors)) {
                return false;
            } else {
                colorSet.union(colors);
                return true;
            }
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HashNode) {
            @NotNull HashNode n = (HashNode) obj;
            return hash == n.hash;
        }
        return false;
    }

    @Override
    public String toString() {
        return "HashNode{" +
                "owner=" + owner +
                ", hash=" + hash +
                '}';
    }

}
