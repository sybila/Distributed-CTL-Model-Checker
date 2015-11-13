package cz.muni.fi.thomas;

import cz.muni.fi.modelchecker.graph.ColorSet;
import net.sf.javabdd.BDD;

import java.util.Arrays;


public class BDDColorSet implements ColorSet {

    public BDD bdd;

    public BDDColorSet(BDD bdd) {
        this.bdd = bdd;
    }

    public BDDColorSet(BDDColorSet copy) {
        this.bdd = copy.bdd.or(copy.bdd);
    }

    @Override
    public void intersect(ColorSet set) {
        BDDColorSet s = (BDDColorSet) set;
        this.bdd = bdd.and(s.bdd);
    }

    @Override
    public void subtract(ColorSet set) {
        BDDColorSet s = (BDDColorSet) set;
        this.bdd = bdd.and(s.bdd.not());
    }

    @Override
    public boolean union(ColorSet set) {
        BDDColorSet s = (BDDColorSet) set;
        if (this.isEmpty() && set.isEmpty()) {
            return false;
        }
        if (this.isEmpty()) {
            this.bdd = s.bdd.or(s.bdd);
            return true;
        }
        if (set.isEmpty()) {
            return false;
        }
        BDD newBdd = bdd.or(s.bdd);
        if (newBdd.satCount() == bdd.satCount() && !newBdd.equals(bdd)) {
            throw new IllegalStateException("Wat?!");
        }
        if (newBdd.equals(this.bdd)) {
            return false;
        } else {
            this.bdd = newBdd;
            return true;
        }
    }

    @Override
    public boolean isEmpty() {
        return this.bdd.satCount() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BDDColorSet that = (BDDColorSet) o;

        return !(bdd != null ? !bdd.equals(that.bdd) : that.bdd != null);

    }

    @Override
    public int hashCode() {
        return bdd != null ? bdd.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "{"/*+bdd.toStringWithDomains()*/+Arrays.deepToString(bdd.allsat().toArray())+" "+bdd.satCount()+"}";
    }
}
