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
 Copyright (C) 2004, 2005, 2006 StatPro Italia srl

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

package org.jquantlib.termstructures;

import org.jquantlib.daycounters.Actual365Fixed;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.time.Calendar;
import org.jquantlib.time.Frequency;
import org.jquantlib.time.Period;
import org.jquantlib.time.TimeUnit;
import org.jquantlib.util.Date;

/**
 * This abstract class defines the interface of concrete rate structures which
 * will be derived from this one.
 * 
 * <p>
 * Rates are assumed to be annual continuous compounding.
 */
// TODO: add derived class ParSwapTermStructure similar to
// ZeroYieldTermStructure, DiscountStructure, ForwardRateStructure
// TEST: observability against evaluation date changes is checked.
public abstract class YieldTermStructure extends TermStructure {

	protected abstract /*DiscountFactor*/ double discountImpl(final /*@Time*/ double t);
	
	
	/**
	 * @see TermStructure#TermStructure() documentation for issues regarding
	 *      constructors.
	 */
	protected YieldTermStructure() {
		this(new Actual365Fixed());
	}

	/**
	 * Term structures initialized by means of this constructor must manage
	 * their own reference date by overriding the getReferenceDate() method.
	 * 
	 * @see TermStructure#TermStructure() documentation for issues regarding
	 *      constructors.
	 */
	protected YieldTermStructure(final DayCounter dc) {
		super(dc);
	}

	/**
	 * Initialize with a fixed reference date
	 * 
	 * @see TermStructure#TermStructure() documentation for issues regarding
	 *      constructors.
	 */
	protected YieldTermStructure(final Date referenceDate, final Calendar cal, final DayCounter dc) {
		super(referenceDate, cal, dc);
	}

	/**
	 * Calculate the reference date based on the global evaluation date
	 * 
	 * @see TermStructure#TermStructure() documentation for issues regarding
	 *      constructors.
	 */
	protected YieldTermStructure(int settlementDays, final Calendar cal, final DayCounter dc) {
		super(settlementDays, cal, dc);
	}

	// These methods return the implied zero-yield rate for a
	// given date or time. In the former case, the double is
	// calculated as a fraction of year from the reference date.};





	/**
	 * The resulting interest rate has the required day-counting rule.
	 */
	public final InterestRate getZeroRate(final Date d, final DayCounter resultDayCounter, final Compounding comp) {
		return getZeroRate(d, resultDayCounter, comp, Frequency.Annual);
	}

	public final InterestRate getZeroRate(final Date d, final DayCounter resultDayCounter, final Compounding comp, final Frequency freq) {
		return getZeroRate(d, resultDayCounter, comp, freq, false);
	}

	protected final InterestRate getZeroRate(final Date d, final DayCounter dayCounter, final Compounding comp, final Frequency freq, boolean extrapolate) {
		if (d == getReferenceDate()) {
			/*@Time*/ double t = 0.0001;
			/*@CompoundFactor*/ double compound = 1/getDiscount(t, extrapolate); // 1/discount(t,extrapolate)
			return InterestRate.getImpliedRate(compound, t, dayCounter, comp, freq);
		} else {
			/*@CompoundFactor*/ double compound = 1/getDiscount(d, extrapolate); // 1/discount(d,extrapolate)
			return InterestRate.getImpliedRate(compound, getReferenceDate(), d, dayCounter, comp, freq);
		}
	}

	/**
	 * The resulting interest rate has the same day-counting rule used by the
	 * term structure. The same rule should be used for calculating the passed
	 * double t.
	 */
	protected InterestRate getZeroRate(final /*@Time*/ double  time, final Compounding comp, final Frequency freq, boolean extrapolate) {
		/*@Time*/ double t = time;
		if (t==0.0) {
			t = 0.0001;
		}
		/*@CompoundFactor*/ double compound = 1/getDiscount(t, extrapolate); // 1/discount(t,extrapolate)
		return InterestRate.getImpliedRate(compound, time, this.getDayCounter(), comp, freq);
	}

