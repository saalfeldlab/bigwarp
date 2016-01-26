/**
 * 
 */
package bigwarp.landmarks;

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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import bigwarp.landmarks.actions.AddPointEdit;
import bigwarp.landmarks.actions.DeleteRowEdit;
import bigwarp.landmarks.actions.LandmarkUndoManager;
import bigwarp.landmarks.actions.ModifyPointEdit;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author John Bogovic
 *
 */
public class LandmarkTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -5865789085410559166L;
	
	private final double[] PENDING_PT;

	public static final int NAMECOLUMN = 0;
	public static final int ACTIVECOLUMN = 1;

	
	private boolean DEBUG = false;
	
	protected int ndims = 3;
	
	protected int numCols = 8;
	protected int numRows = 0;
	
	protected int nextRowP = 0;
	protected int nextRowQ = 0;
	
	protected ArrayList<String> 	names;
	protected ArrayList<Boolean>	activeList;
	protected ArrayList<Double[]>	movingPts;
	protected ArrayList<Double[]>	targetPts;

	protected boolean pointUpdatePending = false; //
	protected boolean pointUpdatePendingMoving = false; //
	protected Double[] pointToOverride;	// hold a backup of a point for fallback
	
	// keeps track of whether points have been updated
	protected ArrayList<Boolean> doesPointHaveAndNeedWarp;
	protected ArrayList<Integer> indicesOfChangedPoints;
	protected boolean			 elementDeleted = false;
	protected ArrayList<Boolean> needsInverse;
	
	// the transformation 
	protected ThinPlateR2LogRSplineKernelTransform estimatedXfm;
	
	// keeps track of warped points so we don't always have to do it on the fly
	protected ArrayList<Double[]> warpedPoints;

	// keep track of the value of the last point that was edited but not-undoable
	// this lets us both render points correctly, and create desirable undo behavior
	// for point drags.
	protected double[] lastPoint; 
	
	// keep track of edits for undo's and redo's
	protected LandmarkUndoManager undoRedoManager;
	
	final static String[] columnNames3d = new String[]
			{
			"Name", "Active",
			"px","py","pz",
			"qx","qy","qz"
			};
	
	final static String[] columnNames2d = new String[]
			{
			"Name", "Active",
			"px","py",
			"qx","qy"
			};
	
	final String[] columnNames;
	
	public final Logger logger = LogManager.getLogger( LandmarkTableModel.class.getName() );

	public LandmarkTableModel( int ndims )
	{
		super();
		this.ndims = ndims;
		PENDING_PT = new double[ ndims ];
		Arrays.fill( PENDING_PT, Double.POSITIVE_INFINITY );
		lastPoint = PENDING_PT;
		
		names = new ArrayList<String>();
		activeList = new ArrayList<Boolean>();
		
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
		indicesOfChangedPoints  = new ArrayList<Integer>();
		needsInverse = new ArrayList<Boolean>();
		
		setTableListener();
		
		undoRedoManager = new LandmarkUndoManager();

		estimatedXfm = new ThinPlateR2LogRSplineKernelTransform ( ndims );
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

	public void printXfmPointPairs()
	{
		int nl = estimatedXfm.getNumLandmarks();
		int nd = estimatedXfm.getNumDims();

		for( int l = 0; l < nl; l++ )
		{
			System.out.print( l + " :  " );

			for( int d = 0; d < nd; d++ )
				System.out.print( String.format( "%04.2f", 
						estimatedXfm.getTargetLandmarks()[d][l]) + " " );

			System.out.print( "  => " );

			for( int d = 0; d < nd; d++ )
				System.out.print( String.format( "%04.2f", 
						estimatedXfm.getSourceLandmarks()[d][l] ) + " " );

			System.out.print( "\n" );
		}
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
		//TODO consider keeping track of this actively instead of recomputing
		int N = 0;
		for( Boolean b : activeList )
			if( b ) 
				N++;

		return N;
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
		activeList.set( row, isActive );

		if( activeList.get( row ) != isActive )
			fireTableCellUpdated( row, ACTIVECOLUMN );
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

	public void deleteRow( int i )
	{
		undoRedoManager.addEdit( new DeleteRowEdit( this, i ) );
		deleteRowHelper( i );
	}

	public synchronized void deleteRowHelper( int i )
	{
		if( i >= names.size() )
		{
			return;
		}
		
		names.remove( i );
		movingPts.remove( i );
		targetPts.remove( i );
		activeList.remove( i );
		
		
		if( indicesOfChangedPoints.contains( i ))
			indicesOfChangedPoints.remove( indicesOfChangedPoints.indexOf( i ) );
		
		doesPointHaveAndNeedWarp.remove( i );
		warpedPoints.remove( i );
		
		numRows--;

		pointUpdatePending = isUpdatePending();

		if( estimatedXfm != null && estimatedXfm.getNumLandmarks() >= (i+1) ){
			estimatedXfm.removePoint( i );
		}

		nextRowP = numRows;
		nextRowQ = numRows;

		fireTableRowsDeleted( i, i );
	}
	
	public boolean isUpdatePending()
	{
		for( int i = 0; i < movingPts.size(); i++ )
			for( int d = 0; d < ndims; d++ )
				if( Double.isInfinite( movingPts.get( i )[ d ] ) || 
					Double.isInfinite( targetPts.get( i )[ d ] ))
						return true;
		
		return false;
	}

	public void setNextRow( boolean isMoving, int row )
	{
		if( isMoving )
			nextRowP = row;
		else
			nextRowQ = row;
	}

	public synchronized void updateNextRows( int lastAddedIndex )
	{
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
	
	/**
	 * Returns the next row to be updated for the moving or target columns.
	 * 
	 * @param isMoving
	 * @return
	 */
	public int getNextRow( boolean isMoving )
	{
		if( isMoving )
			return nextRowP;
		else
			return nextRowQ;
	}

	public Boolean isWarped( int i )
	{
		return doesPointHaveAndNeedWarp.get( i );
	}

	public void updateWarpedPoint( int i, double[] pt )
	{
		if( pt == null )
			return;

		for ( int d = 0; d < ndims; d++ )
			warpedPoints.get( i )[ d ] = pt[ d ];

		doesPointHaveAndNeedWarp.set( i, true );
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
	
	protected void firePointUpdated( int row, boolean isMoving )
	{
		for( int d = 0; d < ndims; d++ )
			fireTableCellUpdated( row, 2 + d );
	}

	private void addEmptyRow( int index )
	{
		Double[] movingPt = new Double[ ndims ];
		Double[] targetPt = new Double[ ndims ];
		
		Arrays.fill( targetPt, new Double( Double.POSITIVE_INFINITY ) );
		Arrays.fill( movingPt, new Double( Double.POSITIVE_INFINITY ) );
		
		movingPts.add( index, movingPt );
		targetPts.add( index, targetPt );
		
		names.add( index, nextName( index ));
		activeList.add( index, false );
		warpedPoints.add( index, new Double[ ndims ] );
		doesPointHaveAndNeedWarp.add( index, false );
		
		fireTableRowsInserted( index, index );
		
		numRows++;
	}
	
	public void clearPt( int row, boolean isMoving )
	{
		pointEdit( row, PENDING_PT, false, isMoving, null, true );
	}
	
	public boolean add( double[] pt, boolean isMoving )
	{
		return pointEdit( -1, pt, true, isMoving, false, true );
	}
	
	public void setPoint( int row, boolean isMoving, double[] pt )
	{
		setPoint( row, isMoving, pt, true );
	}
	
	public void setPoint( int row, boolean isMoving, double[] pt, boolean isUndoable )
	{
		pointEdit( row, pt, false, isMoving, false, isUndoable );
	}
	
	public boolean pointEdit( int index, double[] pt, boolean forceAdd, boolean isMoving, boolean isWarped, boolean isUndoable )
	{
		//TODO point edit
		if( isWarped && estimatedXfm.isSolved() )
			return pointEdit( index, estimatedXfm.apply( pt ), forceAdd, isMoving, pt, isUndoable, false );
		else
			return pointEdit( index, pt, forceAdd, isMoving, null, isUndoable );
	}

	public synchronized boolean pointEdit( int index, double[] pt, boolean forceAdd, boolean isMoving, double[] warpedPt, boolean isUndoable )
	{
		return pointEdit( index, pt, forceAdd, isMoving, warpedPt, isUndoable, true );
	}

	/**
	 * Changes a point's position, or adds a new point.
	 * <p>
	 * 
	 * @param index The index into this table that this edit will affect ( a value of -1 will add a new point )
	 * @param pt the point position
	 * @param isMoving is this point in the moving image space
	 * @param warpedPt 
	 * @param isUndoable is this action undo-able
	 * @param forceUpdateWarpedPoints
	 * @return true if a new row was added
	 */
	public boolean pointEdit( int index, double[] pt, boolean forceAdd, boolean isMoving, double[] warpedPt, boolean isUndoable, boolean forceUpdateWarpedPts )
	{
		if( index == -1 )
		{
			if( isMoving )
				index = nextRowP;
			else
				index = nextRowQ;
		}

		if ( warpedPt != null )
			forceUpdateWarpedPts = false;

		boolean isAdd = forceAdd || (index == getRowCount());

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
		if( isMoving )
			pts = movingPts;
		else
			pts = targetPts;
		
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

		if( forceUpdateWarpedPts )
			computeWarpedPoint( index );

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
	 */
	public void updateAllWarpedPoints()
	{
		for ( int i = 0; i < numRows; i++ )
			if ( !isFixedPoint( i ) && isMovingPoint( i ) && estimatedXfm.getNumActiveLandmarks() > 3 )
				computeWarpedPoint( i );

	}

	/**
	 * Given an row in this table, updates the warped point position if
	 * necessary.
	 * <p>
	 * An action is taken if, for the input row if:
	 * <p>
	 * <ul>
	 * <li>1) There is a point in moving space, and
	 * <li>2) There is not a point in target space, and
	 * <li>3) A transformation has been estimated.
	 * </ul>
	 * <p>
	 * If these conditions are satisfied, the position of the moving point in
	 * target space by iteratively estimating the inverse of the thin plate
	 * spline transformation.
	 * 
	 * @param i
	 *            the row in the table
	 */
	public synchronized void computeWarpedPoint( int i )
	{
		// TODO Perhaps move this into its own thread. and expose the parameters for solving the inverse.
		if ( !isFixedPoint( i ) && isMovingPoint( i ) && estimatedXfm.isSolved() )
		{
			double[] tgt = toPrimitive( movingPts.get( i ) );
			double[] warpedPt = estimatedXfm.initialGuessAtInverse( tgt, 5.0 );

			// double[] resini = estimatedXfm.apply( warpedPt );
			// double err_ini = Math.sqrt( TransformInverseGradientDescent.sumSquaredErrors( tgt, resini ) );

			estimatedXfm.inverseTol( tgt, warpedPt, 0.5, 200 );
			// double[] resfin = estimatedXfm.apply( warpedPt );
			// double err = Math.sqrt( TransformInverseGradientDescent.sumSquaredErrors( tgt, resfin ) );

			// TODO should check for failure or non-convergence here
			updateWarpedPoint( i, warpedPt );
		}
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
		boolean changed = activate != activeList.get( index );

		if ( changed )
		{
			activeList.set( index, activate );
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
	 * @param index
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
	
	public void transferUpdatesToModel()
	{
		if (estimatedXfm == null)
			return;

		initTransformation();

		resetUpdated(); // not strictly necessary

	}

	public void transferUpdatesToModelCareful()
	{
		// TODO transferUpdatesToModel

		if (estimatedXfm == null)
			return;

		double[] source = new double[ ndims ];
		double[] target = new double[ ndims ];
		for( Integer i : indicesOfChangedPoints )
		{
			// 'source' and 'target' are swapped to get inverse transform
			for( int d = 0; d < ndims; d++ )
			{
				target[ d ] = movingPts.get( i )[ d ];
				source[ d ] = targetPts.get( i )[ d ];
			}
				
			if( i < estimatedXfm.getNumLandmarks() )
			{
				estimatedXfm.updateSourceLandmark( i, source );
				estimatedXfm.updateTargetLandmark( i, target );
			}
			else
			{
				estimatedXfm.addMatch( source, target ); 
			}
			
			if( activeList.get( i ))
				estimatedXfm.enableLandmarkPair( i );
			else
				estimatedXfm.disableLandmarkPair( i );
		}
		
		resetUpdated();
	}
	
	public void load( File f ) throws IOException
	{
		load( f, false );
	}
	
	/**
	 * Loads this table from a file
	 * @param f
	 * @throws IOException 
	 */
	public void load( File f, boolean invert ) throws IOException
	{
		CSVReader reader = new CSVReader( new FileReader( f.getAbsolutePath() ));
		List<String[]> rows = reader.readAll();
		reader.close();
		
		names.clear();
		activeList.clear();
		movingPts.clear();
		targetPts.clear();
		
		doesPointHaveAndNeedWarp.clear();
		warpedPoints.clear();
		
		int ndims = 3;
		int expectedRowLength = 8;
		
		numRows = 0;
		for( String[] row : rows )
		{
			// detect a file with 2d landmarks
			if( numRows == 0 && row.length == 6 )
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
			numRows++;
			fireTableRowsInserted( numRows, numRows );
		}

		updateNextRows( 0 );
		initTransformation();
	}
	
//	public void applyResoultionMoving( double[] res )
//	{
//		applyResolution( res, true );
//	}
//	
//	public void applyResoultionTarget( double[] res )
//	{
//		applyResolution( res, false );
//	}
	
//	private void applyResolution( double[] res, boolean isMoving )
//	{
//		int offset = 0;
//		if( !isMoving )
//			offset = ndims;
//		
//		int N = names.size();
//		for( int i = 0; i < N; i++ )
//		{
//			for( int j = 0; j < ndims; j++ )
//			{
//				Double v = pts.get( i )[ offset + j ]; 
//				pts.get( i )[ offset + j ] = v * res[ j ];
//			}
//		}
//	}
	
	public void initTransformation()
	{
		//TODO: better to pass a factory here so the transformation can be any
		// CoordinateTransform ( not just a TPS )
		if( estimatedXfm == null )
		{
			estimatedXfm = new ThinPlateR2LogRSplineKernelTransform ( ndims );
		}
		
		double[][] mvgPts = new double[ ndims ][ this.numRows ];
		double[][] tgtPts = new double[ ndims ][ this.numRows ];
		
		for( int i = 0; i < this.numRows; i++ ) 
		{
			if( activeList.get( i ))
			{
				for( int d = 0; d < ndims; d++ )
				{
					mvgPts[ d ][ i ] = movingPts.get( i )[ d ];
					tgtPts[ d ][ i ] = targetPts.get( i )[ d ];
				}
			}
		}

		// need to find the "inverse TPS" so exchange moving and tgt
		estimatedXfm.setLandmarks( tgtPts, mvgPts );

		for( int i = 0; i < this.numRows; i++ ) 
			if( !activeList.get( i ))
				estimatedXfm.disableLandmarkPair( i );
	}
	
	
	/**
	 * Saves the table to a file
	 * @param f the file
	 * @return true if successful 
	 * @throws IOException 
	 */
	public void save( File f ) throws IOException
	{
		CSVWriter csvWriter = new CSVWriter(new FileWriter( f.getAbsoluteFile() ),','); 
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
        	activeList.set(row, (Boolean)value );
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
			return new Double( movingPts.get( rowIndex )[ columnIndex - 2 ] );
		else 
			return new Double( targetPts.get( rowIndex )[ columnIndex - ndims - 2 ] );
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
	 * @return
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

			inv.add( tmp, false );

			for ( int d = 0; d < ndims; d++ )
				tmp[ d ] = thisTarget[ d ];

			inv.add( tmp, true );
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
}
