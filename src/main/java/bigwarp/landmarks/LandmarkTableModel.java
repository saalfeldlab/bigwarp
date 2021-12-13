/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
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
/**
 * 
 */
package bigwarp.landmarks;

import bdv.viewer.TransformListener;
import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bigwarp.landmarks.actions.AddPointEdit;
import bigwarp.landmarks.actions.DeleteRowEdit;
import bigwarp.landmarks.actions.LandmarkUndoManager;
import bigwarp.landmarks.actions.ModifyPointEdit;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import bdv.gui.BigWarpMessageAnimator;

/**
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 *
 */
public class LandmarkTableModel extends AbstractTableModel implements TransformListener< InvertibleRealTransform >{

	private static final long serialVersionUID = -5865789085410559166L;
	
	private final double[] PENDING_PT;

	public static final int NAMECOLUMN = 0;
	public static final int ACTIVECOLUMN = 1;

	public static Color WARNINGBGCOLOR = new Color( 255, 204, 0 );
	public static Color DEFAULTBGCOLOR = new Color( 255, 255, 255 );
	
	private boolean DEBUG = false;
	
	protected int ndims = 3;
	
	protected int numCols = 8;
	protected int numRows = 0;
	
	protected int nextRowP = 0;
	protected int nextRowQ = 0;

	protected int numActive = 0;

	protected ArrayList<String> 	names;
	protected ArrayList<Boolean>	activeList;
	protected ArrayList<Double[]>	movingPts;
	protected ArrayList<Double[]>	targetPts;

	// this list contains as many elemnts as the table, and
	// contains a unique integer >= 0 if the row is active, or -1 otherwise 
	protected ArrayList<Integer> tableIndexToActiveIndex;

	protected boolean pointUpdatePending = false; //
	protected boolean pointUpdatePendingMoving = false; //
	protected Double[] pointToOverride;	// hold a backup of a point for fallback
	
	// keeps track of whether points have been updated
	protected ArrayList<Boolean> doesPointHaveAndNeedWarp;
	protected ArrayList<Integer> indicesOfChangedPoints;
	protected boolean			 elementDeleted = false;
	protected ArrayList<Boolean> needsInverse;
	
	// true for a row if, after the transform is updated,
	// the warped point has a higher error than the specified tolerance
	protected ArrayList<Boolean> movingDisplayPointUnreliable;

	// the transformation 
	protected ThinPlateR2LogRSplineKernelTransform estimatedXfm;
	
	// keeps track of warped points so we don't always have to do it on the fly
	protected ArrayList<Double[]> warpedPoints;


	// inverse iterations
	protected int maxInverseIterations = 500;

	// inverse threshold
	protected double inverseThreshold = 0.5;

	// keep track of the value of the last point that was edited but not-undoable
	// this lets us both render points correctly, and create desirable undo behavior
	// for point drags.
	protected double[] lastPoint; 

	protected double[] tmp;

	// keep track of edits for undo's and redo's
	protected LandmarkUndoManager undoRedoManager;
	
	protected BigWarpMessageAnimator message;

	protected boolean modifiedSinceLastSave;

	final static String[] columnNames3d = new String[]
			{
			"Name", "Active",
			"mvg-x","mvg-y","mvg-z",
			"fix-x","fix-y","fix-z"
			};
	
	final static String[] columnNames2d = new String[]
			{
			"Name", "Active",
			"mvg-x","mvg-y",
			"fix-x","fix-y"
			};
	
	final String[] columnNames;
	
	public final Logger logger = LoggerFactory.getLogger( LandmarkTableModel.class );

	public LandmarkTableModel( int ndims )
	{
		super();
		this.ndims = ndims;
		PENDING_PT = new double[ ndims ];
		Arrays.fill( PENDING_PT, Double.POSITIVE_INFINITY );
		lastPoint = PENDING_PT;
		
		names = new ArrayList<>();
		activeList = new ArrayList<>();
		tableIndexToActiveIndex = new ArrayList<>();
		
		movingPts = new ArrayList<Double[]>();
		targetPts = new ArrayList<Double[]>();

		pointToOverride = new Double[ ndims ];
		Arrays.fill( pointToOverride, Double.POSITIVE_INFINITY );
		
		if( ndims == 2 ){
			columnNames = columnNames2d;
			numCols = 6;
		}else{
			columnNames = columnNames3d;
		}
		
		warpedPoints = new ArrayList<Double[]>();
		doesPointHaveAndNeedWarp = new ArrayList<Boolean>();
		movingDisplayPointUnreliable = new ArrayList<Boolean>();
		indicesOfChangedPoints  = new ArrayList<Integer>();
		needsInverse = new ArrayList<Boolean>();
		
		setTableListener();
		
		undoRedoManager = new LandmarkUndoManager();

		estimatedXfm = new ThinPlateR2LogRSplineKernelTransform ( ndims );

		tmp = new double[ 3 ];
	}

