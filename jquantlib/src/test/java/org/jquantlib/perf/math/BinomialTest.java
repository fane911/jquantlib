/*
 Copyright (C) 2007 Srinivas Hasti

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
package org.jquantlib.perf.math;

import org.jquantlib.math.distributions.BinomialDistribution;
import org.jquantlib.testsuite.util.StopClock;
import org.junit.Test;

import cern.jet.random.Binomial;

public class BinomialTest {
  StopClock clock = new StopClock(StopClock.Unit.NANO);
 
  
  @Test public void runColt(){
	  System.out.println("Run colt");
	  Binomial binomial = new Binomial(2000,0.9,null);
	  for(int i=0;i<100;i++){
		  clock.reset();
		  clock.startClock();
		  binomial.pdf(1900);
		  clock.stopClock();
	  }
	  
	  double v = 0;
	  clock.reset();
	  clock.startClock();
	  for(int i=0;i<1000;i++){
	   v = binomial.pdf(1900);
	  }
	  clock.stopClock();
	  System.out.println(v);
	  clock.log();
  }
  
  @Test public void runJQ(){
	  System.out.println("\nRun JQ");
	  BinomialDistribution binomial = new BinomialDistribution(0.9,2000);
	  for(int i=0;i<100;i++){
		  clock.reset();
		  clock.startClock();
		  binomial.evaluate(5);
		  clock.stopClock();
	  }
	  
	  double v = 0;
	  clock.reset();
	  clock.startClock();
	  for(int i=0;i<1000;i++){
	   v = binomial.evaluate(1900);
	  }
	  clock.stopClock();
	  System.out.println(v);
	  clock.log();
  }
  
  public static void main(String[] str){
	  BinomialTest binomialTest = new BinomialTest();
	  binomialTest.runJQ();
	  binomialTest.runColt();
	  
  }
}