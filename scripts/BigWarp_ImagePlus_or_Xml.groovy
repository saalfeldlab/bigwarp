#@ String (visibility=MESSAGE, value="<html>Run bigwarp with bigdataviewer xml-h5 files and/or images.<p> <p>XML files take precedence over open images.<p>&nbsp;&nbsp;i.e., if an xml file is given for either the moving (or target) source,<p>&nbsp;&nbsp;then the image that is specified for the moving (target) source will be ignored. </html>") DOCMSG
#@ File(label="Moving xml", required=false, style="extensions:xml" ) movingXml
#@ File(label="Target xml", required=false, style="extensions:xml" ) targetXml
#@ ImagePlus(label="Moving image", autofill=true, required=false, description="Only used if moving xml is empty" ) movingIp
#@ ImagePlus(label="Target image", autofill=true, required=false, description="Only used if target xml is empty" ) targetIp
#@ File(label="Landmark file", required=false, style="extensions:csv") landmarksFile

import java.lang.Exception;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;
import bdv.export.ProgressWriterConsole;

data = null;
if( movingXml != null && targetXml != null  ){
    data = BigWarpInit.createBigWarpDataFromXML( movingXml.getAbsolutePath(), targetXml.getAbsolutePath());
} else if( movingXml != null ){
    data = BigWarpInit.createBigWarpDataFromXMLImagePlus( movingXml.getAbsolutePath(), targetIp );
} else if( targetXml != null ) {
    data = BigWarpInit.createBigWarpDataFromImagePlus( movingXml.getAbsolutePath(), targetIp );
} else{
    data = BigWarpInit.createBigWarpDataFromImages( movingIp, targetIp);
}


try {

    bw = new BigWarp( data, "bigwarp", new ProgressWriterConsole() );

    /* load the landmark points if there are any */
    if ( landmarksFile != null )
        bw.getLandmarkPanel().getTableModel().load( landmarksFile );

}catch(Exception e)
{
    e.printStackTrace();
}