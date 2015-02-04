package cz.muni.fi.ode;

import com.google.common.collect.Range;
import cz.muni.fi.modelchecker.StateSpacePartitioner;

import java.util.List;

/**
 * Created by daemontus on 04/02/15.
 */
public interface CoordinatePartitioner extends StateSpacePartitioner<CoordinateNode> {

    public List<Range<Double>> getMyLimit();
}
