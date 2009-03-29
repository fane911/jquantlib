package org.jquantlib.indexes;

import org.jquantlib.quotes.Handle;
import org.jquantlib.termstructures.YieldTermStructure;
import org.jquantlib.time.Period;
import org.jquantlib.time.TimeUnit;

/**
 * 3-months Euribor index
 * 
 * @author Ueli Hofstetter
 */
public class Euribor3M extends Euribor {
    public Euribor3M(Handle<YieldTermStructure> h) {
        super(new Period(3, TimeUnit.MONTHS), h);
    }
}