/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package cern.colt.matrix.impl;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cern.colt.Utils;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DComplexMatrix1D;
import cern.colt.matrix.DComplexMatrix2D;
import cern.colt.matrix.DComplexMatrix3D;
import cern.colt.matrix.DoubleMatrix3D;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_3D;

/**
 * Dense 3-d matrix holding <tt>complex</tt> elements.
 * <p>
 * <b>Implementation:</b>
 * <p>
 * Internally holds one single contigous one-dimensional array, addressed in (in
 * decreasing order of significance): slice major, row major, column major.
 * Complex data is represented by 2 double values in sequence, i.e.
 * elements[idx] constitute the real part and elements[idx+1] constitute the
 * imaginary part, where idx = index(0,0,0) + slice * sliceStride + row *
 * rowStride + column * columnStride. Note that this implementation is not
 * synchronized.
 * <p>
 * <b>Memory requirements:</b>
 * <p>
 * <tt>memory [bytes] = 8*slices()*rows()*2*columns()</tt>. Thus, a
 * 100*100*100 matrix uses 16 MB.
 * <p>
 * <b>Time complexity:</b>
 * <p>
 * <tt>O(1)</tt> (i.e. constant time) for the basic operations <tt>get</tt>,
 * <tt>getQuick</tt>, <tt>set</tt>, <tt>setQuick</tt> and <tt>size</tt>,
 * <p>
 * Applications demanding utmost speed can exploit knowledge about the internal
 * addressing. Setting/getting values in a loop slice-by-slice, row-by-row,
 * column-by-column is quicker than, for example, column-by-column, row-by-row,
 * slice-by-slice. Thus
 * 
 * <pre>
 * for (int slice = 0; slice &lt; slices; slice++) {
 * 	for (int row = 0; row &lt; rows; row++) {
 * 		for (int column = 0; column &lt; columns; column++) {
 * 			matrix.setQuick(slice, row, column, someValue);
 * 		}
 * 	}
 * }
 * </pre>
 * 
 * is quicker than
 * 
 * <pre>
 * for (int column = 0; column &lt; columns; column++) {
 * 	for (int row = 0; row &lt; rows; row++) {
 * 		for (int slice = 0; slice &lt; slices; slice++) {
 * 			matrix.setQuick(slice, row, column, someValue);
 * 		}
 * 	}
 * }
 * </pre>
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
public class DenseDComplexMatrix3D extends DComplexMatrix3D {
	private static final long serialVersionUID = 3008992680697668332L;

	private DoubleFFT_3D fft3;

	/**
	 * The elements of this matrix. elements are stored in slice major, then row
	 * major, then column major, in order of significance. Complex data is
	 * represented by 2 double values in sequence, i.e. elements[idx] constitute
	 * the real part and elements[idx+1] constitute the imaginary part, where
	 * idx = index(0,0,0) + slice * sliceStride + row * rowStride + column *
	 * columnStride.
	 */
	protected double[] elements;

	/**
	 * Constructs a matrix with a copy of the given values. * <tt>values</tt>
	 * is required to have the form
	 * <tt>re = values[slice][row][2*column], im = values[slice][row][2*column+1]</tt>
	 * and have exactly the same number of rows in every slice and exactly the
	 * same number of columns in in every row.
	 * <p>
	 * The values are copied. So subsequent changes in <tt>values</tt> are not
	 * reflected in the matrix, and vice-versa.
	 * 
	 * @param values
	 *            The values to be filled into the new matrix.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>for any 1 &lt;= slice &lt; values.length: values[slice].length != values[slice-1].length</tt>.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>for any 1 &lt;= row &lt; values[0].length: values[slice][row].length != values[slice][row-1].length</tt>.
	 */
	public DenseDComplexMatrix3D(double[][][] values) {
		this(values.length, (values.length == 0 ? 0 : values[0].length), (values.length == 0 ? 0 : values[0].length == 0 ? 0 : values[0][0].length / 2));
		assign(values);
	}

	/**
	 * Constructs a complex matrix with the same size as <tt>realPart</tt>
	 * matrix and fills the real part of this matrix with elements of
	 * <tt>realPart</tt>.
	 * 
	 * @param realPart
	 *            a real matrix whose elements become a real part of this matrix
	 * @throws IllegalArgumentException
	 *             if <tt>(double)slices*columns*rows > Integer.MAX_VALUE</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>slices<0 || rows<0 || columns<0</tt>.
	 */
	public DenseDComplexMatrix3D(DoubleMatrix3D realPart) {
		this(realPart.slices, realPart.rows, realPart.columns);
		assignReal(realPart);
	}

	/**
	 * Constructs a matrix with a given number of slices, rows and columns. All
	 * entries are initially <tt>0</tt>.
	 * 
	 * @param slices
	 *            the number of slices the matrix shall have.
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @throws IllegalArgumentException
	 *             if <tt>(double)slices*columns*rows > Integer.MAX_VALUE</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>slices<0 || rows<0 || columns<0</tt>.
	 */
	public DenseDComplexMatrix3D(int slices, int rows, int columns) {
		setUp(slices, rows, columns, 0, 0, 0, rows * 2 * columns, 2 * columns, 2);
		this.elements = new double[slices * rows * 2 * columns];
	}

	/**
	 * Constructs a view with the given parameters.
	 * 
	 * @param slices
	 *            the number of slices the matrix shall have.
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @param elements
	 *            the cells.
	 * @param sliceZero
	 *            the position of the first element.
	 * @param rowZero
	 *            the position of the first element.
	 * @param columnZero
	 *            the position of the first element.
	 * @param sliceStride
	 *            the number of elements between two slices, i.e.
	 *            <tt>index(k+1,i,j)-index(k,i,j)</tt>.
	 * @param rowStride
	 *            the number of elements between two rows, i.e.
	 *            <tt>index(k,i+1,j)-index(k,i,j)</tt>.
	 * @param columnnStride
	 *            the number of elements between two columns, i.e.
	 *            <tt>index(k,i,j+1)-index(k,i,j)</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>(double)slices*columns*rows > Integer.MAX_VALUE</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>slices<0 || rows<0 || columns<0</tt>.
	 */
	protected DenseDComplexMatrix3D(int slices, int rows, int columns, double[] elements, int sliceZero, int rowZero, int columnZero, int sliceStride, int rowStride, int columnStride) {
		setUp(slices, rows, columns, sliceZero, rowZero, columnZero, sliceStride, rowStride, columnStride);
		this.elements = elements;
		this.isNoView = false;
	}

	public double[] aggregate(final cern.colt.function.DComplexDComplexDComplexFunction aggr, final cern.colt.function.DComplexDComplexFunction f) {
		double[] b = new double[2];
		if (size() == 0) {
			b[0] = Double.NaN;
			b[1] = Double.NaN;
			return b;
		}
		final int zero = index(0, 0, 0);
		double[] a = null;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			double[][] results = new double[np][2];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<double[]>() {

					public double[] call() throws Exception {
						int idx = zero + startslice * sliceStride;
						double[] a = f.apply(new double[] { elements[idx], elements[idx + 1] });
						int d = 1;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								for (int c = d; c < columns; c++) {
									idx = zero + s * sliceStride + r * rowStride + c * columnStride;
									a = aggr.apply(a, f.apply(new double[] { elements[idx], elements[idx + 1] }));
								}
								d = 0;
							}
						}
						return a;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (double[]) futures[j].get();
				}
				a = results[0];
				for (int j = 1; j < np; j++) {
					a = aggr.apply(a, results[j]);
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			a = f.apply(new double[] { elements[zero], elements[zero + 1] });
			int d = 1; // first cell already done
			int idx;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					for (int c = d; c < columns; c++) {
						idx = zero + s * sliceStride + r * rowStride + c * columnStride;
						a = aggr.apply(a, f.apply(new double[] { elements[idx], elements[idx + 1] }));
					}
					d = 0;
				}
			}
		}
		return a;
	}

	public double[] aggregate(final DComplexMatrix3D other, final cern.colt.function.DComplexDComplexDComplexFunction aggr, final cern.colt.function.DComplexDComplexDComplexFunction f) {
		checkShape(other);
		double[] b = new double[2];
		if (size() == 0) {
			b[0] = Double.NaN;
			b[1] = Double.NaN;
			return b;
		}
		final int zero = index(0, 0, 0);
		final int zeroOther = other.index(0, 0, 0);
		final int sliceStrideOther = other.sliceStride;
		final int rowStrideOther = other.rowStride;
		final int colStrideOther = other.columnStride;
		final double[] elemsOther = (double[]) other.getElements();

		double[] a = null;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			double[][] results = new double[np][2];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<double[]>() {

					public double[] call() throws Exception {
						int idx = zero + startslice * sliceStride;
						int idxOther = zeroOther + startslice * sliceStrideOther;
						double[] a = f.apply(new double[] { elements[idx], elements[idx + 1] }, new double[] { elemsOther[idxOther], elemsOther[idxOther + 1] });
						int d = 1;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								for (int c = d; c < columns; c++) {
									idx = zero + s * sliceStride + r * rowStride + c * columnStride;
									idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther + c * colStrideOther;
									a = aggr.apply(a, f.apply(new double[] { elements[idx], elements[idx + 1] }, new double[] { elemsOther[idxOther], elemsOther[idxOther + 1] }));
								}
								d = 0;
							}
						}
						return a;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (double[]) futures[j].get();
				}
				a = results[0];
				for (int j = 1; j < np; j++) {
					a = aggr.apply(a, results[j]);
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			a = f.apply(new double[] { elements[zero], elements[zero + 1] }, new double[] { elemsOther[zeroOther], elemsOther[zeroOther + 1] });
			int d = 1; // first cell already done
			int idx;
			int idxOther;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					for (int c = d; c < columns; c++) {
						idx = zero + s * sliceStride + r * rowStride + c * columnStride;
						idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther + c * colStrideOther;
						a = aggr.apply(a, f.apply(new double[] { elements[idx], elements[idx + 1] }, new double[] { elemsOther[idxOther], elemsOther[idxOther + 1] }));
					}
					d = 0;
				}
			}
		}
		return a;
	}

	public DComplexMatrix3D assign(final cern.colt.function.DComplexDComplexFunction function) {
		final int zero = index(0, 0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						double[] elem = new double[2];
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									elem[0] = elements[idx];
									elem[1] = elements[idx + 1];
									elem = function.apply(elem);
									elements[idx] = elem[0];
									elements[idx + 1] = elem[1];
									idx += columnStride;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} else {
			int idx;
			double[] elem = new double[2];
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						elem[0] = elements[idx];
						elem[1] = elements[idx + 1];
						elem = function.apply(elem);
						elements[idx] = elem[0];
						elements[idx + 1] = elem[1];
						idx += columnStride;
					}
				}
			}
		}
		return this;
	}

	public DComplexMatrix3D assign(final cern.colt.function.DComplexProcedure cond, final cern.colt.function.DComplexDComplexFunction f) {
		final int zero = index(0, 0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						double[] elem = new double[2];
						int idx;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									elem[0] = elements[idx];
									elem[1] = elements[idx + 1];
									if (cond.apply(elem) == true) {
										elem = f.apply(elem);
										elements[idx] = elem[0];
										elements[idx + 1] = elem[1];
									}
									idx += columnStride;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			double[] elem = new double[2];
			int idx;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						elem[0] = elements[idx];
						elem[1] = elements[idx + 1];
						if (cond.apply(elem) == true) {
							elem = f.apply(elem);
							elements[idx] = elem[0];
							elements[idx + 1] = elem[1];
						}
						idx += columnStride;
					}
				}
			}
		}
		return this;
	}

	public DComplexMatrix3D assign(final cern.colt.function.DComplexProcedure cond, final double[] value) {
		final int zero = index(0, 0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						double[] elem = new double[2];
						int idx;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									elem[0] = elements[idx];
									elem[1] = elements[idx + 1];
									if (cond.apply(elem) == true) {
										elements[idx] = value[0];
										elements[idx + 1] = value[1];
									}
									idx += columnStride;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			double[] elem = new double[2];
			int idx;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						elem[0] = elements[idx];
						elem[1] = elements[idx + 1];
						if (cond.apply(elem) == true) {
							elements[idx] = value[0];
							elements[idx + 1] = value[1];
						}
						idx += columnStride;
					}
				}
			}
		}
		return this;
	}

	public DComplexMatrix3D assign(final cern.colt.function.DComplexRealFunction function) {
		final int zero = index(0, 0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						double[] elem = new double[2];
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									elem[0] = elements[idx];
									elem[1] = elements[idx + 1];
									elem[0] = function.apply(elem);
									elements[idx] = elem[0];
									elements[idx + 1] = 0;
									idx += columnStride;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} else {
			int idx;
			double[] elem = new double[2];
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						elem[0] = elements[idx];
						elem[1] = elements[idx + 1];
						elem[0] = function.apply(elem);
						elements[idx] = elem[0];
						elements[idx + 1] = 0;
						idx += columnStride;
					}
				}
			}
		}
		return this;
	}

	public DComplexMatrix3D assign(DComplexMatrix3D source) {
		// overriden for performance only
		if (!(source instanceof DenseDComplexMatrix3D)) {
			super.assign(source);
			return this;
		}
		final DenseDComplexMatrix3D other_final = (DenseDComplexMatrix3D) source;
		if (other_final == this)
			return this;
		checkShape(other_final);
		int np = Utils.getNP();
		if (this.isNoView && other_final.isNoView) { // quickest
			int size = elements.length;
			if ((np > 1) && (size >= Utils.getThreadsBeginN_3D())) {
				Future[] futures = new Future[np];
				int k = size / np;
				for (int j = 0; j < np; j++) {
					final int startidx = j * k;
					final int length;
					if (j == np - 1) {
						length = size - startidx;
					} else {
						length = k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {
						public void run() {
							System.arraycopy(other_final.elements, startidx, elements, startidx, length);
						}
					});
				}
				try {
					for (int j = 0; j < np; j++) {
						futures[j].get();
					}
				} catch (ExecutionException ex) {
					ex.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				System.arraycopy(other_final.elements, 0, this.elements, 0, this.elements.length);
			}
			return this;
		}
		DenseDComplexMatrix3D other = (DenseDComplexMatrix3D) source;
		if (haveSharedCells(other)) {
			DComplexMatrix3D c = other.copy();
			if (!(c instanceof DenseDComplexMatrix3D)) { // should not happen
				super.assign(source);
				return this;
			}
			other = (DenseDComplexMatrix3D) c;
		}
		final int zero = index(0, 0, 0);
		final int zeroOther = other.index(0, 0, 0);
		final int sliceStrideOther = other.sliceStride;
		final int rowStrideOther = other.rowStride;
		final int columnStrideOther = other.columnStride;
		final double[] elemsOther = other.elements;
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						int idxOther;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
								for (int c = 0; c < columns; c++) {
									elements[idx] = elemsOther[idxOther];
									elements[idx + 1] = elemsOther[idxOther + 1];
									idx += columnStride;
									idxOther += columnStrideOther;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx;
			int idxOther;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
					for (int c = 0; c < columns; c++) {
						elements[idx] = elemsOther[idxOther];
						elements[idx + 1] = elemsOther[idxOther + 1];
						idx += columnStride;
						idxOther += columnStrideOther;
					}
				}
			}
		}
		return this;
	}

	public DComplexMatrix3D assign(final DComplexMatrix3D y, final cern.colt.function.DComplexDComplexDComplexFunction function) {
		checkShape(y);
		final int zero = index(0, 0, 0);
		final int zeroOther = y.index(0, 0, 0);
		final int colStrideOther = y.columnStride;
		final int sliceStrideOther = y.sliceStride;
		final int rowStrideOther = y.rowStride;
		final double[] elemsOther = (double[]) y.getElements();
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						int idxOther;
						double[] elem = new double[2];
						double[] elemOther = new double[2];
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
								for (int c = 0; c < columns; c++) {
									elem[0] = elements[idx];
									elem[1] = elements[idx + 1];
									elemOther[0] = elemsOther[idxOther];
									elemOther[1] = elemsOther[idxOther + 1];
									elem = function.apply(elem, elemOther);
									elements[idx] = elem[0];
									elements[idx + 1] = elem[1];
									idx += columnStride;
									idxOther += colStrideOther;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx;
			int idxOther;
			double[] elem = new double[2];
			double[] elemOther = new double[2];
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
					for (int c = 0; c < columns; c++) {
						elem[0] = elements[idx];
						elem[1] = elements[idx + 1];
						elemOther[0] = elemsOther[idxOther];
						elemOther[1] = elemsOther[idxOther + 1];
						elem = function.apply(elem, elemOther);
						elements[idx] = elem[0];
						elements[idx + 1] = elem[1];
						idx += columnStride;
						idxOther += colStrideOther;
					}
				}
			}
		}

		return this;
	}

	public DComplexMatrix3D assign(final double re, final double im) {
		if (this.isNoView == false) {
			return super.assign(re, im);
		}
		final int zero = index(0, 0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (slices * rows * columns >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									elements[idx] = re;
									elements[idx + 1] = im;
									idx += columnStride;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} else {
			int idx;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						elements[idx] = re;
						elements[idx + 1] = im;
						idx += columnStride;
					}
				}
			}
		}
		return this;
	}

	public DComplexMatrix3D assign(final double[] values) {
		if (values.length != slices * rows * 2 * columns)
			throw new IllegalArgumentException("Must have same length: length=" + values.length + "slices()*rows()*2*columns()=" + slices() * rows() * 2 * columns());
		int np = Utils.getNP();
		if (this.isNoView) {
			int size = elements.length;
			if ((np > 1) && (size >= Utils.getThreadsBeginN_3D())) {
				Future[] futures = new Future[np];
				int k = size / np;
				for (int j = 0; j < np; j++) {
					final int startidx = j * k;
					final int length;
					if (j == np - 1) {
						length = size - startidx;
					} else {
						length = k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {
						public void run() {
							System.arraycopy(values, startidx, elements, startidx, length);
						}
					});
				}
				try {
					for (int j = 0; j < np; j++) {
						futures[j].get();
					}
				} catch (ExecutionException ex) {
					ex.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				System.arraycopy(values, 0, elements, 0, values.length);
			}
		} else {
			final int zero = index(0, 0, 0);
			if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
				Future[] futures = new Future[np];
				int k = slices / np;
				for (int j = 0; j < np; j++) {
					final int startslice = j * k;
					final int stopslice;
					final int glob_idx = j * k * 2 * rows * columns;
					if (j == np - 1) {
						stopslice = slices;
					} else {
						stopslice = startslice + k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {
						public void run() {
							int idxOther = glob_idx;
							int idx;
							for (int s = startslice; s < stopslice; s++) {
								for (int r = 0; r < rows; r++) {
									idx = zero + s * sliceStride + r * rowStride;
									for (int c = 0; c < columns; c++) {
										elements[idx] = values[idxOther++];
										elements[idx + 1] = values[idxOther++];
										idx += columnStride;
									}
								}
							}
						}
					});
				}
				try {
					for (int j = 0; j < np; j++) {
						futures[j].get();
					}
				} catch (ExecutionException ex) {
					ex.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				int idxOther = 0;
				int idx;
				for (int s = 0; s < slices; s++) {
					for (int r = 0; r < rows; r++) {
						idx = zero + s * sliceStride + r * rowStride;
						for (int c = 0; c < columns; c++) {
							elements[idx] = values[idxOther++];
							elements[idx + 1] = values[idxOther++];
							idx += columnStride;
						}
					}
				}
			}
		}
		return this;
	}

	public DComplexMatrix3D assign(final double[][][] values) {
		if (values.length != slices)
			throw new IllegalArgumentException("Must have same number of slices: slices=" + values.length + "slices()=" + slices());
		final int length = 2 * columns;
		int np = Utils.getNP();
		if (this.isNoView) {
			if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
				Future[] futures = new Future[np];
				int k = slices / np;
				for (int j = 0; j < np; j++) {
					final int startslice = j * k;
					final int stopslice;
					if (j == np - 1) {
						stopslice = slices;
					} else {
						stopslice = startslice + k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {
						public void run() {
							int i = startslice * sliceStride;
							for (int s = startslice; s < stopslice; s++) {
								double[][] currentSlice = values[s];
								if (currentSlice.length != rows)
									throw new IllegalArgumentException("Must have same number of rows in every slice: rows=" + currentSlice.length + "rows()=" + rows());
								for (int r = 0; r < rows; r++) {
									double[] currentRow = currentSlice[r];
									if (currentRow.length != length)
										throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "2 * columns()=" + length);
									System.arraycopy(currentRow, 0, elements, i, length);
									i += length;
								}
							}
						}
					});
				}
				try {
					for (int j = 0; j < np; j++) {
						futures[j].get();
					}
				} catch (ExecutionException ex) {
					ex.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				int i = 0;
				for (int s = 0; s < slices; s++) {
					double[][] currentSlice = values[s];
					if (currentSlice.length != rows)
						throw new IllegalArgumentException("Must have same number of rows in every slice: rows=" + currentSlice.length + "rows()=" + rows());
					for (int r = 0; r < rows; r++) {
						double[] currentRow = currentSlice[r];
						if (currentRow.length != length)
							throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "2 * columns()=" + length);
						System.arraycopy(currentRow, 0, elements, i, length);
						i += length;
					}
				}
			}
		} else {
			final int zero = index(0, 0, 0);
			if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
				Future[] futures = new Future[np];
				int k = slices / np;
				for (int j = 0; j < np; j++) {
					final int startslice = j * k;
					final int stopslice;
					if (j == np - 1) {
						stopslice = slices;
					} else {
						stopslice = startslice + k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {

						public void run() {
							int idx;
							for (int s = startslice; s < stopslice; s++) {
								double[][] currentSlice = values[s];
								if (currentSlice.length != rows)
									throw new IllegalArgumentException("Must have same number of rows in every slice: rows=" + currentSlice.length + "rows()=" + rows());
								for (int r = 0; r < rows; r++) {
									idx = zero + s * sliceStride + r * rowStride;
									double[] currentRow = currentSlice[r];
									if (currentRow.length != length)
										throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "2*columns()=" + length);
									for (int c = 0; c < columns; c++) {
										elements[idx] = currentRow[2 * c];
										elements[idx + 1] = currentRow[2 * c + 1];
										idx += columnStride;
									}
								}
							}
						}
					});
				}
				try {
					for (int j = 0; j < np; j++) {
						futures[j].get();
					}
				} catch (ExecutionException ex) {
					ex.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			} else {
				int idx;
				for (int s = 0; s < slices; s++) {
					double[][] currentSlice = values[s];
					if (currentSlice.length != rows)
						throw new IllegalArgumentException("Must have same number of rows in every slice: rows=" + currentSlice.length + "rows()=" + rows());
					for (int r = 0; r < rows; r++) {
						idx = zero + s * sliceStride + r * rowStride;
						double[] currentRow = currentSlice[r];
						if (currentRow.length != length)
							throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "2*columns()=" + length);
						for (int c = 0; c < columns; c++) {
							elements[idx] = currentRow[2 * c];
							elements[idx + 1] = currentRow[2 * c + 1];
							idx += columnStride;
						}
					}
				}
			}
		}
		return this;
	}

	public DComplexMatrix3D assignImaginary(final DoubleMatrix3D other) {
		checkShape(other);
		final int zero = index(0, 0, 0);
		final int zeroOther = other.index(0, 0, 0);
		final int sliceStrideOther = other.sliceStride;
		final int rowStrideOther = other.rowStride;
		final int colStrideOther = other.columnStride;
		final double[] elemsOther = (double[]) other.getElements();
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						int idxOther;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
								for (int c = 0; c < columns; c++) {
									elements[idx] = 0;
									elements[idx + 1] = elemsOther[idxOther];
									idx += columnStride;
									idxOther += colStrideOther;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx;
			int idxOther;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
					for (int c = 0; c < columns; c++) {
						elements[idx] = 0;
						elements[idx + 1] = elemsOther[idxOther];
						idx += columnStride;
						idxOther += colStrideOther;
					}
				}
			}
		}
		return this;
	}

	public DComplexMatrix3D assignReal(final DoubleMatrix3D other) {
		checkShape(other);
		final int zero = index(0, 0, 0);
		final int zeroOther = other.index(0, 0, 0);
		final int sliceStrideOther = other.sliceStride;
		final int rowStrideOther = other.rowStride;
		final int colStrideOther = other.columnStride;
		final double[] elemsOther = (double[]) other.getElements();
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						int idxOther;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
								for (int c = 0; c < columns; c++) {
									elements[idx] = elemsOther[idxOther];
									elements[idx + 1] = 0;
									idx += columnStride;
									idxOther += colStrideOther;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx;
			int idxOther;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
					for (int c = 0; c < columns; c++) {
						elements[idx] = elemsOther[idxOther];
						elements[idx + 1] = 0;
						idx += columnStride;
						idxOther += colStrideOther;
					}
				}
			}
		}
		return this;
	}

	public int cardinality() {
		int cardinality = 0;
		final int zero = index(0, 0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			Integer[] results = new Integer[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Integer>() {
					public Integer call() throws Exception {
						int cardinality = 0;
						int idx;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									if ((elements[idx] != 0.0) || (elements[idx + 1] != 0.0)) {
										cardinality++;
									}
									idx += columnStride;
								}
							}
						}
						return Integer.valueOf(cardinality);
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Integer) futures[j].get();
				}
				cardinality = results[0];
				for (int j = 1; j < np; j++) {
					cardinality += results[j];
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						if ((elements[idx] != 0.0) || (elements[idx + 1] != 0.0)) {
							cardinality++;
						}
						idx += columnStride;
					}
				}
			}
		}
		return cardinality;
	}

	public void fft2Slices() {
		DComplexMatrix2D slice;
		for (int s = 0; s < slices; s++) {
			slice = viewSlice(s).copy();
			slice.fft2();
			viewSlice(s).assign(slice);
		}
	}

	public void fft3() {
		if (fft3 == null) {
			fft3 = new DoubleFFT_3D(slices, rows, columns);
		}
		fft3.complexForward(elements);
	}

	public double[] getElements() {
		return elements;
	}

	public DoubleMatrix3D getImaginaryPart() {
		final DenseDoubleMatrix3D Im = new DenseDoubleMatrix3D(slices, rows, columns);
		final double[] elemsOther = (double[]) Im.getElements();
		final int sliceStrideOther = Im.sliceStride;
		final int rowStrideOther = Im.rowStride;
		final int columnStrideOther = Im.columnStride;
		final int zeroOther = Im.index(0, 0, 0);
		final int zero = index(0, 0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						int idxOther;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
								for (int c = 0; c < columns; c++) {
									elemsOther[idxOther] = elements[idx + 1];
									idx += columnStride;
									idxOther += columnStrideOther;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx;
			int idxOther;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
					for (int c = 0; c < columns; c++) {
						elemsOther[idxOther] = elements[idx + 1];
						idx += columnStride;
						idxOther += columnStrideOther;
					}
				}
			}
		}
		return Im;
	}

	public void getNonZeros(IntArrayList sliceList, IntArrayList rowList, IntArrayList columnList, ArrayList<double[]> valueList) {
		sliceList.clear();
		rowList.clear();
		columnList.clear();
		valueList.clear();
		int zero = index(0, 0, 0);
		int idx;
		double[] elem = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				idx = zero + s * sliceStride + r * rowStride;
				for (int c = 0; c < columns; c++) {
					elem[0] = elements[idx];
					elem[1] = elements[idx + 1];
					if (elem[0] != 0 || elem[1] != 0) {
						sliceList.add(s);
						rowList.add(r);
						columnList.add(c);
						valueList.add(new double[] { elem[0], elem[1] });
					}
					idx += columnStride;
				}
			}
		}
	}

	public double[] getQuick(int slice, int row, int column) {
		int idx = sliceZero + slice * sliceStride + rowZero + row * rowStride + columnZero + column * columnStride;
		return new double[] { elements[idx], elements[idx + 1] };
	}

	public DoubleMatrix3D getRealPart() {
		final DenseDoubleMatrix3D R = new DenseDoubleMatrix3D(slices, rows, columns);
		final double[] elemsOther = (double[]) R.getElements();
		final int sliceStrideOther = R.sliceStride;
		final int rowStrideOther = R.rowStride;
		final int columnStrideOther = R.columnStride;
		final int zeroOther = R.index(0, 0, 0);
		final int zero = index(0, 0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						int idxOther;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
								for (int c = 0; c < columns; c++) {
									elemsOther[idxOther] = elements[idx];
									idx += columnStride;
									idxOther += columnStrideOther;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx;
			int idxOther;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					idxOther = zeroOther + s * sliceStrideOther + r * rowStrideOther;
					for (int c = 0; c < columns; c++) {
						elemsOther[idxOther] = elements[idx];
						idx += columnStride;
						idxOther += columnStrideOther;
					}
				}
			}
		}
		return R;
	}

	public void ifft2Slices(boolean scale) {
		DComplexMatrix2D slice;
		for (int s = 0; s < slices; s++) {
			slice = viewSlice(s).copy();
			slice.ifft2(scale);
			viewSlice(s).assign(slice);
		}
	}

	public void ifft3(boolean scale) {
		if (fft3 == null) {
			fft3 = new DoubleFFT_3D(slices, rows, columns);
		}
		fft3.complexInverse(elements, scale);
	}

	public DComplexMatrix3D like(int slices, int rows, int columns) {
		return new DenseDComplexMatrix3D(slices, rows, columns);
	}

	public void setQuick(int slice, int row, int column, double re, double im) {
		int idx = sliceZero + slice * sliceStride + rowZero + row * rowStride + columnZero + column * columnStride;
		elements[idx] = re;
		elements[idx + 1] = im;
	}

	public void setQuick(int slice, int row, int column, double[] value) {
		int idx = sliceZero + slice * sliceStride + rowZero + row * rowStride + columnZero + column * columnStride;
		elements[idx] = value[0];
		elements[idx + 1] = value[1];
	}

	public double[][][] toArray() {
		final int zero = index(0, 0, 0);
		final double[][][] values = new double[slices][rows][2 * columns];
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						for (int s = startslice; s < stopslice; s++) {
							double[][] currentSlice = values[s];
							for (int r = 0; r < rows; r++) {
								double[] currentRow = currentSlice[r];
								idx = zero + s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									currentRow[2 * c] = elements[idx];
									currentRow[2 * c + 1] = elements[idx + 1];
									idx += columnStride;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx;
			for (int s = 0; s < slices; s++) {
				double[][] currentSlice = values[s];
				for (int r = 0; r < rows; r++) {
					double[] currentRow = currentSlice[r];
					idx = zero + s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						currentRow[2 * c] = elements[idx];
						currentRow[2 * c + 1] = elements[idx + 1];
						idx += columnStride;
					}
				}
			}
		}
		return values;
	}

	public DComplexMatrix1D vectorize() {
		DComplexMatrix1D v = new DenseDComplexMatrix1D(size());
		int length = rows * columns;
		for (int s = 0; s < slices; s++) {
			DComplexMatrix2D slice = viewSlice(s);
			v.viewPart(s * length, length).assign(slice.vectorize());
		}
		return v;
	}

	public double[] zSum() {
		double[] sum = new double[2];
		final int zero = index(0, 0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<double[]>() {

					public double[] call() throws Exception {
						double[] sum = new double[2];
						int idx;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = zero + s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									sum[0] += elements[idx];
									sum[1] += elements[idx + 1];
									idx += columnStride;
								}
							}
						}
						return sum;
					}
				});
			}
			double[] tmp;
			try {
				for (int j = 0; j < np; j++) {
					tmp = (double[]) futures[j].get();
					sum[0] += tmp[0];
					sum[1] += tmp[1];
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = zero + s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						sum[0] += elements[idx];
						sum[1] += elements[idx + 1];
						idx += columnStride;
					}
				}
			}
		}
		return sum;
	}

	protected boolean haveSharedCellsRaw(DComplexMatrix3D other) {
		if (other instanceof SelectedDenseDComplexMatrix3D) {
			SelectedDenseDComplexMatrix3D otherMatrix = (SelectedDenseDComplexMatrix3D) other;
			return this.elements == otherMatrix.elements;
		} else if (other instanceof DenseDComplexMatrix3D) {
			DenseDComplexMatrix3D otherMatrix = (DenseDComplexMatrix3D) other;
			return this.elements == otherMatrix.elements;
		}
		return false;
	}

	protected int index(int slice, int row, int column) {
		return sliceZero + slice * sliceStride + rowZero + row * rowStride + columnZero + column * columnStride;
	}

	protected DComplexMatrix2D like2D(int rows, int columns, int rowZero, int columnZero, int rowStride, int columnStride) {
		return new DenseDComplexMatrix2D(rows, columns, this.elements, rowZero, columnZero, rowStride, columnStride);
	}

	protected DComplexMatrix3D viewSelectionLike(int[] sliceOffsets, int[] rowOffsets, int[] columnOffsets) {
		return new SelectedDenseDComplexMatrix3D(this.elements, sliceOffsets, rowOffsets, columnOffsets, 0);
	}
}