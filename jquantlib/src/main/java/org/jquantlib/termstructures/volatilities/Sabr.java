package org.jquantlib.termstructures.volatilities;

import static org.jquantlib.math.Closeness.isClose;

import org.jquantlib.math.Constants;
import org.jquantlib.number.Rate;
import org.jquantlib.number.Time;


/**
 * Implements the Black equivalent volatility for the S.A.B.R. model.
 * 
 * @author <Richard Gomes>
 *
 */
public class Sabr {

	/**
	 * Computes the Black equivalent volatility without validating parameters
	 * 
	 * @param strike
	 * @param forward
	 * @param expiryTime
	 * @param alpha
	 * @param beta
	 * @param nu
	 * @param rho
	 * 
	 * @return Black equivalent volatility
	 * 
	 * @see #validateSabrParameters(Real, Real, Real, Real)
	 * @see #sabrVolatility(Rate, Rate, Time, Real, Real, Real, Real)
	 */
    public double unsafeSabrVolatility(double strike,
            double forward,
            double expiryTime,
            double alpha,
            double beta,
            double nu,
            double rho) {
    
        final double oneMinusBeta = 1.0-beta;
        final double A = Math.pow(forward*strike, oneMinusBeta);
        final double sqrtA= Math.sqrt(A);
        double logM;
        if (!isClose(forward, strike))
            logM = Math.log(forward/strike);
        else {
            final double epsilon = (forward-strike)/strike;
            logM = epsilon - .5 * epsilon * epsilon ;
        }
        final double z = (nu/alpha)*sqrtA*logM;
        final double B = 1.0-2.0*rho*z+z*z;
        final double C = oneMinusBeta*oneMinusBeta*logM*logM;
        final double tmp = (Math.sqrt(B)+z-rho)/(1.0-rho);
        final double xx = Math.log(tmp);
        final double D = sqrtA*(1.0+C/24.0+C*C/1920.0);
        final double d = 1.0 + expiryTime *
            (oneMinusBeta*oneMinusBeta*alpha*alpha/(24.0*A)
                                + 0.25*rho*beta*nu*alpha/sqrtA
                                    +(2.0-3.0*rho*rho)*(nu*nu/24.0));

        double multiplier;
        // computations become precise enough if the square of z worth
        // slightly more than the precision machine (hence the m)
        final double m = 10;
        if (Math.abs(z*z)>Constants.QL_EPSILON * m) 
            multiplier = z/xx;
        else {
            double talpha = (0.5-rho*rho)/(1.0-rho);
            double tbeta = alpha - .5;
            double tgamma = rho/(1-rho);
            multiplier = 1.0 - beta*z + (tgamma - talpha + tbeta*tbeta*.5)*z*z;
        }
        return (alpha/D)*multiplier*d;

    }
    
    /**
     * checks that the parameters are valid; specifically,
     * <ol>
     * <li><code>alpha</code> > 0.0</li>
     * <li><code>beta</code> >= 0.0 && <=1.0</li>
     * <li><code>nu</code> >= 0.0</li>
     * <li><code>rho*rho</code> < 1.0 </li>
     * </ol>
     * @param alpha
     * @param beta
     * @param nu
     * @param rho
     */
    public void validateSabrParameters(double alpha,
            double beta,
            double nu,
            double rho) {
    	//FIXME don't spent time constructing string until the error is real...
    	if (!(alpha>0.0)){
    		throw new ArithmeticException("alpha must be positive: "
    							+ alpha + " not allowed");
    	}

    	if (!(beta>=0.0 && beta<=1.0)){
    		throw new ArithmeticException("beta must be in (0.0, 1.0): "
                    + beta + " not allowed");
    	}
    	if (!(nu>=0.0)){
    		throw new ArithmeticException("nu must be non negative: "
        			+ nu + " not allowed");
    	}
    	if (!(rho*rho<1.0)){
        	throw new ArithmeticException("rho square must be less than one: "
                    + rho + " not allowed");
    	}
    }

    /**
     * 
     * Computes the S.A.B.R. volatility 
     * <p>
     * Checks S.A.B.R. model parameters using {@code #validateSabrParameters(Real, Real, Real, Real)} 
     * <p>
     * Checks the terms and conditions;
     * <ol>
     * <li><code>strike</code> > 0.0</li>
     * <li><code>forward</code> > 0.0</li>
     * <li><code>expiryTime</code> >= 0.0</li>
     * </ol>
     *  @param strike
     * @param forward
     * @param expiryTime
     * @param alpha
     * @param beta
     * @param nu
     * @param rho
     * @return
     * 
     * @see #unsafeSabrVolatility(Rate, Rate, Time, Real, Real, Real, Real)
     * @see #validateSabrParameters(Real, Real, Real, Real)
     */
    public double sabrVolatility(double strike,
            double forward,
            double expiryTime,
            double alpha,
            double beta,
            double nu,
            double rho) {
    	if (!(strike>0.0)){
    		throw new ArithmeticException("strike must be positive: "
    					+ strike + " not allowed");
    	}
    	
    	if (!(forward>0.0)){
    		throw new ArithmeticException("forward must be positive: "
                    + forward + " not allowed");
    	}
    	
    	if (!(expiryTime>=0.0)){
    		throw new ArithmeticException("expiry time must be non-negative: "
                    + expiryTime + " not allowed");
    	}

    	validateSabrParameters(alpha, beta, nu, rho);
    	return unsafeSabrVolatility(strike, forward, expiryTime,
                        alpha, beta, nu, rho);
    }
    
}