	public void setMessage( final BigWarpMessageAnimator message )
	{
		this.message = message;
	}

	public int getNumdims()
	{
		return ndims;
	}
	
	public double[] getPendingPoint()
	{
		return PENDING_PT;
	}
	
	public ThinPlateR2LogRSplineKernelTransform getTransform()
	{
		return estimatedXfm;
	}
	
	public void printDistances()
	{
		for( int i = 0; i < numRows; i++ )
		{
			System.out.println("");
			for( int d = 0; d < ndims; d++ )
			{
				System.out.print( " " + (movingPts.get( i )[ ndims + d ].doubleValue() - estimatedXfm.getSourceLandmarks()[ d ][ i ]) );
				System.out.print( " " + (targetPts.get( i )[ ndims + d ].doubleValue() - estimatedXfm.getSourceLandmarks()[ d ][ i ]) );
			}
		}
	}
	
	public void printState()
	{
		System.out.println("nextRowP: " + nextRowP );
		System.out.println("nextRowQ: " + nextRowQ );
	}

	public String toString()
	{
		String str = "";
		for( int i = 0; i < numRows; i++ )
		{
			str += Arrays.toString( movingPts.get( i ) ) + " -> " +
					Arrays.toString( targetPts.get( i )) + "\n";
		}
		return str;
	}

	public boolean validateTransformPoints()
	{
		for( int i = 0; i < numRows; i++ )
		{
			for( int d = 0; d < ndims; d++ )
			{
				if ( targetPts.get( i )[ d ].doubleValue() != estimatedXfm.getSourceLandmarks()[ d ][ i ] )
				{
					System.out.println("Wrong for pt: " + i );
					return false;
				}
			}
		}
		
		return true;
	}
	
	public void setTableListener()
	{
		addTableModelListener( new TableModelListener()
		{
			@Override
			public void tableChanged( TableModelEvent e )
			{
				if( estimatedXfm != null && e.getColumn() == 1 && e.getType() == TableModelEvent.UPDATE ) // if its active 
				{
					int row = e.getFirstRow();
					indicesOfChangedPoints.add( row );
				}
			}
		});
	}

	public boolean isModifiedSinceSave()
	{
		return modifiedSinceLastSave;
	}

	protected void importTransformation( File ffwd, File finv ) throws IOException
	{
		byte[] data = FileUtils.readFileToByteArray( ffwd );
		estimatedXfm = (ThinPlateR2LogRSplineKernelTransform) SerializationUtils.deserialize( data );
	}
	
	protected void exportTransformation( File ffwd, File finv ) throws IOException
	{
	    byte[] data    = SerializationUtils.serialize( estimatedXfm );
	    FileUtils.writeByteArrayToFile( ffwd, data );
	}

	public boolean isPointUpdatePending()
	{
		return pointUpdatePending;
	}
	
	public boolean isPointUpdatePendingMoving()
	{
		return pointUpdatePendingMoving;
	}
	
	public void restorePendingUpdate( )
	{
		ArrayList< Double[] > pts;
		
		int i = 0;
		if( pointUpdatePendingMoving )
		{
			i = nextRowP;
			pts = movingPts;
		}
		else
		{
			i = nextRowQ;
			pts = movingPts;
		}
		
		for( int d = 0; d < ndims; d++ )
			pts.get( i )[ d ] = pointToOverride[ d ];
		
		activeList.set( i, true );
		buildTableToActiveIndex();
		pointUpdatePending = false;
		
		fireTableRowsUpdated( i, i );
	}
	
	@Override
	public int getColumnCount()
	{
		return numCols;
	}

	@Override
	public int getRowCount()
	{
		return numRows;
	}

	public int getActiveRowCount()
	{
		return numActive;

//		//TODO consider keeping track of this actively instead of recomputing
//		int N = 0;
//		for( Boolean b : activeList )
//			if( b ) 
//				N++;
//
//		return N;
	}

	@Override 
	public String getColumnName( int col ){
		return columnNames[col];
	}
	
	public ArrayList<Double[]> getPoints( boolean moving ) {
		if( moving )
			return movingPts;
		else 
			return targetPts;
	}
	
	public ArrayList<String> getNames() 
	{
		return names;
	}
	
	public void setColumnName( int row, String name )
	{
		names.set( row, name );
		fireTableCellUpdated( row, NAMECOLUMN );
	}

	public boolean getIsActive( int row )
	{
		return activeList.get( row );
	}

