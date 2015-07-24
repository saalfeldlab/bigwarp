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

import javax.swing.table.AbstractTableModel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author John Bogovic
 *
 */
public class LandmarkTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -5865789085410559166L;
	
	private boolean DEBUG = false;
	
	protected int numCols = 8;
	protected int numRows = 0;
	
	protected int nextRowP = 0;
	protected int nextRowQ = 0;
	
	protected ArrayList<String> 	names;
	protected ArrayList<Boolean>	activeList;
	protected ArrayList<Double[]>	pts;
	
	// determines if the last point that was added has a pair
	protected boolean isLastPointPaired = true; // initialize to true, when there are no points
	protected boolean lastPointMoving = false; // "P"
	
	protected boolean pointUpdatePending = false; //
	protected boolean pointUpdatePendingMoving = false; //
	protected Double[] pointToOverride;	// hold a backup of a point for fallback
	
	// keeps track of whether points have been updated
	protected ArrayList<Boolean> changedPositionSinceWarpEstimation; 
	
	// the transformation 
	protected ThinPlateR2LogRSplineKernelTransform estimatedXfm;
	
	// keeps track of warped points so we don't always have to do it on the fly
	protected ArrayList<Double[]> warpedPoints;
	
	final String[] columnNames = new String[]
			{
			"Name", "Active",
			"px","py","pz",
			"qx","qy","qz"
			};
	
	public LandmarkTableModel(){
		super();
		names = new ArrayList<String>();
		activeList = new ArrayList<Boolean>();
		pts   = new ArrayList<Double[]>();
		pointToOverride = new Double[ 3 ];
		Arrays.fill( pointToOverride, Double.POSITIVE_INFINITY );
		
		warpedPoints = new ArrayList<Double[]>();
		changedPositionSinceWarpEstimation = new ArrayList<Boolean>();
	}
	
	public ThinPlateR2LogRSplineKernelTransform getTransform()
	{
		return estimatedXfm;
	}
	
	public void printDistances()
	{
		for( int i = 0; i < numRows; i++ )
		{
			System.out.print( pts.get( i )[ 3 ].doubleValue() - estimatedXfm.getSourceLandmarks()[ 0 ][ i ] );
			System.out.print( " " + (pts.get( i )[ 4 ].doubleValue() - estimatedXfm.getSourceLandmarks()[ 1 ][ i ]) );
			System.out.print( " " + (pts.get( i )[ 5 ].doubleValue() - estimatedXfm.getSourceLandmarks()[ 2 ][ i ]) + "\n");
		}
	}
	
	public boolean validateTransformPoints()
	{
		for( int i = 0; i < numRows; i++ )
		{
			if( pts.get( i )[ 0 ].doubleValue() != estimatedXfm.getSourceLandmarks()[ i ][ 0 ] ||
				pts.get( i )[ 1 ].doubleValue() != estimatedXfm.getSourceLandmarks()[ i ][ 1 ] ||
				pts.get( i )[ 2 ].doubleValue() != estimatedXfm.getSourceLandmarks()[ i ][ 2 ])
					return false;
		}
		
		return true;
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
	
	public void setPointToUpdate( int i, boolean isMoving )
	{
		if( isMoving )
		{
			nextRowP = i;
			pointToOverride[ 0 ] = pts.get( i )[ 0 ];
			pointToOverride[ 1 ] = pts.get( i )[ 1 ];
			pointToOverride[ 2 ] = pts.get( i )[ 2 ];
			
			pts.get( i )[ 0 ] = Double.POSITIVE_INFINITY;
			pts.get( i )[ 1 ] = Double.POSITIVE_INFINITY;
			pts.get( i )[ 2 ] = Double.POSITIVE_INFINITY;
			
			warpedPoints.get( i )[ 0 ] = Double.POSITIVE_INFINITY;
			warpedPoints.get( i )[ 1 ] = Double.POSITIVE_INFINITY;
			warpedPoints.get( i )[ 2 ] = Double.POSITIVE_INFINITY;
			changedPositionSinceWarpEstimation.set( i, true );
			
			activeList.set( i, false );
			
			pointUpdatePendingMoving = true;
		}
		else 
		{
			nextRowQ = i;
			pointToOverride[ 0 ] = pts.get( i )[ 3 ];
			pointToOverride[ 1 ] = pts.get( i )[ 4 ];
			pointToOverride[ 2 ] = pts.get( i )[ 5 ];
			
			pts.get( i )[ 3 ] = Double.POSITIVE_INFINITY;
			pts.get( i )[ 4 ] = Double.POSITIVE_INFINITY;
			pts.get( i )[ 5 ] = Double.POSITIVE_INFINITY;
			
			warpedPoints.get( i )[ 0 ] = Double.POSITIVE_INFINITY;
			warpedPoints.get( i )[ 1 ] = Double.POSITIVE_INFINITY;
			warpedPoints.get( i )[ 2 ] = Double.POSITIVE_INFINITY;
			changedPositionSinceWarpEstimation.set( i, true );
			
			activeList.set( i, false );
			
			pointUpdatePendingMoving = false;
		}
		pointUpdatePending = true;
		fireTableRowsUpdated( i, i );
		
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
		int i = 0;
		int addme = 0;
		if( pointUpdatePendingMoving )
		{
			i = nextRowP;
		}
		else
		{
			i = nextRowQ;
			addme = 3;
		}
		
		pts.get( i )[ 0 + addme ] = Double.POSITIVE_INFINITY;
		pts.get( i )[ 1 + addme ] = Double.POSITIVE_INFINITY;
		pts.get( i )[ 2 + addme ] = Double.POSITIVE_INFINITY;
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
	
	public ArrayList<Double[]> getPoints() {
		return pts;
	}
	
	public ArrayList<String> getNames() {
		return names;
	}
	
	public void setColumnName( int col, String name ){
		names.set( col, name );
		fireTableRowsUpdated( col, col );
	}
	
	public Class<?> getColumnClass( int col ){
		if( col == 0 ){
			return String.class;
		}else if( col == 1 ){
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
	
	public int nextRow( boolean isMoving )
	{
		if( pts.size() == 0 ){
			return 0;
		}
		
		int addme = 0;
		if( !isMoving )
			addme = 3;
		
		int i = 0 ;
		while( i < pts.size() && pts.get( i )[ 0 + addme ] < Double.POSITIVE_INFINITY )
			i++;
		
		return i;
	}
	
	public void addOld( String name, Double[] pt ){
		names.add(name);
		pts.add( pt );
		activeList.add( true );
		fireTableRowsInserted( numRows, numRows );
		numRows++;
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
	
	public void updateWarpedPoint( int i , double[] pt )
	{
		warpedPoints.get( i )[ 0 ] = pt[ 0 ];
		warpedPoints.get( i )[ 1 ] = pt[ 1 ];
		warpedPoints.get( i )[ 2 ] = pt[ 2 ];
		changedPositionSinceWarpEstimation.set( i, true );
		// System.out.println("updateWarpedPoint " + i + ": " + pt[0] + " " + pt[1] + " " + pt[2] );
	}
	
	public ArrayList<Double[]> getWarpedPoints( )
	{
		return warpedPoints;
	}
	
	public void resetWarpedPoints()
	{
		Collections.fill( changedPositionSinceWarpEstimation, false );
	}
	
	public String add( double[] pt, boolean isMoving )
	{
	
		int addme = 0;
		if( !isMoving )
			addme = 3;
		
		if( isMoving )
		{
			
			if( nextRowP == pts.size() )
			{
				Double[] ptPQ = new Double[ 6 ];
				Arrays.fill( ptPQ, new Double( Double.POSITIVE_INFINITY ) );
				for( int i = 0; i < 3; i++ )
					ptPQ[ i + addme ] = pt[ i ];
				
				pts.add( ptPQ );
				names.add( "Pt-" + nextRowP );
				activeList.add( false );
				warpedPoints.add( new Double[ 3 ] );
				changedPositionSinceWarpEstimation.add( false );
				
				fireTableRowsInserted( nextRowP, nextRowP );
				nextRowP = nextRow( isMoving );
				numRows++;
			}
			else
			{
				Double[] exPts = pts.get( nextRowP );
				for( int i = 0; i < 3; i++ )
					exPts[ i + addme ] = pt[ i ];
				
				pts.set( nextRowP, exPts );
				activeList.set( nextRowP, true );
				changedPositionSinceWarpEstimation.set( nextRowP, false );
				
				warpedPoints.get( nextRowP )[ 0 ] = pt[ 0 ];
				warpedPoints.get( nextRowP )[ 1 ] = pt[ 1 ];
				warpedPoints.get( nextRowP )[ 2 ] = pt[ 2 ];
				
				if( estimatedXfm != null )
				{
					double[] tgt = new double[]{ 
							pts.get(nextRowQ)[3],
							pts.get(nextRowQ)[4],
							pts.get(nextRowQ)[5]};
					
					estimatedXfm.addMatch( tgt, pt );
				}
				// estimatedXfm.updateTargetLandmark( nextRowP, pt );
				
				fireTableCellUpdated( nextRowP, 1 );
				fireTableCellUpdated( nextRowP, 2 );
				fireTableCellUpdated( nextRowP, 3 );
				fireTableCellUpdated( nextRowP, 4 );
				nextRowP = nextRow( isMoving );
			}
		}
		else
		{
			
			if( nextRowQ == pts.size() )
			{
				Double[] ptPQ = new Double[ 6 ];
				Arrays.fill( ptPQ, new Double( Double.POSITIVE_INFINITY ) );
				for( int i = 0; i < 3; i++ )
					ptPQ[ i + addme ] = pt[ i ];
				
				pts.add( ptPQ );
				names.add( "Pt-" + nextRowQ );
				activeList.add( false );
				warpedPoints.add( new Double[ 3 ] );
				changedPositionSinceWarpEstimation.add( false );
				
				fireTableRowsInserted( nextRowQ, nextRowQ );
				nextRowQ = nextRow( isMoving );
				numRows++;
			}
			else
			{
				Double[] exPts = pts.get( nextRowQ );
				for( int i = 0; i < 3; i++ )
					exPts[ i + addme ] = pt[ i ];
				
				pts.set( nextRowQ, exPts );
				activeList.set( nextRowQ, true );
				
				if( estimatedXfm != null )
				{
					double[] tgt = new double[]{ 
							pts.get(nextRowQ)[0],
							pts.get(nextRowQ)[1],
							pts.get(nextRowQ)[2]};
					
					estimatedXfm.addMatch( pt, tgt );
				}
				//estimatedXfm.updateSourceLandmark( nextRowQ, pt );
				
				fireTableCellUpdated( nextRowQ, 1 );
				fireTableCellUpdated( nextRowQ, 5 );
				fireTableCellUpdated( nextRowQ, 6 );
				fireTableCellUpdated( nextRowQ, 7 );
				nextRowQ = nextRow( isMoving );
			}
		}
		
		return "";
	}
	
	/**
	 * Add a landmark point, either to the moving of fixed image
	 */
	public String addAlternate( double[] pt, boolean isMoving )
	{
		String errMessage = alternateCheck( isMoving );
		if( !errMessage.isEmpty() )
		{
			System.err.println( errMessage );
			return errMessage;
		}
		
		Double[] ptPQ;
		if( isLastPointPaired )
		{
			names.add( "Pt-" + numRows );
			
			ptPQ = new Double[ 6 ];
			Arrays.fill( ptPQ, Double.NaN );
		}
		else
		{
			ptPQ = pts.get( pts.size() - 1 );
		}
		
		if( isMoving )
		{
			for( int i = 0; i < 3; i++ )
				ptPQ[ i ] = pt[ i ];
		}
		else
		{
			for( int i = 0; i < 3; i++ )
				ptPQ[ i + 3 ] = pt[ i ];
		}
		
		if( isLastPointPaired ) // then we're adding a new row
		{
			pts.add( ptPQ );
			activeList.add( true );
			fireTableRowsInserted( numRows, numRows );
			isLastPointPaired = false;
		}
		else // we're updating the last added row
		{
			pts.set( numRows, ptPQ );
			fireTableRowsUpdated( numRows, numRows );
			isLastPointPaired = true;
		}
		
		// only update numRows when we've completed a row
		// ( ie have added points for both fixed and moving images)
		if( isLastPointPaired )
		{
			numRows++;
		}
		
		lastPointMoving = isMoving;
		
		return "";
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
		pts.clear();
		
		changedPositionSinceWarpEstimation.clear();
		warpedPoints.clear();
		
		numRows = 0;
		for( String[] row : rows )
		{
			if( row.length < 8 )
				throw new IOException( "Invalid file - not enough columns" );
			
			names.add( row[ 0 ] );
			activeList.add( Boolean.parseBoolean( row[ 1 ]) );
			
			Double[] thisPtMatch = new Double[ 6 ];
			thisPtMatch[ 0 ] = Double.parseDouble( row[ 2 ]);
			thisPtMatch[ 1 ] = Double.parseDouble( row[ 3 ]);
			thisPtMatch[ 2 ] = Double.parseDouble( row[ 4 ]);
			thisPtMatch[ 3 ] = Double.parseDouble( row[ 5 ]);
			thisPtMatch[ 4 ] = Double.parseDouble( row[ 6 ]);
			thisPtMatch[ 5 ] = Double.parseDouble( row[ 7 ]);
			pts.add( thisPtMatch );
			
			warpedPoints.add( new Double[ 3 ] );
			changedPositionSinceWarpEstimation.add( false );
			
			fireTableRowsInserted( numRows, numRows );
			numRows++;
		}
		
		nextRowP = nextRow( true );
		nextRowQ = nextRow( false );
	}
	
	public void initTransformation()
	{
		//TODO: better to pass a factory here so the transformation can be arbitrary
		estimatedXfm = new ThinPlateR2LogRSplineKernelTransform ( 3 );
		
		int numLandmarks = 0;
		for( int i = 0; i < this.numRows; i++ )
			if( activeList.get( i ))
				numLandmarks++;
		
		double[][] mvgPts = new double[ 3 ][ numLandmarks ];
		double[][] tgtPts = new double[ 3 ][ numLandmarks ];
		
		int k = 0;
		for( int i = 0; i < this.numRows; i++ ) 
		{
			if( activeList.get( i ))
			{
				for( int d = 0; d < 3; d++ )
				{
					mvgPts[ d ][ k ] = pts.get( i )[ d ];
					tgtPts[ d ][ k ] = pts.get( i )[ 3 + d ];
				}
				k++;
			}
			
		}
		
		// need to find the "inverse TPS" so exchange moving and tgt
		estimatedXfm.setLandmarks( tgtPts, mvgPts );
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
		
		for( int i = 0; i < N; i++ )
		{
			String[] row = new String[ 8 ];
			row[ 0 ] = names.get( i );
			row[ 1 ] = activeList.get( i ).toString();
			
			row[ 2 ] = pts.get( i )[ 0 ].toString();
			row[ 3 ] = pts.get( i )[ 1 ].toString();
			row[ 4 ] = pts.get( i )[ 2 ].toString();
			row[ 5 ] = pts.get( i )[ 3 ].toString();
			row[ 6 ] = pts.get( i )[ 4 ].toString();
			row[ 7 ] = pts.get( i )[ 5 ].toString();
			
			rows.add( row );
			
		}
		csvWriter.writeAll( rows );
		csvWriter.close();
		
	}
	
	public void setPoint( int row, boolean isMoving, double[] pt )
	{
		
		//System.out.println( "LTM BEFORE - " + pts.get(row));
		int addme = 0;
		if( !isMoving )
			addme = 3;
		
		for( int i = 0; i < 3; i++ )
		{
			pts.get(row)[addme + i] = new Double( pt[i]);
			fireTableCellUpdated(row, 3 + addme + i );
		}
		
		if(( estimatedXfm != null ))
        {
			// moving and target are "switched" since we need an "inverse tps"
			if( isMoving )
				estimatedXfm.updateTargetLandmark( row, pt );
			else
				estimatedXfm.updateSourceLandmark( row, pt );
				
        }
		
		//System.out.println( "LTM AFTER - " + print(pts.get(row)));
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

        if( col == 0 ){
        	names.set(row, (String)value );
        }else if( col == 1 ){
        	activeList.set(row, (Boolean)value );
        }else{
        	Double[] thesePts = pts.get(row);
        	thesePts[col-2] = ((Double)value).doubleValue();
        }
        
        
        
        fireTableCellUpdated(row, col);

    }
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if( columnIndex == 0 ){
			return names.get( rowIndex );
		}else if( columnIndex == 1 ){
			return activeList.get( rowIndex );
		}else{
			return new Double(pts.get(rowIndex)[columnIndex-2]);
		}
	}
	
	/**
	 * Only allow editing of the first ("Name") and 
	 * second ("Active") column
	 */
	public boolean isCellEditable( int row, int col ){
		return ( col <= 1 );
	}

}
