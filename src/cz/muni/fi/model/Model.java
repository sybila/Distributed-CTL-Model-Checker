package cz.muni.fi.model;

import com.google.common.collect.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one ODE model
 */
public class Model {

    public final String filename;
    private List<Range<Double>> variableRange = new ArrayList<>();
    private List<Range<Double>> parameterRange = new ArrayList<>();

    public Model(String filename) {
        this.filename = filename;
    }

    public void load() {
        cppLoad(filename);
        //System.out.println("Parameters: "+parameterRange.size()+" Range: "+variableRange.get(0));
        //System.out.println("Parameters: "+parameterRange.size()+" Range: "+variableRange.get(1));
    }

    private native void cppLoad(String filename);

    public List<Range<Double>> getVariableRange() {
        return variableRange;
    }

    public List<Range<Double>> getParameterRange() {
        return parameterRange;
    }

    public ColorSet getFullColorSet() {
        ColorSet set = TreeColorSet.createEmpty(parameterRange.size());
        for (int i = 0; i < set.size(); i++) {
            set.get(i).add(parameterRange.get(i));
        }
        return set;
    }

    public int variableCount() {
        return variableRange.size();
    }

    public int parameterCount() {
        return parameterRange.size();
    }

}
