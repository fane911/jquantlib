/*
 Copyright (C) 2008 Srinivas Hasti

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

package org.jquantlib.methods.finitedifferences;

import org.jquantlib.lang.reflect.TypeToken;
import org.jquantlib.math.Array;
import org.jquantlib.processes.GeneralizedBlackScholesProcess;

public abstract class PdeOperator<T extends PdeSecondOrderParabolic> extends TridiagonalOperator {
   
	public PdeOperator(Array grid, GeneralizedBlackScholesProcess process, double residualTime) {
		super(grid.length);
		Class<T> clazz = (Class<T>)TypeToken.getClazz(this.getClass());
		PdeSecondOrderParabolic pde = PdeTypeTokenUtil.getPdeInstance(clazz, process);
		timeSetter = new GenericTimeSetter<PdeSecondOrderParabolic>(grid, pde){};
		setTime(residualTime);
	}

}
