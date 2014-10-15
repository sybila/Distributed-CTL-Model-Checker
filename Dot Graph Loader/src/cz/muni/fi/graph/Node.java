package cz.muni.fi.graph;

import cz.muni.fi.ctl.formula.Formula;

import java.util.*;

public class Node {

    private String id;
    private int subGraphId;
    private Set<Path> to = new HashSet<>();  //children
    private Map<String, Float> values = new HashMap<>();
    public Map<Formula, Set<Integer>> formulas = new HashMap<>();

    private Node() {}

    public Node(Map<String, Float> values, String id) {
        if (values == null) {
            throw new NullPointerException("Cannot create node with null values");
        }
        if (id == null) {
            throw new NullPointerException("You have to provide an ID for node.");
        }
        this.values.putAll(values);
        this.id = id;
    }

    public Node copy() {
        Node result = new Node();
        result.values.putAll(values);
        result.id = id;
        result.subGraphId = subGraphId;
        return result;
    }

    public boolean hasVariable(String name) {
        return values.containsKey(name);
    }

    public Float getVariable(String name) {
        return values.get(name);
    }

    public void addAfter(Path path) {
        to.add(path);
    }

    public Set<Path> getAfter() {
        return to;
    }

    public Set<Integer> getValidColors(Formula formula) {
        if (formulas.containsKey(formula)) {
            return formulas.get(formula);
        } else {
            return new HashSet<>();
        }
    }

    public void addFormula(Formula formula, Set<Integer> colors) {
        if (!formulas.containsKey(formula)) {
            formulas.put(formula, new HashSet<Integer>());
        }
        formulas.get(formula).addAll(colors);
    }

    @Override
    public String toString() {
        String result = "Node: ";
        for (Map.Entry<String, Float> pair : values.entrySet()) {
            result += pair.getKey()+":"+pair.getValue()+" ";
        }
        result += "\n";
        result += formulas.size()+" ";
        for (Map.Entry<Formula, Set<Integer>> pair : formulas.entrySet()) {
            result += pair.getKey()+" ("+Arrays.toString(pair.getValue().toArray())+"), ";
        }
        //result += formulas.size()+" "+Arrays.toString(formulas.keySet().toArray());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;

        return id.equals(node.id) && values.equals(node.values);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + values.hashCode();
        return result;
    }

    public boolean isExternal(int subGraph) {
        return subGraphId != subGraph;
    }

    public void setSubGraphId(int subGraphId) {
        this.subGraphId = subGraphId;
    }

    public int getSubGraphId() {
        return this.subGraphId;
    }
}
