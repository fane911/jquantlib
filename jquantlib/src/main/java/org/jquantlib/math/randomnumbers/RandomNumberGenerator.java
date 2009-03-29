/*
 Copyright (C) 2007 Richard Gomes

 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquant-devel@lists.sourceforge.net>. The license is also available online at
 <http://www.jquantlib.org/index.php/LICENSE.TXT>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
 
 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the original copyright notice follows this notice.
 */

package org.jquantlib.math.randomnumbers;

import org.jquantlib.methods.montecarlo.Sample;

/**
 * @author Richard Gomes
 */
public interface RandomNumberGenerator {

    /**
     * @return another random number
     */
    public Sample<Double> next() /*@ReadOnly*/;


    /**
     * Once JVM does not support unsigned fixed arithmetic, we use 64bit variables as containers for 32bit values in order to reduce
     * the complexity and performance overhead of certain fixed arithmetic operations.
     * <p>
     * From the user's perspective, it's easier to understand that a certain <code>long</code> variable contains 
     * values from 0 to 2^32 than understand that a certain <code>int</code> variable contains negative values that should be 
     * converted to it's two's complement and extended to a <code>long</code> variable before use.
     * 
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4504839">Bug 4504839</a>
     * @see <a href="http://darksleep.com/player/JavaAndUnsignedTypes.html">Java and Unsigned Types</a>
     * @see <a href="http://en.wikipedia.org/wiki/Two%27s_complement">Two's complement</a>
     * 
     * @return an unsigned 32bit wide number encapsulated in a 64bit container
     */
    public /*@UnsignedInt*/ long nextInt32() /*@ReadOnly*/;

    // FIXME: code review:: public boolean allowErrorEstimate(); // FIXME: should declare this method???
}
