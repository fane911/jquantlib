/*
 Copyright (C) 2007 Richard Gomes

 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquantlib-dev@lists.sf.net>. The license is also available online at
 <http://jquantlib.org/license.shtml>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
 
 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the originating copyright notice follows below.
 */

/*
 Copyright (C) 2003 Ferdinando Ametrano
 Copyright (C) 2001, 2002, 2003 Sadruddin Rejeb
 Copyright (C) 2006 StatPro Italia srl

 This file is part of QuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://quantlib.org/

 QuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <quantlib-dev@lists.sf.net>. The license is also available online at
 <http://quantlib.org/license.shtml>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
*/

package org.jquantlib.exercise;

import cern.colt.list.IntArrayList;




/**
 * Base exercise class
 * 
 * @author Richard Gomes
 */
public abstract class Exercise extends IntArrayList {

	/**
	 * Defines the exercise type. It can be American, Bermudan or European
	 * 
	 * @author Richard Gomes
	 */
	enum Type {
		American, Bermudan, European;
	}

	private Exercise.Type type;

	/**
	 * Constructs an exercise and defines the exercise type
	 * 
	 * @param type is the type of exercise
	 * 
	 * @see Exercise.Type
	 */
	protected Exercise(Exercise.Type type) {
		this.type = type;
	}

	/**
	 * Returns the exercise type
	 * 
	 * @return the exercise type
	 * 
	 * @see Exercise.Type
	 */
	protected Exercise.Type getType() {
		return type;
	}
	
	/**
	 * This method is only used by extended classes on the very special cases 
	 * when the type of the exercise must be changed.
	 * 
	 * @param type is the exercise type
	 * 
	 * @see Exercise.Type
	 * @see BermudanExercise
	 */
	protected void setType(Exercise.Type type) {
		this.type = type;
	}

}
