/*
 Copyright (C) 2007 Srinivas Hasti

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
package org.jquantlib.pricingengines.vanilla.finitedifferences;

import java.util.Vector;

import org.jquantlib.instruments.Payoff;
import org.jquantlib.instruments.StrikedTypePayoff;
import org.jquantlib.math.Array;
import org.jquantlib.math.SampledCurve;
import org.jquantlib.methods.finitedifferences.BoundaryCondition;
import org.jquantlib.methods.finitedifferences.NeumannBC;
import org.jquantlib.methods.finitedifferences.OperatorFactory;
import org.jquantlib.methods.finitedifferences.TridiagonalOperator;
import org.jquantlib.pricingengines.arguments.Arguments;
import org.jquantlib.pricingengines.arguments.OneAssetOptionArguments;
import org.jquantlib.processes.GeneralizedBlackScholesProcess;
import org.jquantlib.util.Date;

/**
 * @author Srinivas Hasti
 * 
 */
public class FDVanillaEngine {
	protected GeneralizedBlackScholesProcess process;
	protected/* Size */int timeSteps, gridPoints;
	protected boolean timeDependent;
	protected/* Real */double requiredGridValue;
	protected Date exerciseDate;
	protected Payoff payoff;
	protected TridiagonalOperator finiteDifferenceOperator;
	protected SampledCurve intrinsicValues;
	protected Vector<BoundaryCondition<TridiagonalOperator>> bcS;
	// temporaries
	protected/* Real */double sMin, center, sMax;

	public final static/* Real */double safetyZoneFactor = 1.1;

	public FDVanillaEngine(GeneralizedBlackScholesProcess process,
			int timeSteps, int gridPoints, boolean timeDependent) {
		this.process = process;
		this.timeSteps = timeSteps;
		this.gridPoints = gridPoints;
		this.timeDependent = timeDependent;
		bcS = new Vector<BoundaryCondition<TridiagonalOperator>>();
	}

	public Array grid() {
		return intrinsicValues.grid();
	}

	protected void setGridLimits() {
		setGridLimits(process.stateVariable().getLink().evaluate(),
				getResidualTime());
		ensureStrikeInGrid();
	}

	protected void setupArguments(Arguments a) {
		OneAssetOptionArguments args = (OneAssetOptionArguments) a;
		exerciseDate = args.exercise.lastDate();
		payoff = args.payoff;
		requiredGridValue = ((StrikedTypePayoff) (payoff)).getStrike();
	}

	protected void setGridLimits(/* Real */double center, /* Time */double t) {
		if (center <= 0.0)
			throw new IllegalStateException("negative or null underlying given");
		this.center = center;
		/* Size */int newGridPoints = safeGridPoints(gridPoints, t);
		if (newGridPoints > intrinsicValues.size()) {
			intrinsicValues = new SampledCurve(newGridPoints);
		}

		/* Real */double volSqrtTime = Math.sqrt(process.blackVolatility()
				.getLink().blackVariance(t, center));

		// the prefactor fine tunes performance at small volatilities
		/* Real */double prefactor = 1.0 + 0.02 / volSqrtTime;
		/* Real */double minMaxFactor = Math.exp(4.0 * prefactor * volSqrtTime);
		sMin = center / minMaxFactor; // underlying grid min value
		sMax = center * minMaxFactor; // underlying grid max value
	}

	protected void ensureStrikeInGrid() {
		// ensure strike is included in the grid
		StrikedTypePayoff striked_payoff = (StrikedTypePayoff) (payoff);
		if (striked_payoff == null)
			return;
		/* Real */double requiredGridValue = striked_payoff.getStrike();

		if (sMin > requiredGridValue / safetyZoneFactor) {
			sMin = requiredGridValue / safetyZoneFactor;
			// enforce central placement of the underlying
			sMax = center / (sMin / center);
		}
		if (sMax < requiredGridValue * safetyZoneFactor) {
			sMax = requiredGridValue * safetyZoneFactor;
			// enforce central placement of the underlying
			sMin = center / (sMax / center);
		}
	}

	protected void initializeInitialCondition() {
		intrinsicValues.setLogGrid(sMin, sMax);
		intrinsicValues.sample(payoff);
	}

	protected void initializeOperator() {
		finiteDifferenceOperator = OperatorFactory.getOperator(process,
				intrinsicValues.grid(), getResidualTime(), timeDependent);
	}

	protected void initializeBoundaryConditions() {

		bcS.add(new NeumannBC(intrinsicValues.value(1)
				- intrinsicValues.value(0), NeumannBC.Side.LOWER));

		bcS.add(new NeumannBC(intrinsicValues
				.value(intrinsicValues.size() - 1)
				- intrinsicValues.value(intrinsicValues.size() - 2),
				NeumannBC.Side.UPPER));

	}

	protected/* Time */double getResidualTime() {
		return process.getTime(exerciseDate);
	}

	// safety check to be sure we have enough grid points.
	protected/* Size */int safeGridPoints(/* Size */int gridPoints,
	/* Time */double residualTime) {
		final int minGridPoints = 10;
		final int minGridPointsPerYear = 2;
		return Math
				.max(
						gridPoints,
						residualTime > 1.0 ? (int) ((minGridPoints + (residualTime - 1.0)
								* minGridPointsPerYear))
								: minGridPoints);
	}
}