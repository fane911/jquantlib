/*
 Copyright (C) 2008 Daniel Kong
 
 This source code is release under the BSD License.
 
 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the JQuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquant-devel@lists.sourceforge.net>. The license is also available online at
 <http://www.jquantlib.org/index.php/LICENSE.TXT>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
 
 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the original copyright notice follows this notice.
 */
package org.jquantlib.pricingengines;

import org.jquantlib.methods.lattices.BinomialTree;

/**
 * 
 * @author Daniel Kong
 * 
 */
//TODO: work in progress

//! Binomial Tsiveriotis-Fernandes engine for convertible bonds
/*  \ingroup hybridengines

    \test the correctness of the returned value is tested by
          checking it against known results in a few corner cases.
*/
public class BinomialConvertibleEngine <B extends BinomialTree> extends ConvertibleBondOptionEngine {
	
	private int timeSteps;
	
	public BinomialConvertibleEngine (int timeSteps){
		this.timeSteps = timeSteps;
	}

    //
    // implements PricingEngine
    //
    
	@Override
	public void calculate() {
		throw new UnsupportedOperationException();
	}

}