	public void setIsActive( int row, boolean isActive )
	{
		synchronized( this ) {
			if( isRowUnpaired( row ) && isActive )
			{
				if( message != null )
					message.showMessage( "Can't activate unpaired row ( " + names.get( row ) + " )" );
				return;
			}

			activeList.set( row, isActive );
			buildTableToActiveIndex();
		}

		if( activeList.get( row ) != isActive )
		{
			fireTableCellUpdated( row, ACTIVECOLUMN );
			modifiedSinceLastSave = true;
		}
	}

	private void buildTableToActiveIndex()
	{
		synchronized(this) {
			tableIndexToActiveIndex.clear();

			int N = 0;
			int j = 0;
			for( int i = 0; i < getRowCount(); i++ )
			{
				if( isActive( i ))
				{
	//				tableIndexToActiveIndex.set( i, j++ );
					tableIndexToActiveIndex.add( j++ );
					N++;
				}
			}
			numActive = N;
		}
	}

	public int getActiveIndex( int tableIndex )
	{
		return tableIndexToActiveIndex.get( tableIndex );
	}

	public Class<?> getColumnClass( int col ){
		if( col == NAMECOLUMN ){
			return String.class;
		}else if( col == ACTIVECOLUMN ){
			return Boolean.class;
		}else{
			return Double.class;
		}
	}
	
	public boolean isActive( int i ){
		if( i < 0 || i >= getRowCount() ){
			return false;
		}else{
			return (Boolean)getValueAt(i,1);
		}
	}

	public void clear()
	{
		for( int i = getRowCount() - 1; i >= 0; i-- )
			deleteRow( i );
	}

	public void deleteRow( int i )
	{
		if( getRowCount() > 0 && i >= 0)
		{
			undoRedoManager.addEdit( new DeleteRowEdit( this, i ) );
			deleteRowHelper( i );
		}
	}

	public void deleteRowHelper( int i )
	{
		synchronized( this ) {
			if( i >= names.size() )
			{
				return;
			}

			names.remove(i);
			movingPts.remove(i);
			targetPts.remove(i);
			activeList.remove(i);

			if (indicesOfChangedPoints.contains(i))
				indicesOfChangedPoints.remove(indicesOfChangedPoints.indexOf(i));

			doesPointHaveAndNeedWarp.remove(i);
			movingDisplayPointUnreliable.remove(i);
			warpedPoints.remove(i);

			numRows--;

			pointUpdatePending = isUpdatePending();

			nextRowP = numRows;
			nextRowQ = numRows;

			fireTableRowsDeleted(i, i);
			modifiedSinceLastSave = true;

			buildTableToActiveIndex();
		}
	}

	/**
	 * Returns true if the ith row is unpaired - i.e., 
	 * if either of the moving or target points are unset.
	 * 
	 * @param i row index
	 * @return true of the 
	 */
	public boolean isRowUnpaired( final int i )
	{
		for( int d = 0; d < ndims; d++ )
			if( Double.isInfinite( movingPts.get( i )[ d ] ) || 
				Double.isInfinite( targetPts.get( i )[ d ] ))
					return true;

		return false;
	}
	
	/**
	 * Returns true if any row is unpaired.
	 * 
	 * @return is update pending
	 */
	public boolean isUpdatePending()
	{
		for( int i = 0; i < movingPts.size(); i++ )
			if( isRowUnpaired( i ))
				return true;
		
		return false;
	}

	public void setNextRow( boolean isMoving, int row )
	{
		synchronized(this) {
			if( isMoving )
				nextRowP = row;
			else
				nextRowQ = row;
		}
	}

	public void updateNextRows( int lastAddedIndex )
	{
		synchronized(this) {
			nextRowP = numRows;
			nextRowQ = numRows;

			pointUpdatePendingMoving = false;
			for ( int i = lastAddedIndex; i < numRows; i++ )
			{
				// moving image
				if ( Double.isInfinite( movingPts.get( i )[ 0 ] ) )
				{
					pointUpdatePendingMoving = true;

					if ( i < nextRowP )
						nextRowP = i;

					// TODO is synchronization here bad
					setIsActive( i, false );
				}

				// target image
				if ( Double.isInfinite( targetPts.get( i )[ 0 ] ) )
				{
					pointUpdatePendingMoving = true;

					if ( i < nextRowQ )
						nextRowQ = i;

					setIsActive( i, false );
				}
			}

			// try wrapping
			if ( lastAddedIndex > 0 && !pointUpdatePendingMoving )
				updateNextRows( 0 );

			logger.trace(" updateNextRows  - moving: " + nextRowP + "  -  target:  " + nextRowQ );
			// nextRowP = ( nextRowP == numRows ) ? -1 : numRows;
			// nextRowQ = ( nextRowQ == numRows ) ? -1 : numRows;
		}
	}
	
