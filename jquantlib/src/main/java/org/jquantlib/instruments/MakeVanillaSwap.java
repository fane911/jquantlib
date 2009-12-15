package org.jquantlib.instruments;

import org.jquantlib.QL;
import org.jquantlib.Settings;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.daycounters.Thirty360;
import org.jquantlib.indexes.IborIndex;
import org.jquantlib.pricingengines.PricingEngine;
import org.jquantlib.pricingengines.swap.DiscountingSwapEngine;
import org.jquantlib.quotes.Handle;
import org.jquantlib.termstructures.YieldTermStructure;
import org.jquantlib.time.BusinessDayConvention;
import org.jquantlib.time.Calendar;
import org.jquantlib.time.Date;
import org.jquantlib.time.DateGeneration;
import org.jquantlib.time.Period;
import org.jquantlib.time.Schedule;
import org.jquantlib.time.TimeUnit;

// TODO: code review :: Please complete this class and perform another code review.
// TODO: code review :: license, class comments, comments for access modifiers, comments for @Override
// TODO: consider refactoring this class and make it an inner class
public class MakeVanillaSwap {


    private final Period swapTenor_;
    private final IborIndex iborIndex_;
    private final /*Rate*/ double fixedRate_;
    private final Period forwardStart_;

    private Date effectiveDate_, terminationDate_;
    private Calendar fixedCalendar_, floatCalendar_;

    private VanillaSwap.Type type_;
    private /*Real*/double nominal_;
    private Period fixedTenor_, floatTenor_;
    private BusinessDayConvention fixedConvention_, fixedTerminationDateConvention_;
    private BusinessDayConvention floatConvention_, floatTerminationDateConvention_;
    private DateGeneration.Rule fixedRule_, floatRule_;
    private boolean fixedEndOfMonth_, floatEndOfMonth_;
    private Date fixedFirstDate_, fixedNextToLastDate_;
    private Date floatFirstDate_, floatNextToLastDate_;
    private /*Spread*/double floatSpread_;
    private DayCounter fixedDayCount_, floatDayCount_;
    private PricingEngine engine_;

    public MakeVanillaSwap (
            final Period swapTenor,
            final IborIndex index) {
        this(swapTenor, index, 0.0, new Period(0,TimeUnit.Days));
    }

    public MakeVanillaSwap (
            final Period swapTenor,
            final IborIndex index,
            final /*Rate*/ double fixedRate) {
        this(swapTenor, index, fixedRate, new Period(0,TimeUnit.Days));
    }

    public MakeVanillaSwap(
            final Period swapTenor,
            final IborIndex index,
            final /* @Rate */ double fixedRate,
            final Period forwardStart) 
    {
        this.swapTenor_ = (swapTenor);
        iborIndex_ = (index);
        fixedRate_ = (fixedRate);
        forwardStart_ = (forwardStart);
        //effectiveDate_ = Date.maxDate();
        fixedCalendar_ = (index.fixingCalendar());
        floatCalendar_ = (index.fixingCalendar());
        type_ = (VanillaSwap.Type.Payer);
        nominal_ = (1.0);
        fixedTenor_ = (new Period(1, TimeUnit.Years));
        floatTenor_ = (index.tenor());
        fixedConvention_ = (BusinessDayConvention.ModifiedFollowing);
        fixedTerminationDateConvention_ = (BusinessDayConvention.ModifiedFollowing);
        floatConvention_ = (index.businessDayConvention());
        floatTerminationDateConvention_ = (index.businessDayConvention());
        fixedRule_ = (DateGeneration.Rule.Backward);
        floatRule_ = (DateGeneration.Rule.Backward);
        fixedEndOfMonth_ = (false);
        floatEndOfMonth_ = (false);
        //FIXME : JM port from quantlib
        //fixedFirstDate_ = Date.maxDate();
        //fixedNextToLastDate_ = Date.maxDate();
        //floatFirstDate_ = Date.maxDate();
        //floatNextToLastDate_ = Date.maxDate();
        floatSpread_ = (0.0);
        fixedDayCount_ = (new Thirty360());
        floatDayCount_ = (index.dayCounter());
        engine_ = new DiscountingSwapEngine(index.termStructure());
    }