	/**
	 * Forward rates
	 * 
	 * <p>
	 * These methods returns the implied forward interest rate between two dates
	 * or times. In the former case, times are calculated as fractions of year
	 * from the reference date. The resulting interest rate has the required
	 * day-counting rule.
	 * 
	 * <p>
	 * Dates are not adjusted for holidays
	 */
	public InterestRate getForwardRate(final Date d1, final Date d2, final DayCounter resultDayCounter, final Compounding comp) {
		return getForwardRate(d1, d2, resultDayCounter, comp, Frequency.Annual);
	}

	public InterestRate getForwardRate(final Date d1, final Date d2, final DayCounter resultDayCounter, final Compounding comp, final Frequency freq) {
		return getForwardRate(d1, d2, resultDayCounter, comp, freq, false);
	}

	protected InterestRate getForwardRate(final Date d1, final Date d2, final DayCounter dayCounter, final Compounding comp, final Frequency freq, boolean extrapolate) {
		if (d1.eq(d2)) {
			/*@Time*/ double  t1 = getTimeFromReference(d1);
			/*@Time*/ double  t2 = t1+0.0001;
			/*@Time*/ double  delta = t2-t1;
			/*@DiscountFactor*/ double factor1 = getDiscount(t1, extrapolate);
			/*@DiscountFactor*/ double factor2 = getDiscount(t2, extrapolate);
			/*@CompoundFactor*/ double compound = factor1 / factor2;
			return InterestRate.getImpliedRate(compound, delta, dayCounter, comp, freq);
		} else if (d1.lt(d2)) {
			/*@DiscountFactor*/ double discount1 = getDiscount(d1, extrapolate);
			/*@DiscountFactor*/ double discount2 = getDiscount(d2, extrapolate);
			/*@CompoundFactor*/ double compound = discount1 / discount2;
			return InterestRate.getImpliedRate(compound, d1, d2, dayCounter, comp, freq);
		} else {
			throw new IllegalArgumentException(d1 + " later than " + d2);
		}
	}

	protected InterestRate getForwardRate(final Date d, final Period p, final DayCounter resultDayCounter, Compounding comp) {
		return getForwardRate(d, p, resultDayCounter, comp);
	}

	protected InterestRate getForwardRate(final Date d, final Period p, final DayCounter resultDayCounter, Compounding comp, Frequency freq) {
		return getForwardRate(d, p, resultDayCounter, comp, freq, false);
	}

	protected InterestRate getForwardRate(final Date d, final Period p, final DayCounter dayCounter, final Compounding comp, final Frequency freq, boolean extrapolate) {
		return getForwardRate(d, d.add(p), dayCounter, comp, freq, extrapolate);
	}

	/**
	 * @see YieldTermStructure#forwardRate(Date, Date, DayCounter,
	 *      org.jquantlib.termstructures.InterestRate.Compounding, Frequency)
	 */
	public InterestRate getForwardRate(final /*@Time*/ double  t1, final /*@Time*/ double  t2, final Compounding comp) {
		return getForwardRate(t1, t2, comp, Frequency.Annual);
	}

	/**
	 * @see YieldTermStructure#forwardRate(Date, Date, DayCounter,
	 *      org.jquantlib.termstructures.InterestRate.Compounding, Frequency)
	 */
	public InterestRate getForwardRate(final /*@Time*/ double  t1, final /*@Time*/ double t2, final Compounding comp, final Frequency freq) {
		return getForwardRate(t1, t2, comp, freq, false);
	}