	/**
	 * Returns the next row to be updated for the moving or target columns.
	 * 
	 * @param isMoving isMoving
	 * @return index of the next row
	 */
	public int getNextRow( boolean isMoving )
	{
		synchronized(this) {
			if( isMoving )
				return nextRowP;
			else
				return nextRowQ;
		}
	}

	public Boolean isWarped( int i )
	{
		return doesPointHaveAndNeedWarp.get( i );
	}

	public void updateWarpedPoint( int i, double[] pt )
	{
		synchronized(this) {
			if( pt == null )
				return;

			for ( int d = 0; d < ndims; d++ )
				warpedPoints.get( i )[ d ] = pt[ d ];

			doesPointHaveAndNeedWarp.set( i, true );
		}
	}
	
	public void printWarpedPoints()
	{
		String s = "";
		int N = doesPointHaveAndNeedWarp.size();
		for( int i = 0; i < N; i++ )
		{
			if( doesPointHaveAndNeedWarp.get( i ))
			{
//				String s = "" + i + " : ";
				s += String.format("%04d : ", i);
				for ( int d = 0; d < ndims; d++ )
					s += String.format("%f\t", warpedPoints.get( i )[ d ] );
				
				s+="\n";
			}
		}
		System.out.println( s );
	}

	public ArrayList< Double[] > getWarpedPoints()
	{
		return warpedPoints;
	}
	
	public ArrayList<Boolean> getChangedSinceWarp()
	{
		return doesPointHaveAndNeedWarp;
	}

	public void resetWarpedPoint( int i )
	{
		if ( activeList.get( i ) )
			doesPointHaveAndNeedWarp.set( i, false );
	}

	public void resetWarpedPoints()
	{
		for ( int i = 0; i < doesPointHaveAndNeedWarp.size(); i++ )
			resetWarpedPoint( i );
	}

	public void resetNeedsInverse(){
		Collections.fill( needsInverse, false );
	}
	
	public void setNeedsInverse( int i )
	{
		needsInverse.set( i, true );
	}

	public boolean rowNeedsWarning( int row )
	{
		return movingDisplayPointUnreliable.get( row );
	}
	
	protected void firePointUpdated( int row, boolean isMoving )
	{
		modifiedSinceLastSave = true;
		for( int d = 0; d < ndims; d++ )
			fireTableCellUpdated( row, 2 + d );
	}

	private void addEmptyRow( int index )
	{
		synchronized(this) {
			Double[] movingPt = new Double[ ndims ];
			Double[] targetPt = new Double[ ndims ];

			Arrays.fill( targetPt, Double.POSITIVE_INFINITY );
			Arrays.fill( movingPt, Double.POSITIVE_INFINITY );

			movingPts.add( index, movingPt );
			targetPts.add( index, targetPt );
			
			names.add( index, nextName( index ));
			activeList.add( index, false );
			warpedPoints.add( index, new Double[ ndims ] );
			doesPointHaveAndNeedWarp.add( index, false );
			movingDisplayPointUnreliable.add( index, false );
			tableIndexToActiveIndex.add( -1 );
			
			numRows++;
			modifiedSinceLastSave = true;
		}

		fireTableRowsInserted( index, index );
	}
	
	public void clearPt( int row, boolean isMoving )
	{
		pointEdit( row, PENDING_PT, false, isMoving, null, true );
	}
	
	public boolean add( double[] pt, boolean isMoving )
	{
		return pointEdit( -1, pt, true, isMoving, false, true, null );
	}

	public boolean add( double[] pt, boolean isMoving, final RealTransform xfm )
	{
		return pointEdit( -1, pt, true, isMoving, false, true, xfm );
	}
	
	public void setPoint( int row, boolean isMoving, double[] pt, final RealTransform xfm )
	{
		setPoint( row, isMoving, pt, true, xfm );
	}
	
	public void setPoint( int row, boolean isMoving, double[] pt, boolean isUndoable, final RealTransform xfm )
	{
		pointEdit( row, pt, false, isMoving, false, isUndoable, xfm );
	}

	public boolean pointEdit( final int index, final double[] pt, final boolean forceAdd, final boolean isMoving, final boolean isWarped, final boolean isUndoable, final RealTransform xfm )
	{
		//TODO point edit
		if( isWarped )
		{
			xfm.apply( pt, tmp );
			return pointEdit( index, tmp, forceAdd, isMoving, pt, isUndoable );
		}
		else
			return pointEdit( index, pt, forceAdd, isMoving, null, isUndoable );
	}

