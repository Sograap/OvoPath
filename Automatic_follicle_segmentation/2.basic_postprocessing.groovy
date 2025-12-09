/*
 * OvoPath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/Sograap/OvoPath  if you have any doubts about licensing
 *
 * POST-PROCESSING SEMANTIC SEGMENTATION 
 * 
 * This script does a small post processing to the load mask from the neural network inference
 * 
 * Parameters:
 * 
 * @Parameter "outputClassName": String with the output classification. Example: "Cluster".
 *
 * @Parameter "fillHoles": boolean, if true, fill the holes inside the hole annotation
 *
 * @Paramenter "minArea": double objects with less area in um2 are deleted
 * 
 * @Parameter "maxEccentricity": double objects with more eccentricity (more elongated) are deleted
 * 
 * @Parameter "minSolidity": double objects with less solidity that the value are deleted
 * 
 * 
 * 
 */


 //To Complete
def outputClassificationString = "null"
def fillHoles = true
def minArea = 500 
def maxExcentricity = 0.9 
def minSolidity = 0.3 






////////////////////////////////////////////
////////DO NOT TOUCH FROM HERE//////////////
////////////////////////////////////////////




runPostProcessingSegmentation(outputClassificationString,fillHoles,minArea,maxExcentricity,minSolidity)

def runPostProcessingSegmentation(outputClassificationString,fillHoles,minArea,maxExcentricity,minSolidity) {
    
    def server = getCurrentServer()
    def cal = server.getPixelCalibration()
    double pixelWidth = cal.getPixelWidthMicrons()
    double pixelHeight = cal.getPixelHeightMicrons()
   
    toProcess = getAnnotationObjects().findAll {
       it.getPathClass().toString() == outputClassificationString 
    }
    
    if(toProcess.size()== 0 ) {
        throw new RuntimeException("ERROR: There is no output annotations, please check the classification")
    }
    
    
    if(fillHoles == true) {
       
       selectObjects(toProcess)
       runPlugin('qupath.lib.plugins.objects.FillAnnotationHolesPlugin', '{}')
       selectObjects(toProcess)
       runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')
       
    }else if(fillHoles == false) {
        
       selectObjects(toProcess)
       runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')
        
    }
    
    
    //now compute the features for each object using QuPath tools
    
    toProcessSplit = getAnnotationObjects().findAll {
       it.getPathClass().toString() == outputClassificationString 
    }
    
    selectObjects(toProcessSplit)
    addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER")
    
    //compute the eccentricity, usefull when you have elongated shapes, usually in borders.
    computeEccentricity(toProcessSplit)
    
    
    //now remove the objects filtered by area and eccentricity and solidity
     

    def toDelete = getAnnotationObjects().findAll { it.getPathClass() == getPathClass(outputClassificationString) && 
    (it.getROI().getScaledArea(pixelWidth, pixelHeight) < minArea ||
    it.measurements["Eccentricity"] > maxExcentricity ||
    it.measurements["Solidity"] < minSolidity )
    }
    
    
    removeObjects(toDelete, true)
    
   
   
   
}



def computeEccentricity(objects) {
   
   objects.forEach {
      majAxis = it.measurements['Max diameter µm']
      minAxis=it.measurements['Min diameter µm']
      eccentricity = 2*Math.sqrt(((majAxis * majAxis * 0.25) - (minAxis * minAxis * 0.25))) / majAxis
      it.getMeasurementList().put('Eccentricity',eccentricity )
      
   }
}
