package cz.muni.fi;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

import java.util.Arrays;

/**
 * Created by daemontus on 10/11/15.
 */
public class BDDPlayground {

    public static void main(String[] args) {

        BDDFactory bdd = JFactory.init(1000, 1000);

        BDDDomain[] doms = bdd.extDomain(new int[]{ 8 , 16, 32 });

        System.out.println(Arrays.toString(doms[0].vars()));
        System.out.println(Arrays.toString(doms[1].vars()));
        System.out.println(Arrays.toString(doms[2].vars()));
        BDD a = doms[0].ithVar(1).or(doms[0].ithVar(5));
        BDD b = doms[1].ithVar(2);
        BDD c = doms[2].varRange(4, 10);

        System.out.println(Arrays.deepToString(a.allsat().toArray()));
        System.out.println(Arrays.deepToString(b.allsat().toArray()));
        System.out.println(Arrays.deepToString(c.allsat().toArray()));

        System.out.println(Arrays.deepToString((a.and(b).and(c)).allsat().toArray()));
    }

}
