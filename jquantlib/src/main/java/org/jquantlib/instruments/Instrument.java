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
 Copyright (C) 2000, 2001, 2002, 2003 RiskMap srl
 Copyright (C) 2003, 2004, 2005, 2006, 2007 StatPro Italia srl

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

package org.jquantlib.instruments;

import java.util.Map;
import java.util.TreeMap;

import org.jquantlib.pricingengines.PricingEngine;
import org.jquantlib.util.LazyObject;
import org.jquantlib.util.Observer;

//FIXME: add comments
public abstract class Instrument extends LazyObject implements Observer {

	/**
	 * Represents the net present value of the instrument.
	 */
	private /*@Price*/ double NPV_;
	
	/**
	 * Represents the error estimate on the NPV when available.
	 */
	private /*@Price*/ double errorEstimate_;
	
	/**
	 * Represents all additional result returned by the pricing engine.
	 */
	private Map<String, Object> additionalResults_;
	
	/**
	 * The value of this attribute and any other that derived
	 * classes might declare must be set during calculation.
	 * 
	 * @todo verify how these variables are used
	 */
	protected PricingEngine engine_;
	
	

    public Instrument() {
    	super();
    	this.NPV_ = Double.NaN;
    	this.errorEstimate_ = 0.0;
    }

	public final /*@Price*/ double getNPV() /* @ReadOnly */ {
		calculate();
		if (this.NPV_==Double.NaN) throw new ArithmeticException("NPV not provided");
		return NPV_;
	}

	public final /*@Price*/ double getErrorEstimate() /* @ReadOnly */ {
		calculate();
		if (this.errorEstimate_==Double.NaN) throw new ArithmeticException("error estimate not provided");
		return errorEstimate_;
	}

	
	
	//
	// abstract methods
	//
	
	/**
	 * @return <code>true</code> whether the instrument is still tradeable.
	 */
	public abstract boolean isExpired();
	
    /**
     * When a derived argument structure is defined for an
     * instrument, this method should be overridden to fill
     * it. This is mandatory in case a pricing engine is used.
     *  
     * @param arguments
     */
    public abstract void setupArguments(final PricingEngine.Arguments arguments);

    

    //
	// methods overridden by extended classes
	//
	
    /**
     * When a derived result structure is defined for an
     * instrument, this method should be overridden to read from  
     * it. This is mandatory in case a pricing engine is used.
     * 
     * @param results
     */
    protected void fetchResults(final PricingEngine.Results results) /* @ReadOnly */ {
    	if (results == null) throw new NullPointerException("no results returned from pricing engine");
    	if (results instanceof InstrumentResults) {
    		InstrumentResults r = (InstrumentResults)results;
        	NPV_ = r.value;
        	errorEstimate_ = r.errorEstimate;
        	additionalResults_ = r.additionalResults;
    	} else {
    		throw new ClassCastException(); // FIXME:message
    	}
    }

    
    /**
     * This method must leave the instrument in a consistent
     * state when the expiration condition is met.
     */
    protected void setupExpired() {
        NPV_ = 0.0;
        errorEstimate_ = 0.0;
        additionalResults_.clear();
    }
    

    /**
     * In case a pricing engine is <b>not</b> used, this
     * method must be overridden to perform the actual
     * calculations and set any needed results. In case
     * a pricing engine is used, the default implementation
     * can be used.
     */
    protected final void performCalculations() {
        if (engine_==null) throw new NullPointerException("null pricing engine");
        engine_.reset();
        setupArguments(engine_.getArguments());
        engine_.getArguments().validate();
        engine_.calculate();
        fetchResults(engine_.getResults());
    }

    

    //
    // protected final methods
    //
    
    /**
     * Set the pricing engine to be used.
     * 
     * @note calling this method will have no effects in 
     * case the <b>performCalculation</b> method
     * was overridden in a derived class.
     *  
     * @param engine
     */
    protected final void setPricingEngine(final PricingEngine engine) {
    	if (this.engine_!=null) {
    		this.engine_.deleteObserver(this);
    	}
    	this.engine_ = engine;
    	if (this.engine_!=null) {
    		this.engine_.addObserver(this);
    	}
    	update(this, null);
    }

    
	protected final void calculate() {
		if (isExpired()) {
			setupExpired();
			calculated_ = true;
		} else {
			super.calculate();
		}
	}
    

	// FIXME: CODE REVIEW
	protected final Object getResult(final String tag) {
    	calculate();
    	Object value = additionalResults_.get(tag);
    	if (value==null) throw new IllegalArgumentException(tag+" not provided");
        return value; // return boost::any_cast<T>(value->second);
    }

	

	//
	// Inner class Instrument.Results
	//
	
	public final Map<String, Object> getAdditionalResults() {
        return additionalResults_;
    }

    
    protected class InstrumentResults implements PricingEngine.Results {
        protected double value;
        protected double errorEstimate;
        protected Map<String, Object> additionalResults;
        
        protected InstrumentResults() {
        	additionalResults = new TreeMap<String, Object>(); // FIXME: use some FastUtil class ?
        	reset();
        }
        
        public void reset() {
              value = errorEstimate = Double.NaN;
              additionalResults.clear();
        }
    }


}