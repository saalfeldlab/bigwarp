/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2022 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package bigwarp.landmarks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import bdv.ij.ApplyBigwarpPlugin;
import bdv.viewer.Source;
import bigwarp.BigWarp;
import ij.gui.GenericDialog;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.iterator.RealIntervalIterator;
import net.imglib2.realtransform.BoundingBoxEstimation;

public class LandmarkGridGenerator
{

	public static int NUM_LANDMARKS_FOR_WARNING = 1200;

	private static final double SPACING_MULTIPLIER = 0.9999999;

	protected final double[] spacing;

	protected final RealInterval interval;

	private TableModelListener[] listeners;

	public LandmarkGridGenerator( final RealInterval interval, final double[] spacing )
	{
		this.interval = interval;
		this.spacing = spacing;
	}

	public LandmarkGridGenerator( final RealInterval interval, final long[] number )
	{
		this.interval = interval;
		spacing = new double[ interval.numDimensions() ];
		for( int i = 0; i < interval.numDimensions(); i++  )
		{
			spacing[ i ] =  SPACING_MULTIPLIER * ( interval.realMax( i ) - interval.realMin( i ) ) / ( number[ i ] - 1 ); 
		}
	}

	public double approxNumberOfPoints()
	{
		double total = 1;
		for( int i = 0; i < interval.numDimensions(); i++  )
		{
			total *= ( ( interval.realMax( i ) - interval.realMin( i ) ) / spacing [ i ] ) + 1 ;
		}
		return total;
	}

	/*
	 * Returns number of points added
	 */
	public int fill( LandmarkTableModel ltm )
	{
		pauseListeners(ltm);

		int i = 0;
		double[] p = new double[ interval.numDimensions() ];
		RealIntervalIterator it = new RealIntervalIterator( interval, spacing );

		while( it.hasNext() )
		{
			it.fwd();
			it.localize( p );

			// ltm makes a copy, so can re-use p
			ltm.add( p, true );
			ltm.setPoint( ltm.getNextRow( false ), false, p, null );
			i++;
		}

		resumeAndTriggerListeners(ltm);
		return i;
	}
	
	public static boolean fillFromDialog( final BigWarp<?> bw )
	{
		LandmarkTableModel ltm = bw.getLandmarkPanel().getTableModel();
		int nd = ltm.ndims;
		final GenericDialog gd = new GenericDialog( "Generate landmark grid." );

		gd.addMessage( "Field of view and resolution:" );

		gd.addMessage( "Number of points per dimension (default)");
		gd.addNumericField( "nx", 5.0, 2 );
		gd.addNumericField( "ny", 5.0, 2 );

		if( nd > 2 )
			gd.addNumericField( "nz", 5.0, 2 );

		gd.addMessage( "Spacing");
		gd.addNumericField( "sx", -1.0, 4 );
		gd.addNumericField( "sy", -1.0, 4 );
		
		if( nd > 2 )
			gd.addNumericField( "sz", -1.0, 4 );
		
		gd.showDialog();

		if ( gd.wasCanceled() )
			return false;
	

		double nx = gd.getNextNumber();
		double ny = gd.getNextNumber();
		double nz = -1;
		if( nd > 2 )
			nz = gd.getNextNumber();

		double sx = gd.getNextNumber();
		double sy = gd.getNextNumber();
		double sz = -1;
		if( nd > 2 )
			sz = gd.getNextNumber();


		Source<?> tgtSrc = null;
		if (bw.getData().numTargetSources() == 0)
			throw new RuntimeException("Requested target field of view, but no target source exists.");
		else
			tgtSrc = bw.getData().getTargetSource(0).getSpimSource();

		double[] res = ApplyBigwarpPlugin.getResolution( bw.getData(), ApplyBigwarpPlugin.TARGET, null );
		List<RealInterval> outputIntervals = ApplyBigwarpPlugin.getPhysicalInterval(bw.getData(), ltm, null,
				ApplyBigwarpPlugin.TARGET, null, null, null, null, res);

		final double[] offset = ApplyBigwarpPlugin.getPhysicalOffset( ApplyBigwarpPlugin.TARGET, null, ltm, null,
				"", new BoundingBoxEstimation(), res, null, tgtSrc );

		final RealInterval interval = offsetAndSqueezeInterval( outputIntervals.get(0), offset, nd );

		LandmarkGridGenerator gen;
		if (sx > 0 || sy > 0 || sz > 0)
			gen = new LandmarkGridGenerator(interval, new double[]{sx, sy, sz});
		else
			gen = new LandmarkGridGenerator(interval, new long[]{(long)nx, (long)ny, (long)nz});

		final double N = gen.approxNumberOfPoints();
		if( N > NUM_LANDMARKS_FOR_WARNING )
		{
			final GenericDialog warningDialog = new GenericDialog( "Warning" );
			warningDialog.addMessage( "You are about to add approximately\n" + Math.round( N ) + "\npoints." );
			warningDialog.addMessage( "This could cause Bigwarp to be slow or crash." );
			
			warningDialog.addMessage("Proceed?" );
			warningDialog.showDialog();

			if ( warningDialog.wasCanceled() )
				return false;

		}

		gen.fill( ltm );

		return true;
	}

	private void pauseListeners( LandmarkTableModel ltm ) {

		listeners = ltm.getTableModelListeners();
		for( TableModelListener l : listeners )
			ltm.removeTableModelListener(l);
	}

	private void resumeAndTriggerListeners( LandmarkTableModel ltm ) {

		final TableModelEvent e = new TableModelEvent(ltm);
		for (TableModelListener l : listeners) {
			ltm.addTableModelListener(l);
			l.tableChanged(e);
		}
	}

	private static RealInterval offsetAndSqueezeInterval( RealInterval interval, double[] offset, int nd ) {

		if( interval.numDimensions() <= nd ) 
			return interval;

		double[] min = new double[nd];
		double[] max = new double[nd];
		for( int i = 0; i < nd; i++ )
		{
			min[i] = interval.realMin(i) + offset[i];
			max[i] = interval.realMax(i) + offset[i];
		}

		return new FinalRealInterval(min, max);
	}

}
