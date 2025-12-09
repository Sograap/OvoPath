/*
 * OvoPath project  is under   GNU GENERAL PUBLIC LICENSE, GPL-3.0 license
 * Please check https://github.com/Sograap/OvoPath  if you have any doubts about licensing
 *
 * Watershed cell clusters
 * 
 * This script applies the watershed algorithm to the merged clusters to split them
 * 
 * Parameters:
 * 
 * inputClassificationObjectsString: classification of the objects to split
 * outputClassificationString: classification for the output objects
 * 
 * removeInputObjects: true/false. if you want to remove or keep the original objects
 * 
 * 
 * 
 */



//Set the input and output classifcations
inputClassificationObjectsString = "Other"
outputClassificationString = null

//Remove Input Objects
removeInputObjects = true






////////////////////////////////////////////
////////DO NOT TOUCH FROM HERE//////////////
////////////////////////////////////////////



runWatershedPostprocessingAnnotations(inputClassificationObjectsString,outputClassificationString,removeInputObjects)

def runWatershedPostprocessingAnnotations(inputClassificationString, outputClassificationString,removeInputObjectsm) {
   
   
   qupathAnnotationsWatersheed =[]
   
   annotationsToPostprocess = getAnnotationObjects().findAll {
      it.getPathClass() == getPathClass(inputClassificationString) 
   }
   
   if(annotationsToPostprocess.size()== 0 ) {
        throw new RuntimeException("ERROR: There is no parent annotations, please check the classification")
    }
  
   
   
   //apply the watershed
   annotationsToPostprocess.forEach {
      
      whatershedROI = watershedSingleAnnotationIJ(it,inputClassificationString )
      
      //split the rois and convert to annotations in QuPath
      whatershedROI.forEach {
          roisSplited = RoiTools.splitROI(it)
          
          roisSplited.forEach {
              annotations = PathObjects.createAnnotationObject(it,getPathClass(outputClassificationString))
              qupathAnnotationsWatersheed << annotations
          }
          
      }
      
   }
   
   
 
   
   
   addObjects(qupathAnnotationsWatersheed)
   
   //check if i ahve a bolean, and if it is true remove the objects
   if(removeInputObjects instanceof Boolean && removeInputObjects) {
      removeObjects(annotationsToPostprocess,true) 
   }
   
   
   println "Watershed Postprocesing Finished!"
   
}




import qupath.imagej.tools.IJTools
import qupath.lib.images.servers.LabeledImageServer
import qupath.lib.regions.RegionRequest
import ij.plugin.filter.EDM
import qupath.imagej.processing.RoiLabeling




def watershedSingleAnnotationIJ (annotation,inputClassificationString) {
   
   def imageData = getCurrentImageData()
   def plane = ImagePlane.getPlane(0, 0)
   def downsample = 1
   
   //create the label server to export the label in IJ. IMPORTANT: the label here has a value of 1.

   def labelServer = new LabeledImageServer.Builder(imageData)
    .backgroundLabel(0, ColorTools.BLACK) // Background label
    .addLabel(inputClassificationString, 1)// Add label for annotations
    .downsample(downsample)          // Define resolution
    .multichannelOutput(false)       // Single-channel output
    .build()
   
   //coords to place the rois back in QuPath
   xCoord = annotation.getROI().getBoundsX()
   yCoord = annotation.getROI().getBoundsY()
   
   //request the labelled region
   def regionRequest = RegionRequest.createInstance(labelServer.getPath(), downsample, annotation.getROI())
   
   //transform the region to imgPlus for IJ processing
   def pathImgPlus = IJTools.convertToImagePlus(labelServer, regionRequest)
   def imgPlus = pathImgPlus.getImage()
   
   //get the processor and do the Euclidian Distance Map and watershed
   def ip = imgPlus.getProcessor()
   def edm = new EDM()
   edm.toWatershed(ip)
   
   //now we can convert the watersheed result to ROIs. We set nclass to 1, since the label is 1
   def nclass= 1
   def roisIJ = RoiLabeling.labelsToConnectedROIs(ip, nclass)
   
   def roisQuPath = roisIJ.collect {
   if (it == null)
          return
           //here we put the coords for translation with a minus before 
    return IJTools.convertToROI(it, -xCoord , -yCoord, downsample, plane);
   }

   
   //Close the imgPlus and return the roi, althoug splited stil is one object.
   //will do the postprocessing to the final split later.
   imgPlus.close()
   
   
   return roisQuPath
}
