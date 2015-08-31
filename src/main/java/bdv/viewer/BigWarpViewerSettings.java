package bdv.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

public class BigWarpViewerSettings  { 

	/**
	 * Defines the key for the main color. Accepted values are color.
	 */
	public static final String KEY_COLOR = "Color";
	
	public static final String KEY_INACTIVE_COLOR = "InactiveColor";
	
	public static final String KEY_UNMATCHED_COLOR = "InactiveColor";

	/**
	 * Defines the key for the spot visibility. Values are boolean. If
	 * <code>false</code>, spots are not visible.
	 * */
	public static final String KEY_SPOTS_VISIBLE = "SpotsVisible";

	/**
	 * Defines the key for the spot name display. Values are boolean. If
	 * <code>false</code>, spot names are not visible.
	 */
	public static final String KEY_DISPLAY_SPOT_NAMES = "DisplaySpotNames";

	/**
	 * Defines the key for the spot radius ratio. Value should be a positive
	 * {@link Double} object. Spots will be rendered with a radius equals to
	 * their actual radius multiplied by this ratio.
	 */
	public static final String KEY_SPOT_RADIUS_RATIO = "SpotRadiusRatio";
	
	public static final String KEY_SPOT_SIZE = "SpotSize";

	/**
	 * The default color for spots.
	 */
//	public static final Color DEFAULT_SPOT_COLOR = new Color( 0.7f, 0, 0.7f, 0.5f );
	public static final Color DEFAULT_SPOT_COLOR = new Color( 1.0f, 0.2f, 1.0f, 0.75f );
	
	public static final Color DEFAULT_INACTIVE_SPOT_COLOR = new Color( 0.3f, 0, 1.0f, 0.75f );
	public static final Color DEFAULT_UNMATCHED_SPOT_COLOR = new Color( 0.3f, 0, 1.0f, 0.75f );
	
	public static final Stroke NORMAL_STROKE = new BasicStroke( 1.0f );
	public static final Stroke HIGHLIGHT_STROKE = new BasicStroke( 2.0f );
	
	/**
	 * The default color for highlighting.
	 */
	public static final Color DEFAULT_HIGHLIGHT_COLOR = new Color( 1f, 0.5f, 1f );

	public static final double DEFAULT_SPOT_SIZE = 4;
	
	protected Map< String, Object > displaySettings;
	

	protected Color currentSpotColor;
	protected Color currentInactiveSpotColor;
	public double currentSpotSize;
	
	
	public BigWarpViewerSettings(){
		createDisplaySettings();
		
		currentSpotColor = DEFAULT_SPOT_COLOR;
		currentSpotSize = DEFAULT_SPOT_SIZE;
		currentInactiveSpotColor = new Color( 
				currentSpotColor.getRed() / 2, 
				currentSpotColor.getGreen() / 2,
				currentSpotColor.getBlue() / 2,
				currentSpotColor.getAlpha() );
	}
	
	protected Map< String, Object > createDisplaySettings(  )
	{
		displaySettings = new HashMap< String, Object >();
		displaySettings.put( KEY_SPOT_SIZE, DEFAULT_SPOT_SIZE );
		displaySettings.put( KEY_COLOR, DEFAULT_SPOT_COLOR );
		displaySettings.put( KEY_INACTIVE_COLOR, DEFAULT_INACTIVE_SPOT_COLOR );
		displaySettings.put( KEY_SPOTS_VISIBLE, true );
		displaySettings.put( KEY_DISPLAY_SPOT_NAMES, false );
		displaySettings.put( KEY_SPOT_RADIUS_RATIO, 1.0d );
		return displaySettings;
	}
	
	
	public Boolean areLandmarksVisible(){
		return (Boolean)displaySettings.get( KEY_SPOTS_VISIBLE );
	}
	
	public void togglePointsVisible(){
		displaySettings.put( KEY_SPOTS_VISIBLE, 
				!((Boolean)displaySettings.get( KEY_SPOTS_VISIBLE )).booleanValue());
		
		//System.out.println(((Boolean)displaySettings.get( KEY_DISPLAY_SPOT_NAMES )));
	}
	
	public void setSpotColor( Color c )
	{
		this.currentSpotColor = c;
		currentInactiveSpotColor = new Color( 
				currentSpotColor.getRed() / 2, 
				currentSpotColor.getGreen() / 2,
				currentSpotColor.getBlue() / 2,
				currentSpotColor.getAlpha() );
	}
	
	public Color getSpotColor()
	{
		return currentSpotColor;
	}
	
	public Color getInactiveSpotColor()
	{
		return currentInactiveSpotColor;
	}
	
	public void setSpotSize( double size )
	{
		this.currentSpotSize = size;
	}
	
	public double getSpotSize()
	{
		return currentSpotSize;
	}
	
	public Boolean areNamesVisible(){
		return (Boolean)displaySettings.get( KEY_DISPLAY_SPOT_NAMES );
	}
	
	public void toggleNamesVisible(){
		displaySettings.put( KEY_DISPLAY_SPOT_NAMES, 
				!((Boolean)displaySettings.get( KEY_DISPLAY_SPOT_NAMES )).booleanValue());
	}

	public Object get( String key ){
		return displaySettings.get(key);
	}

	
}
