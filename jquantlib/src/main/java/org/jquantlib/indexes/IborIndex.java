/*
 Copyright (C) 2007 Srinivas Hasti

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

package org.jquantlib.indexes;


import org.jquantlib.QL;
import org.jquantlib.currencies.Currency;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.lang.exceptions.LibraryException;
import org.jquantlib.quotes.Handle;
import org.jquantlib.termstructures.YieldTermStructure;
import org.jquantlib.time.BusinessDayConvention;
import org.jquantlib.time.Calendar;
import org.jquantlib.time.Date;
import org.jquantlib.time.Period;

/**
 * base class for Inter-Bank-Offered-Rate indexes (e.g. %Libor, etc.)
 *
 * @author Srinivas Hasti
 *
 */
// TODO: code review :: please verify against QL/C++ code
// TODO: code review :: license, class comments, comments for access modifiers, comments for @Override
public class IborIndex extends InterestRateIndex {

    private final BusinessDayConvention convention;
    private final Handle<YieldTermStructure> handle;
    private final boolean endOfMonth;

    public IborIndex(
            final String familyName,
            final Period tenor,
            final /*@Natural*/ int fixingDays,
            final Currency currency,
            final Calendar fixingCalendar,
            final BusinessDayConvention convention,
            final boolean endOfMonth,
            final DayCounter dayCounter,
            final Handle<YieldTermStructure> handle) {
        super(familyName, tenor, fixingDays, fixingCalendar, currency, dayCounter);
        this.convention = convention;
        this.handle = handle;
        this.endOfMonth = endOfMonth;
        if (handle != null) {
            handle.addObserver(this);
            //XXX:registerWith
            //registerWith(handle.getLink());
        }
    }

    public IborIndex(
            final String familyName,
            final Period tenor,
            final /*@Natural*/ int fixingDays,
            final Calendar fixingCalendar,
            final Currency currency,
            final BusinessDayConvention convention,
            final boolean endOfMonth,
            final DayCounter dayCounter) {
        super(familyName, tenor, fixingDays, fixingCalendar, currency, dayCounter);
        this.convention = convention;
        this.handle = null;
        this.endOfMonth = endOfMonth;
    }


    public IborIndex clone(final Handle<YieldTermStructure> handle) {
        final IborIndex clone = new IborIndex(
                this.familyName(),
                this.tenor(),
                this.fixingDays(),
                this.currency(),
                this.fixingCalendar(),
                this.convention,
                this.endOfMonth,
                this.dayCounter,
                handle);
        return clone;
    }



    protected static BusinessDayConvention euriborConvention(final Period p) {
        switch (p.units()) {
        case Days:
        case Weeks:
            return BusinessDayConvention.Following;
        case Months:
        case Years:
            return BusinessDayConvention.ModifiedFollowing;
        default:
            throw new LibraryException("invalid time units"); // QA:[RG]::verified // TODO: message
        }
    }

    protected static boolean euriborEOM(final Period p) {
        switch (p.units()) {
        case Days:
        case Weeks:
            return false;
        case Months:
        case Years:
            return true;
        default:
            throw new LibraryException("invalid time units");  // QA:[RG]::verified // TODO: message
        }
    }














    /*
     * (non-Javadoc)
     *
     * @see org.jquantlib.indexes.InterestRateIndex#forecastFixing(org.jquantlib.util.Date)
     */
    @Override
    protected double forecastFixing(final Date fixingDate) {
        // TODO: code review :: please verify against QL/C++ code
        QL.require(! handle.empty() , "no forecasting term structure set to " + name());  // QA:[RG]::verified // TODO: message
        final Date fixingValueDate = valueDate(fixingDate);
        final Date endValueDate = maturityDate(fixingValueDate);
        final double fixingDiscount = handle.currentLink().discount(fixingValueDate);
        final double endDiscount = handle.currentLink().discount(endValueDate);
        final double fixingPeriod = dayCounter().yearFraction(fixingValueDate, endValueDate);
        return (fixingDiscount / endDiscount - 1.0) / fixingPeriod;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jquantlib.indexes.InterestRateIndex#getTermStructure()
     */
    @Override
    public Handle<YieldTermStructure> termStructure() {
        return handle;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jquantlib.indexes.InterestRateIndex#maturityDate(org.jquantlib.util.Date)
     */
    @Override
    public Date maturityDate(final Date valueDate) {
        return fixingCalendar().advance(valueDate, tenor(), convention, endOfMonth);
    }

    public BusinessDayConvention businessDayConvention() {
        return convention;
    }

    public boolean endOfMonth() {
        return endOfMonth;
    }

}
