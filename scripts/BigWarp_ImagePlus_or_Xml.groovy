#@ File(label="Moving xml", required=false, style="extensions:xml" ) movingXml
#@ File(label="Target xml", required=false, style="extensions:xml" ) targetXml
#@ ImagePlus(label="Moving image", autofill=true, required=false, description="Only used if moving xml is empty" ) movingIp
#@ ImagePlus(label="Target image", autofill=true, required=false, description="Only used if target xml is empty" ) targetIp
#@ File(label="Landmark file", required=false, style="extensions:csv") landmarksFile

println( 'moving ip ' + movingIp )
println( 'target ip ' + targetIp )

println( 'moving xml ' + movingXml )
println( 'target xml ' + targetXml )

import java.lang.Exception;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import bigwarp.landmarks.LandmarkTableModel;
import bdv.export.ProgressWriterConsole;

data = null;
if( movingXml != null && targetXml != null  ){
    data = BigWarpInit.createBigWarpDataFromXML( movingXml.getAbsolutePath(), targetXml.getAbsolutePath());
    println("xml xml")
} else if( movingXml != null ){
    data = BigWarpInit.createBigWarpDataFromXMLImagePlus( movingXml.getAbsolutePath(), targetIp );
    println("xml ip")
} else if( targetXml != null ) {
    data = BigWarpInit.createBigWarpDataFromImagePlus( movingXml.getAbsolutePath(), targetIp );
    println("ip xml")
} else{
    data = BigWarpInit.createBigWarpDataFromImages( movingIp, targetIp);
    println("ip ip")
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