	/**
	 * Changes a point's position, or adds a new point.
	 * <p>
	 * 
	 * @param index The index into this table that this edit will affect ( a value of -1 will add a new point )
	 * @param pt the point position
	 * @param forceAdd force addition of a new point at the specified index
	 * @param isMoving is this point in the moving image space
	 * @param warpedPt position of the warped point
	 * @param isUndoable is this action undo-able
	 * @return true if a new row was added
	 */
	public boolean pointEdit( int index, double[] pt, boolean forceAdd, boolean isMoving, double[] warpedPt, boolean isUndoable )
	{
		boolean isAdd;
		synchronized ( this ) {

			// this means we should add a new point.  
			// index of this point should be the next free row in the table
			if( index == -1 )
			{
				if( isMoving )
					index = nextRowP;
				else
					index = nextRowQ;
			}

			isAdd = forceAdd || (index == getRowCount());

			if( isAdd )
				addEmptyRow( index );

			double[] oldpt = copy( PENDING_PT );
			if( !isAdd )
			{
				if ( lastPoint != PENDING_PT )
				{
					oldpt = copy( lastPoint );
				}
				else
				{
					if ( isMoving )
						oldpt = toPrimitive( movingPts.get( index ) );
					else
						oldpt = toPrimitive( targetPts.get( index ) );
				}
			}
			
			ArrayList< Double[] > pts;

			/********************
			 * Update the point *
			 ********************/
			// get the relevant list of points (moving or target)
			if( isMoving )
				pts = movingPts;
			else
				pts = targetPts;

			// create a new point and add it
			Double[] exPts = new Double[ ndims ];
			for( int i = 0; i < ndims; i++ ){
				exPts[ i ] = pt[ i ];
			}
			pts.set( index, exPts );

			/************************************************
			 * Determine if we have to update warped points *
			 ************************************************/
			if( isMoving && warpedPt != null )
				updateWarpedPoint( index, warpedPt );
			else
				resetWarpedPoint( index );

			// make sure we can undo this action if relevant
			// some point-drag behaviors are not and should not be undo-able
			if ( isUndoable )
			{
				if ( isAdd )
				{
					undoRedoManager.addEdit( new AddPointEdit( this, index, pt, isMoving ) );
				}
				else
				{
					undoRedoManager.addEdit( new ModifyPointEdit(
							this, index,
							oldpt, pt,
							isMoving ) );
				}
				lastPoint = PENDING_PT;
			}
			else if( lastPoint == PENDING_PT )
			{
				// Remember the "oldpt" even when an edit is not-undoable
				// but don't overwrite an existing value
				lastPoint = oldpt;
			}

			/***********************
			 * Update next indices *
			 ***********************/
			updateNextRows( index );

			activateRow( index );
		}

		firePointUpdated( index, isMoving );

		return isAdd;
	}

	public void setLastPoint( int i, boolean isMoving )
	{
		if( isMoving )
			lastPoint = toPrimitive( movingPts.get( i ) );
		else
			lastPoint = toPrimitive( targetPts.get( i ) );
	}

	public void resetLastPoint()
	{
		lastPoint = PENDING_PT;
	}

	/**
	 * Looks through the table for points where there is a point in moving space but not fixed space.
	 * For any such landmarks that are found, compute the inverse transform and add the result to the fixed points line.
	 * 
	 *  @param xfm the new transformation
	 */
	public void updateAllWarpedPoints( final InvertibleRealTransform xfm )
	{
		final InvertibleRealTransform xfmToUse;
		if (xfm instanceof Wrapped2DTransformAs3D && ndims == 2)
			xfmToUse = ((Wrapped2DTransformAs3D) xfm).transform;
		else
			xfmToUse = xfm;

		for ( int i = 0; i < numRows; i++ )
			if ( !isFixedPoint( i ) && isMovingPoint( i ) )
				computeWarpedPoint( i, xfmToUse );
	}

	/**
	 * Given an row in this table, updates the warped point position if
	 * necessary.
	 * <p>
	 * An action is taken if, for the input row if:
	 * <ul><li>1) There is a point in moving space, and
	 * <li>2) There is not a point in target space, and
	 * <li>3) A transformation has been estimated.
	 * </ul>
	 * <p>
	 * If these conditions are satisfied, the position of the moving point in
	 * target space by iteratively estimating the inverse of the thin plate
	 * spline transformation.
	 * 
	 * @param i the row in the table
	 * @param xfm the invertible transformation
	 */
	public void computeWarpedPoint( int i, final InvertibleRealTransform xfm )
	{
		synchronized( this ) {
			// TODO pass a transform here as argument - don't use estimatedXfm stored here

			// TODO Perhaps move this into its own thread. and expose the parameters for solving the inverse.
			if ( !isFixedPoint( i ) && isMovingPoint( i ) && xfm != null )
			{
				double[] tgt = toPrimitive( movingPts.get( i ) );

				double[] warpedPt = new double[ ndims ];
				xfm.applyInverse( warpedPt, tgt );

				if( xfm instanceof WrappedIterativeInvertibleRealTransform )
				{
					WrappedIterativeInvertibleRealTransform<?> inv = (WrappedIterativeInvertibleRealTransform<?>)xfm;
					double error = inv.getOptimzer().getError();

					if( error > inverseThreshold )
					{
						movingDisplayPointUnreliable.set( i, true );
						message.showMessage( String.format(
							"Warning: location of moving point %s in warped space is innacurate", names.get( i )));
					}
					else
						movingDisplayPointUnreliable.set( i, false );
				}

				// TODO should check for failure or non-convergence here
				// can use the error returned by the inverse method to do this. 
				// BUT - it's not clear what to do upon failure 
				updateWarpedPoint( i, warpedPt );
			}
		}
	}

