package cz.muni.fi.modelchecker.verification;

/**
 * Object responsible for verification of one operator over given model.
 */
interface FormulaProcessor {

    /** Use available data to compute new information. */
    public void verify();

}
