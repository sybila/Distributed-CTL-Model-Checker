package cz.muni.fi.thomas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by daemontus on 12/02/15.
 */
public class NativeModel {

    private final String fileName;

    //Basically a copy of kinetics object.
    //For every specie, For every context of a specie specify list of target values.
    public Map<String, Map<String, List<Byte>>> specieContextTargetMapping = new HashMap<>();

    public NativeModel(String fileName) {
        this.fileName = fileName;
    }

    public void loadModel(NetworkModel nodeStorage) {
        loadNative(fileName, nodeStorage);
        /*for (Map.Entry<String, Map<String, List<Byte>>> entry : specieContextTargetMapping.entrySet()) {
            System.out.println("Specie: "+entry.getKey());
            for (Map.Entry<String, List<Byte>> context : entry.getValue().entrySet()) {
                System.out.println("Context: " + context.getKey());
                System.out.println("Targets: "+ Arrays.toString(context.getValue().toArray()));
            }
        }*/
    }

    private native void loadNative(String filename, NetworkModel nodeStorage);

}
