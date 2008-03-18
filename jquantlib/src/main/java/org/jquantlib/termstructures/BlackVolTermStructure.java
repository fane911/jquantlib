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

package org.jquantlib.termstructures;

import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.time.Calendar;
import org.jquantlib.util.Date;

/**
 * Black-volatility term structure
 * 
 * <p>
 * This abstract class defines the interface of concrete Black-volatility term
 * structures which will be derived from this one.
 * 
 * <p>
 * Volatilities are assumed to be expressed on an annual basis.
 * 
 * @author Richard Gomes
 */
// FIXME: pending review
public abstract class BlackVolTermStructure extends TermStructure {

	static private final double dT = 1.0/365.0;

	/**
	 * The minimum strike for which the term structure can return vols
	 */
	public abstract /*@Price*/ double getMinStrike();

	/**
	 * The maximum strike for which the term structure can return vols
	 */
	public abstract /*@Price*/ double getMaxStrike();

	protected abstract /*@Volatility*/ double blackVolImpl(final /*@Time*/ double maturity, final /*@Price*/ double strike);

	protected abstract /*@Variance*/ double blackVarianceImpl(final /*@Time*/ double maturity, final /*@Price*/ double strike);

	
	
	// Constructors

	public BlackVolTermStructure(final DayCounter dc) {
		super(dc);
	}

	public BlackVolTermStructure(final Date refDate, final Calendar cal, final DayCounter dc) {
		super(refDate, cal, dc);
	}

	public BlackVolTermStructure(int settlDays, final Calendar cal, final DayCounter dc) {
		super(settlDays, cal, dc);
	}

	/**
	 * Present (a.k.a spot) volatility
	 */
	public final /*@Volatility*/ double blackVol(final Date maturity, final /*@Price*/ double strike) {
		return blackVol(maturity, strike, false);
	}

	/**
	 * Present (a.k.a spot) volatility
	 */
	public final /*@Volatility*/ double blackVol(final Date maturity, final /*@Price*/ double strike, boolean extrapolate) {
		/*@Time*/ double t = super.getTimeFromReference(maturity);
		checkRange(t, strike, extrapolate);
		return blackVolImpl(t, strike);
	}

	/**
	 * Present (a.k.a spot) volatility
	 */
	public final /*@Volatility*/ double blackVol(final /*@Time*/ double maturity, final /*@Price*/ double strike) {
		return blackVol(maturity, strike, false);
	}

	/**
	 * Present (a.k.a spot) volatility
	 */
	public final /*@Volatility*/ double blackVol(final /*@Time*/ double maturity, final /*@Price*/ double strike, boolean extrapolate) {
		checkRange(maturity, strike, extrapolate);
		return blackVolImpl(maturity, strike);
	}

	/**
	 * Present (a.k.a spot) variance
	 */
	public final /*@Variance*/ double blackVariance(final Date maturity, final /*@Price*/ double strike) {
		return blackVariance(maturity, strike, false);
	}

	/**
	 * Present (a.k.a spot) variance
	 */
	public final /*@Variance*/ double blackVariance(final Date maturity, final /*@Price*/ double strike, boolean extrapolate) {
		/*@Time*/ double t = super.getTimeFromReference(maturity);
		checkRange(t, strike, extrapolate);
		return blackVarianceImpl(t, strike);
	}

	/**
	 * Present (a.k.a spot) variance
	 */
	public final /*@Variance*/ double blackVariance(final /*@Time*/ double maturity, final /*@Price*/ double strike) {
		return blackVariance(maturity, strike, false);
	}

	/**
	 * Present (a.k.a spot) variance
	 */
	public final /*@Variance*/ double blackVariance(final /*@Time*/ double maturity, final /*@Price*/ double strike, boolean extrapolate) {
		checkRange(maturity, strike, extrapolate);
		return blackVarianceImpl(maturity, strike);
	}

// XXX	
//	protected void accept(final ElementVisitor<BlackVolTermStructure> v) {
//		if (v != null) {
//			v.visit(this);
//		} else {
//			throw new IllegalArgumentException("not a Black-volatility term structure visitor");
//		}
//	}

