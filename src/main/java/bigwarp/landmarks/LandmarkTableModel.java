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
	
	protected int ndims = 3;
	
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
	
	public LandmarkTableModel( int ndims ){
		super();
		this.ndims = ndims;
		
		names = new ArrayList<String>();
		activeList = new ArrayList<Boolean>();
		pts   = new ArrayList<Double[]>();
		
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
				System.out.print( " " + (pts.get( i )[ ndims + d ].doubleValue() - estimatedXfm.getSourceLandmarks()[ d ][ i ]) );
			}
		}
	}
	
	public boolean validateTransformPoints()
	{
		for( int i = 0; i < numRows; i++ )
		{
			for( int d = 0; d < ndims; d++ )
			{
				if ( pts.get( i )[ ndims + d ].doubleValue() != estimatedXfm.getSourceLandmarks()[ d ][ i ] )
					return false;
			}
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
		int offset = 0;
		if( isMoving )
		{
			nextRowP = i;
			pointUpdatePendingMoving = true;
		}
		else 
		{
			nextRowQ = i;
			offset = ndims;
			pointUpdatePendingMoving = false;
		}
		
		for( int d = 0; d < ndims; d++ )
		{
			pointToOverride[ d ] = pts.get( i )[ offset + d ];
			pts.get( i )[ offset + d ] = Double.POSITIVE_INFINITY;
			warpedPoints.get( i )[ d ] = Double.POSITIVE_INFINITY;
		}
		changedPositionSinceWarpEstimation.set( i, true );
		activeList.set( i, false );
		
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
		int offset = 0;
		if( pointUpdatePendingMoving )
		{
			i = nextRowP;
		}
		else
		{
			i = nextRowQ;
			offset = 3;
		}
		
		for( int d = 0; d < ndims; d++ )
			pts.get( i )[ d + offset ] = Double.POSITIVE_INFINITY;
		
		
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
			addme = ndims;
		
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
		for( int d = 0; d < ndims; d++ )
			warpedPoints.get( i )[ d ] = pt[ d ];
		
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
	
	protected void firePointUpdated( int row, boolean isMoving )
	{
		for( int d = 0; d < ndims; d++ )
			fireTableCellUpdated( row, 2 + d );
	}
	
	public String add( double[] pt, boolean isMoving )
	{
		int offset = 0;
		int nextRow = -1;
		if( isMoving )
		{
			nextRow = nextRowP;
		}
		else
		{
			nextRow = nextRowQ;
			offset = ndims;
		}
		
		if( nextRow == pts.size() )
		{
			Double[] ptPQ = new Double[ 2 * ndims ];
			Arrays.fill( ptPQ, new Double( Double.POSITIVE_INFINITY ) );
			for( int i = 0; i < ndims; i++ )
				ptPQ[ i + offset ] = pt[ i ];
			
			pts.add( ptPQ );
			names.add( "Pt-" + nextRow );
			activeList.add( false );
			warpedPoints.add( new Double[ ndims ] );
			changedPositionSinceWarpEstimation.add( false );
			
			fireTableRowsInserted( nextRow, nextRow );
			
			numRows++;
		}
		else
		{
			Double[] exPts = pts.get( nextRow );
			for( int i = 0; i < ndims; i++ )
				exPts[ i + offset ] = pt[ i ];
			
			pts.set( nextRow, exPts );
			activeList.set( nextRow, true );
			
			
			double[] tgtpt = null;
			double[] srcpt = null;
			double[] pointerPt = null;
			
			int copyOffset = 0;
			if( isMoving )
			{
				changedPositionSinceWarpEstimation.set( nextRow, false );
				for( int d = 0; d < ndims; d++ )
					warpedPoints.get( nextRowP )[ d ] = pt[ d ];
				
				copyOffset = ndims;
				
				srcpt = pt;
				tgtpt = new double[ ndims ];
				pointerPt  = tgtpt; 
			}
			else
			{
				tgtpt = pt;
				srcpt = new double[ ndims ];
				pointerPt  = srcpt;
			}
			
			if( estimatedXfm != null )
			{
				for( int i = 0; i < ndims; i++ )
					pointerPt[ i ] = pts.get( nextRow )[ copyOffset + i ];
				
				estimatedXfm.addMatch( tgtpt, srcpt );
			}
		}
		
		if( isMoving )
			nextRowP = nextRow( isMoving );
		else
			nextRowQ = nextRow( isMoving );
		
		firePointUpdated( nextRowP, isMoving );
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
			
			Double[] thisPtMatch = new Double[ 2 * ndims ];
			
			thisPtMatch[ 0 ] = Double.parseDouble( row[ 2 ]);
			thisPtMatch[ 1 ] = Double.parseDouble( row[ 3 ]);
			thisPtMatch[ 2 ] = Double.parseDouble( row[ 4 ]);
			thisPtMatch[ 3 ] = Double.parseDouble( row[ 5 ]);
			
			if( ndims == 3 ) // if 3d add the qy and qz
			{
				thisPtMatch[ 4 ] = Double.parseDouble( row[ 6 ]);
				thisPtMatch[ 5 ] = Double.parseDouble( row[ 7 ]);
			}
			
			pts.add( thisPtMatch );
			
			warpedPoints.add( new Double[ ndims ] );
			changedPositionSinceWarpEstimation.add( false );
			
			fireTableRowsInserted( numRows, numRows );
			numRows++;
		}
		
		nextRowP = nextRow( true );
		nextRowQ = nextRow( false );
	}
	
	public void initTransformation()
	{
		//TODO: better to pass a factory here so the transformation can be any
		// CoordinateTransform ( not just a TPS )
		estimatedXfm = new ThinPlateR2LogRSplineKernelTransform ( ndims );
		
		int numLandmarks = 0;
		for( int i = 0; i < this.numRows; i++ )
			if( activeList.get( i ))
				numLandmarks++;
		
		double[][] mvgPts = new double[ ndims ][ numLandmarks ];
		double[][] tgtPts = new double[ ndims ][ numLandmarks ];
		
		int k = 0;
		for( int i = 0; i < this.numRows; i++ ) 
		{
			if( activeList.get( i ))
			{
				for( int d = 0; d < ndims; d++ )
				{
					mvgPts[ d ][ k ] = pts.get( i )[ d ];
					tgtPts[ d ][ k ] = pts.get( i )[ ndims + d ];
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
		
		int rowLength = 2 * ndims + 2;
		for( int i = 0; i < N; i++ )
		{
			String[] row = new String[ rowLength ];
			row[ 0 ] = names.get( i );
			row[ 1 ] = activeList.get( i ).toString();
			
			row[ 2 ] = pts.get( i )[ 0 ].toString();
			row[ 3 ] = pts.get( i )[ 1 ].toString();
			row[ 4 ] = pts.get( i )[ 2 ].toString();
			row[ 5 ] = pts.get( i )[ 3 ].toString();
			
			if( ndims == 3 )
			{
				row[ 6 ] = pts.get( i )[ 4 ].toString();
				row[ 7 ] = pts.get( i )[ 5 ].toString();
			}
			
			rows.add( row );
			
		}
		csvWriter.writeAll( rows );
		csvWriter.close();
		
	}
	
	public void setPoint( int row, boolean isMoving, double[] pt )
	{
		
		//System.out.println( "LTM BEFORE - " + pts.get(row));
		int offset = 0;
		if( !isMoving )
			offset = ndims;
		
		for( int i = 0; i < ndims; i++ )
		{
			pts.get(row)[offset + i] = new Double( pt[i] );
			fireTableCellUpdated( row, ndims + offset + i );
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
