package cz.muni.fi.export;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daemontus on 16/05/15.
 */
public class Successor {

    public Node target;
    //public Colours colours;
    public List<Param> colours = new ArrayList<>();

    public static class Successors {
        public Node origin;
        public List<Successor> transitions = new ArrayList<>();
    }

    public static class Param {
        public Parameter parameter;
        public List<Interval<Double>> intervals = new ArrayList<>();
    }

}