	public void setMaxInverseIterations( final int maxIters )
	{
		maxInverseIterations = maxIters;
	}

	public void setInverseThreshold( final double inverseThreshold )
	{
		this.inverseThreshold = inverseThreshold;
	}
	public int getIndexNearestTo( double[] pt, boolean isMoving )
	{
		Double[] p;
		double minDist = Double.MAX_VALUE;
		int minIndex = -1;
		for( int i = 0; i < numRows; i++ )
		{
			p = getPoint( isMoving, i );
			double thisdist = squaredDistance( p, pt );
			if( thisdist < minDist )
			{
				minDist = thisdist;
				minIndex = i;
			}
		}
		return minIndex;
	}

	public double squaredDistance( Double[] p, double[] q )
	{
		double dist = 0;
		for( int j = 0; j < p.length; j++ )
		{
			dist += ( p[j] - q[j] ) * ( p[j] - q[j] );
		}
		return dist;
	}

	public int getIndexNearestTo( RealLocalizable pt, boolean isMoving )
	{
		Double[] p;
		double minDist = Double.MAX_VALUE;
		int minIndex = -1;
		for( int i = 0; i < numRows; i++ )
		{
			p = getPoint( isMoving, i );
			double thisdist = squaredDistance( p, pt );
			if( thisdist < minDist )
			{
				minDist = thisdist;
				minIndex = i;
			}
		}
		return minIndex;
	}

	public double squaredDistance( Double[] p, RealLocalizable q )
	{
		double dist = 0;
		for( int j = 0; j < p.length; j++ )
		{
			dist += ( p[j].doubleValue() - q.getDoublePosition( j ) ) * ( p[j].doubleValue() - q.getDoublePosition( j ) );
		}
		return dist;
	}

	public Double[] getPoint( boolean isMoving, int index )
	{
		if ( isMoving )
			return movingPts.get( index );
		else
			return targetPts.get( index );
	}

	public Double[] getMovingPoint( int index )
	{
		return movingPts.get( index );
	}

	public Double[] getFixedPoint( int index )
	{
		return targetPts.get( index );
	}

	public boolean isMovingPoint( int index )
	{
		return !Double.isInfinite( movingPts.get( index )[ 0 ] );
	}

	public boolean isFixedPoint( int index )
	{
		return !Double.isInfinite( targetPts.get( index )[ 0 ] );
	}

	public boolean isFixedPoint( int index, boolean isMoving )
	{
		if ( isMoving )
			return isMovingPoint( index );
		else
			return isFixedPoint( index );
	}

	public void activateRow( int index )
	{
		boolean changed = false;
		synchronized( this ) {
			boolean activate = true;

			for( int d = 0; d < ndims; d++ )
			{
				if( !isMovingPoint( index ) )
				{
					activate = false;
					break;
				}
				if( !isFixedPoint( index ) )
				{
					activate = false;
					break;
				}	
			}
			changed = activate != activeList.get( index );

			if ( changed )
			{
				activeList.set( index, activate );
				buildTableToActiveIndex();
			}
		}

		if ( changed )
		{
			fireTableCellUpdated( index, ACTIVECOLUMN );
		}
	}

	/**
	 * Returns a name for a row to be inserted at the given index.
	 * 
	 * @param index index of the row
	 * @return a name for the new row
	 */
	private String nextName( int index )
	{
		// TODO the current implmentation avoids overlap in common use cases.
		// Consider whether checking all names for overlaps is worth it.
		final String s;
		if( index == 0 )
			s = "Pt-0";
		else
		{
			int i = index;
			try
			{
				// Increment the index in the name of the previous row
				i = 1 + Integer.parseInt( names.get( index - 1 ).replaceAll( "Pt-", "" ));
			}
			catch ( Exception e ){}

			s = String.format( "Pt-%d", i );
		}
		return s;
	}
	
	/**
	 * Sets a flag that indicates the point at the input index has changed
	 * since the last time a transform was estimated.
	 * 
	 * Not currently in use, but may be in the future 
	 * @param index the row index
	 */
	@SuppressWarnings("unused")
	private void markAsChanged( int index )
	{
		for( int i = 0; i < this.numRows; i++ )
		{
			if( !indicesOfChangedPoints.contains( i ))
				indicesOfChangedPoints.add( i ); 
		}
	}


