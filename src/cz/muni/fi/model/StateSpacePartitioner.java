package cz.muni.fi.model;

import com.google.common.collect.Range;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.distributed.graph.Graph;
import cz.muni.fi.distributed.graph.Node;

import java.util.ArrayList;
import java.util.List;

public class StateSpacePartitioner {

    private final int size;
    private final List<Range<Double>> ranges = new ArrayList<>();

    public StateSpacePartitioner(Model model, int size, int rank) {
        this.size = size;
        Range<Double> firstVar = model.getVariableRange().get(0);
        if (!firstVar.hasLowerBound() || !firstVar.hasUpperBound()) {
            throw new IllegalArgumentException("Range is unbounded");
        }
        int interval = (int) (firstVar.upperEndpoint() - firstVar.lowerEndpoint());
        interval /= size;
        for (int i = 0; i<size; i++) {
            if (i == size - 1) {
                Log.d("Range: "+i+" "+(firstVar.lowerEndpoint() + interval * i)+" "+firstVar.upperEndpoint());
                ranges.add(Range.closed(firstVar.lowerEndpoint() + interval * i, firstVar.upperEndpoint()));
            } else {
                Log.d("Range: "+i+" "+(firstVar.lowerEndpoint() + interval * i)+" "+(firstVar.lowerEndpoint() + interval * (i+1)));
                ranges.add(Range.closedOpen(firstVar.lowerEndpoint() + interval * i, firstVar.lowerEndpoint() + interval * (i+1)));
            }
        }
        Log.d("Partitioning done");
    }

    public int getGraphId(Node node) {
        for (int i = 0; i < size; i++) {
            Range<Double> range = ranges.get(i);
            if (range.contains((double) node.getCoordinate(0))) {
                return i;
            }
        }
        Log.d("Ranges: "+ranges.size());
        for (int i=0; i<size; i++) {
            Range<Double> range = ranges.get(i);
            Log.d("Range: "+range.lowerEndpoint()+" "+range.upperEndpoint());
        }
        throw new IllegalStateException("Node "+node.getCoordinate(0)+" does not have a parent node "+ranges.size()+" "+ranges.get(0).lowerEndpoint()+" "+ranges.get(0).upperEndpoint());
    }

    public List<Range<Double>> getMyLimit(Graph graph) {
        List<Range<Double>> ret = new ArrayList<>();
        ret.add(ranges.get(graph.id));
        List<Range<Double>> params = graph.model.getVariableRange();
        for (int i=1; i < params.size(); i++) {
            //Log.d(i+"th param: "+params.get(i).lowerEndpoint()+" "+params.get(i).upperEndpoint());
            ret.add(params.get(i));
        }
        return ret;
    }
}
