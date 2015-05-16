package cz.muni.fi.export;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by daemontus on 16/05/15.
 */
public class Model {

    public final List<Variable> variables = new ArrayList<>();

    public final List<Successor.Successors> transitions = new ArrayList<>();

}