	public void resetUpdated()
	{
		indicesOfChangedPoints.clear();
		elementDeleted = false;
	}
	
	@Deprecated
	public void transferUpdatesToModel()
	{
		if (estimatedXfm == null)
			return;

		initTransformation();

		resetUpdated(); // not strictly necessary

	}
	
	public void load( File f ) throws IOException
	{
		load( f, false );
	}
	
	/**
	 * Loads this table from a file
	 * @param f the file
	 * @param invert invert the moving and target point sets
	 * @throws IOException an exception
	 */
	public void load( File f, boolean invert ) throws IOException
	{
		synchronized(this) {
			clear();

			CSVReader reader = new CSVReader( new FileReader( f.getAbsolutePath() ));
			List< String[] > rows = null;
			try
			{
				rows = reader.readAll();
				reader.close();
			}
			catch( CsvException e ){}
			if( rows == null || rows.size() < 1 )
			{
				System.err.println("Error reading csv");
				return;
			}

			int ndims = 3;
			int expectedRowLength = 8;

			int i = 0;
			for( String[] row : rows )
			{
				// detect a file with 2d landmarks
				if( i == 0 && // only check for the first row
						row.length == 6 )
				{
					ndims = 2;
					expectedRowLength = 6;
				}

				if( row.length != expectedRowLength  )
					throw new IOException( "Invalid file - not enough columns" );

				names.add( row[ 0 ] );
				activeList.add( Boolean.parseBoolean( row[ 1 ]) );

				Double[] movingPt = new Double[ ndims ];
				Double[] targetPt = new Double[ ndims ];

				int k = 2;
				for( int d = 0; d < ndims; d++ )
					movingPt[ d ] = Double.parseDouble( row[ k++ ]);

				for( int d = 0; d < ndims; d++ )
					targetPt[ d ] = Double.parseDouble( row[ k++ ]);

				if( invert )
				{
					movingPts.add( targetPt );
					targetPts.add( movingPt );
				}
				else
				{
					movingPts.add( movingPt );
					targetPts.add( targetPt );
				}

				warpedPoints.add( new Double[ ndims ] );
				doesPointHaveAndNeedWarp.add( false );
				movingDisplayPointUnreliable.add( false );
				i++;
			}

			this.ndims = ndims;
			numRows = i;
			updateNextRows( 0 );
			buildTableToActiveIndex();
	//		initTransformation();
		}

		for( int i = 0; i < numRows; i++ )
			fireTableRowsInserted( i, i );

	}

	public int numActive()
	{
		return numActive;
//		int numActive = 0;
//		for ( int i = 0; i < this.numRows; i++ )
//		{
//			if ( activeList.get( i ) )
//				numActive++;
//		}
//		return numActive;
	}

	public void copyLandmarks( int tableIndex, double[][] movingLandmarks, double[][] targetLandmarks )
	{
		synchronized( this ) {
			if( numActive != movingLandmarks[0].length )
			{
				System.out.println( "copy landmarks INCONSISTENCY");
			}

			if ( activeList.get( tableIndex ) )
			{
				int activeIndex = getActiveIndex( tableIndex );
				for ( int d = 0; d < ndims; d++ )
				{
					movingLandmarks[ d ][ activeIndex ] = movingPts.get( tableIndex )[ d ];
					targetLandmarks[ d ][ activeIndex ] = targetPts.get( tableIndex )[ d ];
				}
			}
		}
	}

	public void copyLandmarks( double[][] movingLandmarks, double[][] targetLandmarks )
	{
		synchronized(this) {
			logger.trace(
					String.format("copyLandmarks. nActive=%d.  sizes = %d x %d ; %d x %d ", numActive,
							movingLandmarks.length, movingLandmarks[0].length,
							targetLandmarks.length, targetLandmarks[0].length));
			int k = 0;
			for ( int i = 0; i < this.numRows; i++ )
			{
				if ( activeList.get( i ) )
				{
					for ( int d = 0; d < ndims; d++ )
					{
						movingLandmarks[ d ][ k ] = movingPts.get( i )[ d ];
						targetLandmarks[ d ][ k ] = targetPts.get( i )[ d ];
					}
					k++;
				}
			}
		}
	}

	@Deprecated
	public void initTransformation()
	{
		int numActive = numActive();
		// TODO: better to pass a factory here so the transformation can be any
		// CoordinateTransform ( not just a TPS )
		double[][] mvgPts = new double[ ndims ][ numActive ];
		double[][] tgtPts = new double[ ndims ][ numActive ];

		copyLandmarks( mvgPts, tgtPts );

		// need to find the "inverse TPS" so exchange moving and tgt
		estimatedXfm = new ThinPlateR2LogRSplineKernelTransform( ndims, tgtPts, mvgPts );
	}
	
