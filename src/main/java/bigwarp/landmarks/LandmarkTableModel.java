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
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import jitk.spline.XfmUtils;
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
	
	// pre-dragged point
	protected double[] preDraggedPoint;
	
	// determines if the last point that was added has a pair
	protected boolean isLastPointPaired = true; // initialize to true, when there are no points
	protected boolean lastPointMoving = false; // "P"
	
	protected boolean pointUpdatePending = false; //
	protected boolean pointUpdatePendingMoving = false; //
	protected Double[] pointToOverride;	// hold a backup of a point for fallback
	
	// keeps track of whether points have been updated
	protected ArrayList<Boolean> changedPositionSinceWarpEstimation;
	protected ArrayList<Integer> indicesOfChangedPoints;
	protected boolean			 elementDeleted = false;
	protected ArrayList<Boolean> needsInverse;
	
	// the transformation 
	protected ThinPlateR2LogRSplineKernelTransform estimatedXfm;
	
	// keeps track of warped points so we don't always have to do it on the fly
	protected ArrayList<Double[]> warpedPoints;
	
	// keep track of edits for undo's and redo's
	protected UndoManager undoRedoManager;
	
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
	
	public LandmarkTableModel( int ndims )
	{
		super();
		this.ndims = ndims;
		PENDING_PT = new double[ ndims ];
		Arrays.fill( PENDING_PT, Double.POSITIVE_INFINITY );
		
		names = new ArrayList<String>();
		activeList = new ArrayList<Boolean>();
		
		movingPts = new ArrayList<Double[]>();
		targetPts = new ArrayList<Double[]>();
		
		preDraggedPoint = new double[ ndims ];
		resetPreDraggedPoint();
		
		pointToOverride = new Double[ ndims ];
		Arrays.fill( pointToOverride, Double.POSITIVE_INFINITY );
		
		if( ndims == 2 ){
			columnNames = columnNames2d;
			numCols = 6;
		}else{
			columnNames = columnNames3d;
		}
		
		warpedPoints = new ArrayList<Double[]>();
		changedPositionSinceWarpEstimation = new ArrayList<Boolean>();
		indicesOfChangedPoints  = new ArrayList<Integer>();
		needsInverse = new ArrayList<Boolean>();
		
		setTableListener();
		
		undoRedoManager = new UndoManager();
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
					//System.out.println("Wrong for pt: " + i );
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
	
	private void updateNextRows()
	{
		nextRowP = numRows;
		nextRowQ = numRows;
		for ( int i = 0; i < numRows; i++ )
		{
			if ( Double.isInfinite( movingPts.get( i )[ 0 ] ))
			{
				pointUpdatePendingMoving = true;
				
				if( i < nextRowP )
					nextRowP = i;
				
				setIsActive( i, false );
			}
			
			if ( Double.isInfinite( targetPts.get( i )[ 0 ] ))
			{
				pointUpdatePendingMoving = true;
				
				if( i < nextRowQ )
					nextRowQ = i;
				
				setIsActive( i, false );
			}
		}
		pointUpdatePendingMoving = false;
	}

	public void resetPreDraggedPoint()
	{
		for ( int d = 0; d < ndims; d++ )
			this.preDraggedPoint[ d ] = Double.NaN;
	}
	
	public boolean isPreDraggedPoint()
	{
		return !Double.isNaN( preDraggedPoint[ 0 ] );
	}
	
	public void setPreDraggedPoint( int i, boolean isMoving )
	{
		if( isPreDraggedPoint() )
		{
			return;
		}
		
		Double[] pt;
		if( isMoving )
			pt = movingPts.get( i );
		else
			pt = targetPts.get( i );
		
		for ( int d = 0; d < ndims; d++ )
		{
			this.preDraggedPoint[ d ] = pt[ d ];
		}
		
//		System.out.println( "set pre dragged point to: " + 
//				preDraggedPoint[0] + " " + preDraggedPoint[1] + " " + preDraggedPoint[2] );
	}
	
	public void setPreDraggedPoint( double[] pt )
	{
		for ( int d = 0; d < ndims; d++ )
		{
			this.preDraggedPoint[ d ] = pt[ d ];
		}
	}
	
	public void setPointToUpdate( int i, boolean isMoving )
	{
		undoRedoManager.addEdit( new ModifyPointEdit( i, PENDING_PT, isMoving ) );
		addHelper( false, i, PENDING_PT, isMoving );
		updateNextRows();
	}
	
	/*
	public void setPointToUpdateOld( int i, boolean isMoving )
	{
		undoRedoManager.addEdit( new ModifyPointEdit( i, PENDING_PT, isMoving ) );
		
		ArrayList< Double[] > pts;
		
		if ( isMoving )
		{
			nextRowP = i;
			pointUpdatePendingMoving = true;
			pts = movingPts;
		} else
		{
			nextRowQ = i;
			pointUpdatePendingMoving = false;
			pts = targetPts;
		}

		for ( int d = 0; d < ndims; d++ )
		{
			pointToOverride[ d ] = pts.get( i )[ d ];
			pts.get( i )[ d ] 			= Double.POSITIVE_INFINITY;
			warpedPoints.get( i )[ d ] 	= Double.POSITIVE_INFINITY;
		}

		changedPositionSinceWarpEstimation.set( i, false );
		activeList.set( i, false );

		pointUpdatePending = true;
		fireTableRowsUpdated( i, i );

	}
	*/
	
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
	
	public boolean isMostRecentPointPaired()
	{
		return isLastPointPaired;
	}
	
	@Override
	public int getColumnCount() {
		return numCols;
	}

	@Override
	public int getRowCount() { 
		return numRows;
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
	
	public void setIsActive( int row, boolean isActive )
	{
		activeList.set( row, isActive );
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
		undoRedoManager.addEdit( new DeleteRowEdit( i ) );
		deleteRowHelper( i );
	}

	private void deleteRowHelper( int i )
	{
		if( i >= names.size() ){
			//System.out.println(" i to big ");
			return;
		}
		
		names.remove( i );
		movingPts.remove( i );
		targetPts.remove( i );
		activeList.remove( i );
		
		
		if( indicesOfChangedPoints.contains( i ))
			indicesOfChangedPoints.remove( indicesOfChangedPoints.indexOf( i ) );
		
		changedPositionSinceWarpEstimation.remove( i );
		warpedPoints.remove( i );
		
		numRows--;
		nextRowP = nextRow( true  );
		nextRowQ = nextRow( false );
		pointUpdatePending = isUpdatePending();

		if( estimatedXfm != null && estimatedXfm.getNumLandmarks() >= (i+1) ){
			estimatedXfm.removePoint( i );
		}
		
		updateNextRows();
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
	
	public int nextRow( boolean isMoving )
	{
		ArrayList< Double[] > pts;
		if( isMoving )
			pts = movingPts;
		else
			pts = targetPts;
		
		if( pts.size() == 0 ){
			return 0;
		}
		
		int i = 0 ;
		while( i < pts.size() && pts.get( i )[ 0 ] < Double.POSITIVE_INFINITY )
			i++;
		
		return i;
	}
	
	/**
	 * Method to help enforce that landmark points are added in pairs.
	 * @return Message if there is an error, empty string otherwise  
	 */
	public String alternateCheck( boolean isMoving )
	{
		// check conditions
		if( !isLastPointPaired )
		{
			if( lastPointMoving && isMoving )
			{
				return "Error, last point added was in moving image, add in fixed image next";
			}
			else if( !lastPointMoving && !isMoving )
			{
				return "Error, last point added was in fixed image, add in moving image next";
			}
		}
		return "";
	}
	
	public Boolean isWarpedPositionChanged( int i )
	{
		return changedPositionSinceWarpEstimation.get( i );
	}

	public void updateWarpedPoint( int i, double[] pt )
	{
		//System.out.println( "updateWarpedPoint " + i + " pt: " + pt[0] + " " + pt[1] + " " + pt[2] );
//		warpedPoints.size()
		for ( int d = 0; d < ndims; d++ )
			warpedPoints.get( i )[ d ] = pt[ d ];

		changedPositionSinceWarpEstimation.set( i, true );
	}
	
	public void printWarpedPoints()
	{
		String s = "";
		int N = changedPositionSinceWarpEstimation.size();
		for( int i = 0; i < N; i++ )
		{
			if( changedPositionSinceWarpEstimation.get( i ))
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
		return changedPositionSinceWarpEstimation;
	}
	
	public void resetWarpedPoints()
	{
		Collections.fill( changedPositionSinceWarpEstimation, false );
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

	private int addHelper( double[] pt, boolean isMoving )
	{

		int nextRow = -1;
		if ( isMoving )
		{
			nextRow = nextRowP;
		} else
		{
			nextRow = nextRowQ;
		}

		if ( nextRow == movingPts.size() )
		{
			addHelper( true, nextRow, pt, isMoving );
			return nextRow;
		} else
		{
			addHelper( false, nextRow, pt, isMoving );
			return nextRow;
		}

	}

//	private boolean addHelper( int index, double[] pt, boolean isMoving )
//	{
//		addHelper( true, index, pt, isMoving );
//		return true;
//	}

	/**
	 * Adds a point, but does not add an edit to the undo stack.
	 * Returns true if this is the first point in the pair to be added.
	 * 
	 * @param pt
	 * @param isMoving
	 * @return
	 */
	private void addHelper( boolean isFirst, int index, double[] pt, boolean isMoving )
	{
		System.out.println("\naddHelper");
		System.out.println("isFirst: " + isFirst );
		System.out.println("isMoving: " + isMoving );
		System.out.println("pt: " + XfmUtils.printArray(pt));
		System.out.println("index: " + index + "\n");
		
		if( isFirst )
		{
			//System.out.println("addHelper first");
			
			Double[] movingPt = new Double[ ndims ];
			Double[] targetPt = new Double[ ndims ];
			
			if( isMoving )
			{
				for( int i = 0; i < ndims; i++ ){ movingPt[ i ] = pt[ i ]; }
				Arrays.fill( targetPt, new Double( Double.POSITIVE_INFINITY ) );
			}
			else
			{
				for( int i = 0; i < ndims; i++ ){ targetPt[ i ] = pt[ i ]; }
				Arrays.fill( movingPt, new Double( Double.POSITIVE_INFINITY ) );
			}
			
			movingPts.add( index, movingPt );
			targetPts.add( index, targetPt );
			
			names.add( index, "Pt-" + index );
			activeList.add( index, false );
			warpedPoints.add( index, new Double[ ndims ] );
			changedPositionSinceWarpEstimation.add( index, false );
			
			fireTableRowsInserted( index, index );
			
			numRows++;
		}
		else
		{
			//System.out.println("addHelper not first");
			
			ArrayList< Double[] > pts;
			
			if( isMoving )
				pts = movingPts;
			else
				pts = targetPts;
			
			// add a new one
			Double[] exPts = new Double[ ndims ];
			for( int i = 0; i < ndims; i++ ){
				exPts[ i ] = pt[ i ];
			}
			pts.set( index, exPts );
			
			// modify current
//			Double[] curPt = pts.get( index );
//			for( int i = 0; i < ndims; i++ ){
//				curPt[ i ] = pt[ i ];
//			}
			
			activeList.set( index, true );
			
			if( isMoving )
			{
				updateWarpedPoint( index, pt );
			}
			
			fireTableRowsUpdated( index, index );
		}
		
		markAsChanged( index );
		
		if( isMoving )
			nextRowP = nextRow( isMoving );
		else
			nextRowQ = nextRow( isMoving );
		
		updateNextRows();
		firePointUpdated( nextRowP, isMoving );
		
	}
	
	private void markAsChanged( int index )
	{
		for( int i = 0; i < this.numRows; i++ )
		{
			if( !indicesOfChangedPoints.contains( i ))
				indicesOfChangedPoints.add( i ); 
		}
	}
	
	/**
	 * Adds a point, adding an edit to the undo stack.
	 * @param pt
	 * @param isMoving
	 * @return
	 */
	public int add( double[] pt, boolean isMoving )
	{
		System.out.println("add new");
		int nrStart = this.getRowCount();
		int nextRow = addHelper( pt, isMoving );
		
		addUndoable( pt, isMoving, nextRow, (nextRow == nrStart) );
		return nextRow;
	}
	
	private void addUndoable( double[] pt, boolean isMoving, int row, boolean isNew )
	{
		System.out.println("addUndoable isNew: " + isNew );
		if( isNew )
			undoRedoManager.addEdit( new AddPointEdit( row, pt, isMoving ) );
		else
			undoRedoManager.addEdit( new ModifyPointEdit( row, 
					this.PENDING_PT, pt,
					null, null, isMoving ));
	}
	
//	private void addUndoable( double[] pt, boolean isMoving )
//	{
//		int nextRow = -1;
//		
//		if( isMoving )	nextRow = nextRowP;
//		else			nextRow = nextRowQ;
//		
//		addUndoable( pt, isMoving, nextRow );
//	}
	
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

		//System.out.println("changed pts: " + indicesOfChangedPoints);
		//System.out.println("moving size" + movingPts.size());
		//System.out.println("target size" + targetPts.size());

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
	
	/**
	 * Loads this table from a file
	 * @param f
	 * @throws IOException 
	 */
	public void load( File f ) throws IOException
	{
		CSVReader reader = new CSVReader( new FileReader( f.getAbsolutePath() ));
		List<String[]> rows = reader.readAll();
		reader.close();
		
		names.clear();
		activeList.clear();
		movingPts.clear();
		targetPts.clear();
		
		changedPositionSinceWarpEstimation.clear();
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
			
			movingPts.add( movingPt );
			targetPts.add( targetPt );
			
			warpedPoints.add( new Double[ ndims ] );
			changedPositionSinceWarpEstimation.add( false );
			
			fireTableRowsInserted( numRows, numRows );
			numRows++;
		}
		
		nextRowP = nextRow( true );
		nextRowQ = nextRow( false );
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
		estimatedXfm = new ThinPlateR2LogRSplineKernelTransform ( ndims );
		
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
	public void setPoint( int row, boolean isMoving, double[] pt )
	{
		setPoint( row, isMoving, pt, true );
	}
	
	public void setPoint( int row, boolean isMoving, double[] pt, boolean isUndoable )
	{
		if( isUndoable )
			undoRedoManager.addEdit( new ModifyPointEdit( row, pt, isMoving ) );
		
		setPointHelper( row, isMoving, pt );
	}

	public void setPointHelper( int row, boolean isMoving, double[] pt )
	{
		ArrayList< Double[] > pts;

		int offset = 0;
		if ( isMoving )
			pts = movingPts;
		else
		{
			pts = targetPts;
			offset = ndims;
		}

		for ( int i = 0; i < ndims; i++ )
		{
			pts.get( row )[ i ] = pt[ i ];
			fireTableCellUpdated( row, ndims + offset + i );
		}

		if ( (estimatedXfm != null) )
		{
			if ( !indicesOfChangedPoints.contains( row ) )
				indicesOfChangedPoints.add( row );

//			 if( !isMoving && !isWarpedPositionChanged( row ))
//				 updateWarpedPoint( row, pt );
//			 else if( isMoving )
//			 	updateWarpedPoint( row, pt );

			// // moving and target are "switched" since we need an
			// "inverse tps"
			// if( isMoving )
			// estimatedXfm.updateTargetLandmark( row, pt );
			// else
			// estimatedXfm.updateSourceLandmark( row, pt );

		}
		
		updateNextRows(); 
	}
	
	public static String print( Double[] d )
	{
		String out = "";
		for( int i=0; i<d.length; i++)
			out += d[i] + " ";
		
		return out;
	}
	
	public void setValueAt(Object value, int row, int col) {
        if (DEBUG) {
            System.out.println("Setting value at " + row + "," + col
                               + " to " + value
                               + " (an instance of "
                               + value.getClass() + ")");
        }

        if( col == NAMECOLUMN ){
        	names.set(row, (String)value );
        }else if( col == ACTIVECOLUMN ){
        	activeList.set(row, (Boolean)value );
        }else if( col < 2 + ndims ){
        	Double[] thesePts = movingPts.get(row);
        	thesePts[ col - 2 ] = ((Double)value).doubleValue();
        }else{
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

	public UndoManager getUndoManager()
	{
		return undoRedoManager;
	}
	
	public class AddPointEdit extends AbstractUndoableEdit
	{
		private static final long serialVersionUID = -2912174487193763272L;

		private final int index;
		private final double[] oldpt;
		private final double[] newpt;
		private final boolean isMoving;
		private final boolean isNewRow;
		
		private final double[] warpedPos;

		public AddPointEdit( final int index, final double[] pt, final boolean isMoving )
		{
			System.out.println("made add pt edit with warped pos");
			System.out.println("index: " + index );
			
			this.index = index;
			this.newpt = pt;
			this.isMoving = isMoving;
			this.isNewRow = (index == LandmarkTableModel.this.numRows - 1);

			System.out.println("is new: " + isNewRow );
			
			if ( !isNewRow )
			{
				if ( isMoving )
					oldpt = toPrimitive( LandmarkTableModel.this.movingPts.get( index ) );
				else
					oldpt = toPrimitive( LandmarkTableModel.this.targetPts.get( index ) );
			} else
				oldpt = null;
			
			if( LandmarkTableModel.this.isWarpedPositionChanged( index ))
			{
				System.out.println("made add pt edit with warped pos");
				warpedPos = LandmarkTableModel.toPrimitive( LandmarkTableModel.this.getWarpedPoints().get( index ));
			}
			else
			{
				warpedPos = null;
			}
		}

		@Override
		public void undo()
		{
			System.out.println("undo add");
			if ( isNewRow )
				LandmarkTableModel.this.deleteRowHelper( index );
			else
				LandmarkTableModel.this.setPointHelper( index, isMoving, oldpt );
			
		}

		@Override
		public void redo()
		{
			System.out.println("redo add");
			int nr = LandmarkTableModel.this.numRows;
			if ( isNewRow )
				LandmarkTableModel.this.addHelper(  true, nr, newpt, isMoving );
			else
				LandmarkTableModel.this.addHelper( false, nr-1, newpt, isMoving );
			
			if( warpedPos != null )
			{
				System.out.println("warped");
				LandmarkTableModel.this.updateWarpedPoint(  LandmarkTableModel.this.numRows, warpedPos );
			}
		}
	}
	
	public class ModifyPointEdit extends AbstractUndoableEdit
	{
		private static final long serialVersionUID = 1805061472166752857L;

		private final int index;
		private final double[] oldpt;
		private final double[] newpt;
		private final boolean isMoving;
		
		private final boolean  isWarped;
		private final double[] newWarpedPos;
		private final double[] oldWarpedPos;

		public ModifyPointEdit( final int index, 
				final double[] oldpt, double[] newpt, 
				final double[] oldwarped, final double[] warped, 
				final boolean isMoving )
		{
			this.index = index;
			this.isMoving = isMoving;
			
			this.oldpt = oldpt;
			this.newpt = newpt;
			
			System.out.println("ModifyPointEdit2");
			System.out.println("oldpt: " + XfmUtils.printArray( oldpt ));
			System.out.println("newpt: " + XfmUtils.printArray( newpt ));
			
			if( oldwarped != null && warped == null )
			{
				isWarped = false;
				oldWarpedPos = null;
				newWarpedPos = null;
			}
			else
			{
				isWarped = true;
				oldWarpedPos = oldwarped;
				newWarpedPos = warped;
			}
		}
		
		public ModifyPointEdit( final int index, 
				double[] pt, 
				final boolean isMoving )
		{
			System.out.println("ModifyPointEdit");
			this.index = index;
			this.isMoving = isMoving;
			
			if( LandmarkTableModel.this.isWarpedPositionChanged( index ) )
			{
				isWarped = true;
				oldWarpedPos = LandmarkTableModel.toPrimitive( LandmarkTableModel.this.getWarpedPoints().get( index ));
				
				newWarpedPos = pt;
				this.newpt = LandmarkTableModel.this.getTransform().apply( pt );
				
			}
			else
			{
				isWarped = false;
				newWarpedPos = null;
				oldWarpedPos = null;
				
				this.newpt = pt;
			}
			
			if( LandmarkTableModel.this.isPreDraggedPoint() )
			{
				oldpt = copy( LandmarkTableModel.this.preDraggedPoint );
				LandmarkTableModel.this.resetPreDraggedPoint();
			}
			else{
				if ( isMoving )
					oldpt = toPrimitive( LandmarkTableModel.this.movingPts.get( index ) );
				else
					oldpt = toPrimitive( LandmarkTableModel.this.targetPts.get( index ) );
			}
		}

		@Override
		public void undo()
		{
			LandmarkTableModel.this.addHelper( false, index, oldpt, isMoving );
			LandmarkTableModel.this.updateNextRows();
		}

		@Override
		public void redo()
		{
			//System.out.println( "  ModifyPointEdit:  " + index + " " + isMoving + " " + XfmUtils.printArray(  newpt ));
			LandmarkTableModel.this.addHelper( false, index, newpt, isMoving );
			LandmarkTableModel.this.updateNextRows();
		}
	}

	public class DeleteRowEdit extends AbstractUndoableEdit
	{
		private static final long serialVersionUID = -3624020789748090982L;

		private final int index;
		private final double[] movingPt;
		private final double[] targetPt;

		public DeleteRowEdit( final int index )
		{
			this.index = index;
			movingPt = toPrimitive( LandmarkTableModel.this.movingPts.get( index ) );
			targetPt = toPrimitive( LandmarkTableModel.this.targetPts.get( index ) );
		}

		@Override
		public void undo()
		{
			LandmarkTableModel.this.addHelper( true, index, movingPt, true );
			LandmarkTableModel.this.addHelper( false, index, targetPt, false );
		}

		@Override
		public void redo()
		{
			LandmarkTableModel.this.deleteRowHelper( index );
		}
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