    public VanillaSwap value() /* @ReadOnly */ {


        if (System.getProperty("EXPERIMENTAL") == null) {
            throw new UnsupportedOperationException("Work in progress");
        }


        Date startDate;
        if (effectiveDate_ != null)
        {
            startDate = effectiveDate_;
        }
        else
        {
            int fixingDays = iborIndex_.fixingDays();
            Date referenceDate = new Settings().evaluationDate();
            Date spotDate = floatCalendar_.advance (referenceDate, fixingDays, TimeUnit.Days);
            startDate = spotDate.add (forwardStart_);
        }
        
        Date endDate;
        if (terminationDate_ != null)
        {
            endDate = terminationDate_;
        }
        else
        {
            endDate = startDate.add (swapTenor_);
        }   
        
        Schedule fixedSchedule = new Schedule(startDate, endDate,
              fixedTenor_, fixedCalendar_,
              fixedConvention_,
              fixedTerminationDateConvention_,
              fixedRule_, fixedEndOfMonth_,
              fixedFirstDate_, fixedNextToLastDate_);
        
        Schedule floatSchedule = new Schedule(startDate, endDate,
              floatTenor_, floatCalendar_,
              floatConvention_,
              floatTerminationDateConvention_,
              floatRule_ , floatEndOfMonth_,
              floatFirstDate_, floatNextToLastDate_);
        
        double usedFixedRate = fixedRate_;

        if (Double.isNaN (fixedRate_))
        {
            QL.require(!iborIndex_.termStructure().empty(),  "no forecasting term structure set to " +
                   iborIndex_.name());

            VanillaSwap temp = new VanillaSwap(type_, nominal_,
                                               fixedSchedule, 0.0, fixedDayCount_,
                                               floatSchedule, iborIndex_,
                                               floatSpread_, floatDayCount_,
                                               BusinessDayConvention.Following);
            
            // ATM on the forecasting curve
            temp.setPricingEngine(new DiscountingSwapEngine(iborIndex_.termStructure()));
            usedFixedRate = temp.fairRate();
        }
        
        VanillaSwap swap = new VanillaSwap (type_, 
                                            nominal_,
                                            fixedSchedule, 
                                            usedFixedRate,
                                            fixedDayCount_,
                                            floatSchedule,
                                            iborIndex_,
                                            floatSpread_,
                                            floatDayCount_,
                                            BusinessDayConvention.Following);
        swap.setPricingEngine (engine_);
        return swap;
    }
    


    public MakeVanillaSwap receiveFixed(final boolean flag) {
        type_ = flag ? VanillaSwap.Type.Receiver : VanillaSwap.Type.Payer;
        return this;
    }

    public MakeVanillaSwap withType(final VanillaSwap.Type type) {
        type_ = type;
        return this;
    }

    public MakeVanillaSwap withNominal(/* Real */final double n) {
        nominal_ = n;
        return this;
    }

    public MakeVanillaSwap withEffectiveDate(final Date effectiveDate) {
        effectiveDate_ = effectiveDate;
        return this;
    }

    public MakeVanillaSwap withTerminationDate(final Date terminationDate) {
        terminationDate_ = terminationDate;
        return this;
    }

    public MakeVanillaSwap withRule(final DateGeneration.Rule r) {
        fixedRule_ = r;
        floatRule_ = r;
        return this;
    }

    public MakeVanillaSwap withDiscountingTermStructure(final Handle<YieldTermStructure> discountingTermStructure) {
        engine_ = (new DiscountingSwapEngine(discountingTermStructure));
        return this;
    }

    public MakeVanillaSwap withFixedLegTenor(final Period t) {
        fixedTenor_ = t;
        return this;
    }

    public MakeVanillaSwap withFixedLegCalendar(final Calendar cal) {
        fixedCalendar_ = cal;
        return this;
    }

    public MakeVanillaSwap withFixedLegConvention(final BusinessDayConvention bdc) {
        fixedConvention_ = bdc;
        return this;
    }

    public MakeVanillaSwap withFixedLegTerminationDateConvention(final BusinessDayConvention bdc) {
        fixedTerminationDateConvention_ = bdc;
        return this;
    }

    public MakeVanillaSwap withFixedLegRule(final DateGeneration.Rule r) {
        fixedRule_ = r;
        return this;
    }

    public MakeVanillaSwap withFixedLegEndOfMonth(final boolean flag) {
        fixedEndOfMonth_ = flag;
        return this;
    }

    public MakeVanillaSwap withFixedLegFirstDate(final Date d) {
        fixedFirstDate_ = d;
        return this;
    }

    public MakeVanillaSwap withFixedLegNextToLastDate(final Date d) {
        fixedNextToLastDate_ = d;
        return this;
    }

    public MakeVanillaSwap withFixedLegDayCount(final DayCounter dc) {
        fixedDayCount_ = dc;
        return this;
    }

    public MakeVanillaSwap withFloatingLegTenor(final Period t) {
        floatTenor_ = t;
        return this;
    }

    public MakeVanillaSwap withFloatingLegCalendar(final Calendar cal) {
        floatCalendar_ = cal;
        return this;
    }

    public MakeVanillaSwap withFloatingLegConvention(final BusinessDayConvention bdc) {
        floatConvention_ = bdc;
        return this;
    }

    public MakeVanillaSwap withFloatingLegTerminationDateConvention(final BusinessDayConvention bdc) {
        floatTerminationDateConvention_ = bdc;
        return this;
    }

    public MakeVanillaSwap withFloatingLegRule(final DateGeneration.Rule r) {
        floatRule_ = r;
        return this;
    }

    public MakeVanillaSwap withFloatingLegEndOfMonth(final boolean flag) {
        floatEndOfMonth_ = flag;
        return this;
    }

    public MakeVanillaSwap withFloatingLegFirstDate(final Date d) {
        floatFirstDate_ = d;
        return this;
    }

    public MakeVanillaSwap withFloatingLegNextToLastDate(final Date d) {
        floatNextToLastDate_ = d;
        return this;
    }

    public MakeVanillaSwap withFloatingLegDayCount(final DayCounter dc) {
        floatDayCount_ = dc;
        return this;
    }

    public MakeVanillaSwap withFloatingLegSpread(/* Spread */final double sp) {
        floatSpread_ = sp;
        return this;
    }

}