	/**
	 * Saves the table to a file
	 * @param f the file
	 * @throws IOException an exception
	 */
	public void save( File f ) throws IOException
	{
		CSVWriter csvWriter = new CSVWriter(new FileWriter( f.getAbsoluteFile() )); 

		synchronized(this) { 
			int N = names.size();
			List<String[]> rows = new ArrayList<String[]>( N );
			
			int rowLength = 2 * ndims + 2;
			for( int i = 0; i < N; i++ )
			{
				String[] row = new String[ rowLength ];
				row[ 0 ] = names.get( i );
				row[ 1 ] = activeList.get( i ).toString();

				int k = 2;
				int j = 0;
				while( j < ndims )
					row[ k++ ] = movingPts.get( i )[ j++ ].toString();

				j = 0;
				while( j < ndims )
					row[ k++ ] = targetPts.get( i )[ j++ ].toString();

				rows.add( row );
			}
			csvWriter.writeAll( rows );
			csvWriter.close();
			modifiedSinceLastSave = false;
		}
	}
	
	public static String print( Double[] d )
	{
		String out = "";
		for( int i=0; i<d.length; i++)
			out += d[i] + " ";
		
		return out;
	}
	
	public void setValueAt(Object value, int row, int col)
	{
		synchronized( this ) { 

			if( row < 0 || col < 0
				|| row >= numRows || col >= numCols )
			{
				System.out.println( "Warning: row (" + row + ") or column (" + col  + ") out of range." );
				return;
			}

			if (DEBUG)
			{
				System.out.println("Setting value at " + row + "," + col
								   + " to " + value
								   + " (an instance of "
								   + value.getClass() + ")");
			}

			if( col == NAMECOLUMN )
			{
				names.set(row, (String)value );
			}
			else if( col == ACTIVECOLUMN )
			{
				setIsActive( row, ( Boolean ) value );
			}
			else if( col < 2 + ndims )
			{
				Double[] thesePts = movingPts.get(row);
				thesePts[ col - 2 ] = ((Double)value).doubleValue();
			}
			else
			{
				Double[] thesePts = targetPts.get(row);
				thesePts[ col - ndims - 2 ] = ((Double)value).doubleValue();
			}

		}

        fireTableCellUpdated(row, col);
    }
	
	@Override
	public Object getValueAt( int rowIndex, int columnIndex )
	{
		if( rowIndex >= names.size() )
			return null;
		else if ( columnIndex == NAMECOLUMN )
			return names.get( rowIndex );
		else if ( columnIndex == ACTIVECOLUMN )
			return activeList.get( rowIndex );
		else if( columnIndex < 2 + ndims )
			return movingPts.get( rowIndex )[ columnIndex - 2 ];
		else
			return targetPts.get( rowIndex )[ columnIndex - ndims - 2 ];
	}

	/**
	 * Only allow editing of the first ("Name") and 
	 * second ("Active") column
	 */
	public boolean isCellEditable( int row, int col )
	{
		return ( col <= ACTIVECOLUMN );
	}

	/**
	 * Returns a new LandmarkTableModel but makes the moving points fixed and vice versa.
	 *
	 * @return the LandmarkTableModel
	 */
	public LandmarkTableModel invert()
	{
		LandmarkTableModel inv = new LandmarkTableModel( ndims );

		int N = this.getRowCount();

		double[] tmp = new double[ ndims ];
		for ( int i = 0; i < N; i++ )
		{
			Double[] thisMoving = movingPts.get( i );
			Double[] thisTarget = targetPts.get( i );

			for ( int d = 0; d < ndims; d++ )
				tmp[ d ] = thisMoving[ d ];

			inv.add( tmp, false, null );

			for ( int d = 0; d < ndims; d++ )
				tmp[ d ] = thisTarget[ d ];

			inv.setPoint( i, true, tmp, null );
		}

		return inv;
	}

	public LandmarkUndoManager getUndoManager()
	{
		return undoRedoManager;
	}

	public static double[] copy( double[] in )
	{
		double[] out = new double[ in.length ];
		for ( int i = 0; i < in.length; i++ )
			out[ i ] = in [ i ];
		
		return out;
	}
	
	public static double[] toPrimitive( Double[] in )
	{
		double[] out = new double[ in.length ];
		for ( int i = 0; i < in.length; i++ )
			out[ i ] = in[ i ];

		return out;
	}

	@Override
	public void transformChanged( final InvertibleRealTransform transform )
	{
		// update warped point locations when the transform changes
		updateAllWarpedPoints( transform );
	}
}