	private final void checkRange(final /*@Time*/ double time, final /*@Price*/ double strike, boolean extrapolate) {
		super.checkRange(time, extrapolate);
		if (! (extrapolate || allowsExtrapolation() || (strike >= getMinStrike()) && (strike <= getMaxStrike()) ) )
			throw new ArithmeticException("strike (" + strike + ") is outside the curve domain [" + getMinStrike() + "," + getMaxStrike() + "]");
	}

	/**
	 * Future (a.k.a. forward) volatility
	 * 
	 * @param date1
	 * @param date2
	 * @param strike
	 * @param extrapolate
	 * @return
	 */
	public final /*@Volatility*/ double blackForwardVol(final Date date1, final Date date2, final /*@Price*/ double strike, boolean extrapolate) {
		if (date1.gt(date2))
			throw new IllegalArgumentException(date1 + " later than " + date2);
		/*@Time*/ double time1 = getTimeFromReference(date1);
		/*@Time*/ double time2 = getTimeFromReference(date2);
		return blackForwardVol(time1, time2, strike, extrapolate);
	}

	/**
	 * Future (a.k.a. forward) volatility
	 * 
	 * @param time1
	 * @param time2
	 * @param strike
	 * @param extrapolate
	 * @return
	 */
	public final /*@Volatility*/ double blackForwardVol(final /*@Time*/ double time1, final /*@Time*/ double time2, final /*@Price*/ double strike, boolean extrapolate) {
		/*@Time*/ double t1 = time1;
		/*@Time*/ double t2 = time2;
		if (t1>t2) throw new IllegalArgumentException(time1 + " later than " + time2);
		checkRange(time2, strike, extrapolate);
		
		if (t1==t2) {
			if (t1==0.0) {
				/*@Time*/ double epsilon = 1.0e-5;
				/*@Variance*/ double var = blackVarianceImpl(epsilon, strike);
				return Math.sqrt(var/epsilon);
			} else {
				double epsilon = Math.min(1.0e-5, t1);
				/*@Variance*/ double var1 = blackVarianceImpl(t1-epsilon, strike);
				/*@Variance*/ double var2 = blackVarianceImpl(t1+epsilon, strike);
				if (var2<var1) throw new ArithmeticException("variances must be non-decreasing");
				return  Math.sqrt((var2-var1) / (2*epsilon));
			}
		} else {
			/*@Variance*/ double var1 = blackVarianceImpl(time1, strike);
			/*@Variance*/ double var2 = blackVarianceImpl(time2, strike);
			if (var2<var1) throw new ArithmeticException("variances must be non-decreasing");
			return  Math.sqrt((var2-var1)/(t2-t1));
		}
	}

	/**
	 * Future (a.k.a. forward) variance
	 * 
	 * @param date1
	 * @param date2
	 * @param strike
	 * @param extrapolate
	 * @return
	 */
	public final /*@Variance*/ double blackForwardVariance(final Date date1, final Date date2, final /*@Price*/ double strike, boolean extrapolate) {
		if (date1.gt(date2))
			throw new IllegalArgumentException(date1 + " later than " + date2);
		/*@Time*/ double time1 = getTimeFromReference(date1);
		/*@Time*/ double time2 = getTimeFromReference(date2);
		return blackForwardVariance(time1, time2, strike, extrapolate);
	}

	/**
	 * Future (a.k.a. forward) variance
	 * 
	 * @param time1
	 * @param time2
	 * @param strike
	 * @param extrapolate
	 * @return
	 */
	public final /*@Variance*/ double blackForwardVariance(final /*@Time*/ double time1, final /*@Time*/ double time2, final /*@Price*/ double strike, boolean extrapolate) {
		/*@Time*/ double t1 = time1;
		/*@Time*/ double t2 = time2;
		if (t1>t2) throw new IllegalArgumentException(time1 + " later than " + time2);
		checkRange(time2, strike, extrapolate);

		/*@Variance*/ double v1 = blackVarianceImpl(time1, strike);
		/*@Variance*/ double v2 = blackVarianceImpl(time2, strike);
		if (v2<v1) throw new ArithmeticException("variances must be non-decreasing");
		return v2-v1;
	}

}