	/**
	 * The resulting interest rate has the same day-counting rule used by the
	 * term structure. The same rule should be used for the calculating the
	 * passed times t1 and t2.
	 */
	// FIXME; this method is clearly buggy
	public InterestRate getForwardRate(final /*@Time*/ double  time1, final /*@Time*/ double  time2, final Compounding comp, final Frequency freq, boolean extrapolate) {
		/*@Time*/ double t1 = time1;
		/*@Time*/ double t2 = time2;
		if (t2==t1) t2 = t1+0.0001;
		if (t1<=t2) throw new IllegalArgumentException("t1 (" + t1 + ") < t2 (" + t2 + ")");
		/*@DiscountFactor*/ double discount1 = getDiscount(t1, extrapolate);
		/*@DiscountFactor*/ double discount2 = getDiscount(t2, extrapolate);
		/*@CompoundFactor*/ double compound = discount1 / discount2;
		/*@Time*/ double delta = t2-t1;
		return InterestRate.getImpliedRate(compound, delta, this.getDayCounter(), comp, freq);
	}

	/**
	 * About par rates
	 * 
	 * <p>
	 * These methods returns the implied par rate for a given sequence of
	 * payments at the given dates or times. In the former case, times are
	 * calculated as fractions of year from the reference date.
	 * 
	 * <p>
	 * <b>Warning:</b> though somewhat related to a swap rate, this method is
	 * not to be used for the fair rate of a real swap, since it does not take
	 * into account all the market conventions' details. The correct way to
	 * evaluate such rate is to instantiate a SimpleSwap with the correct
	 * conventions, pass it the term structure and call the swap's fairRate()
	 * method.
	 */
	protected /*@Rate*/ double getParRate(int tenor, final Date startDate, final Frequency freq, boolean extrapolate) {
		Date[] dates = new Date[tenor + 1];
		dates[0] = startDate;
		for (int i = 1; i <= tenor; i++)
			dates[i] = startDate.add(new Period(i, TimeUnit.Years));
		return getParRate(dates, freq, extrapolate);
	}

	/**
	 * @param dates
	 * @param freq
	 * @param extrapolate
	 * @return the first date in the vector must equal the start date; the
	 *         following dates must equal the payment dates.
	 * @see YieldTermStructure#parRate(int, Date, Frequency, boolean)
	 */
	protected /*@Rate*/ double getParRate(final Date[] dates, final Frequency freq, boolean extrapolate) {
		/*@Time*/ double [] times = new /*@Time*/ double [dates.length];
		for (int i = 0; i < dates.length; i++)
			times[i] = getTimeFromReference(dates[i]);
		return getParRate(times, freq, extrapolate);
	}

	/**
	 * @return the first double in the vector must equal the start time; the
	 *         following times must equal the payment times.
	 * @see YieldTermStructure#parRate(int, Date, Frequency, boolean)
	 */
	protected /*@Rate*/ double getParRate(final /*@Time*/ double[] times, final Frequency frequency, boolean extrapolate) {
		if (times.length < 2)
			throw new IllegalArgumentException("at least two times are required");
		/*@Time*/ double last = times[times.length - 1];
		checkRange(last, extrapolate);
		/*@DiscountFactor*/ double sum = 0.0;
		for (int i = 1; i < times.length; i++) {
			sum += discountImpl(times[i]);
		}
		/*@Rate*/ double result = discountImpl(times[0]) - discountImpl(last);
		int freq = frequency.toInteger();
		result *= freq/sum;
		return result;
	}

	/**
	 * Discount Factors
	 * 
	 * <p>
	 * These methods return the discount factor for a given date or time. In the
	 * former case, the double is calculated as a fraction of year from the
	 * reference date.
	 */
	public /*@DiscountFactor*/ double getDiscount(final Date d) {
		return getDiscount(d, false);
	}

	public /*@DiscountFactor*/ double getDiscount(final Date d, boolean extrapolate) {
		checkRange(d, extrapolate);
		return discountImpl(getTimeFromReference(d));
	}

	/**
	 * The same day-counting rule used by the term structure should be used for
	 * calculating the passed double t.
	 */
	public /*@DiscountFactor*/ double getDiscount(final /*@Time*/ double t) {
		return getDiscount(t, false);
	}

	public /*@DiscountFactor*/ double getDiscount(final /*@Time*/ double t, boolean extrapolate) {
		checkRange(t, extrapolate);
		return discountImpl(t);
	}

}