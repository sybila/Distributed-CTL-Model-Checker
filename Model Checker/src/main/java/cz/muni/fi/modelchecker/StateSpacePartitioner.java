package cz.muni.fi.modelchecker;

import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

/**
 * Class used to partition provided state space into separate sub graphs that can be processed on different machines.
 */
public interface StateSpacePartitioner<T extends Node> {

    /**
     * Computes the id of a sub graph where provided node is located.
     * @param node some node of a graph
     * @return ID of sub graph that holds provided node
     * @throws IllegalArgumentException Thrown if node is not a part of graph managed by this partitioner.
     */
    int getNodeOwner(@NotNull T node) throws IllegalArgumentException;

}
