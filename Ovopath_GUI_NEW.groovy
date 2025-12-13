/* OvoPath GUI. 
 * This GUI is created with the paper: Digital analysis of ovarian tissue: generating a standardized method of follicle analysis. 
 * Requires QuPath Version 0.5.0 or above
 *
 * @authors Isaac Vieco-Martí & @Sofia Granados-Aparici
 *  
 */


import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.control.Tab
import javafx.scene.layout.GridPane
import javafx.stage.Stage
import qupath.lib.gui.QuPathGUI
import qupath.fx.dialogs.Dialogs
import qupath.lib.gui.tools.GuiTools
import qupath.fx.utils.GridPaneUtils
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjectTools
import qupath.lib.objects.PathObjects
import qupath.lib.objects.classes.PathClass
import qupath.lib.roi.GeometryTools
import javafx.collections.FXCollections
import qupath.fx.utils.FXUtils
import qupath.lib.plugins.parameters.ParameterList;
import qupath.process.gui.commands.ml.ClassificationResolution;
import qupath.lib.images.ImageData;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Spinner;
import ij.IJ;
import qupath.opencv.tools.MultiscaleFeatures.MultiscaleFeature;
import javafx.scene.control.TextField
import javafx.scene.control.ScrollPane


//Platform.runLater { buildStage().show()}

def customId = "autothreshold_tab"
Platform.runLater {
    gui = QuPathGUI.getInstance()
    panelTabs = gui.getAnalysisTabPane().getTabs()
    RemoveTab(panelTabs,customId)
    
    def pane = buildPane()
    Tab newTab = new Tab("Follicle Analysis", pane)
    newTab.setId(customId)
    newTab.setTooltip(new Tooltip("Multiplex class selection"))
    panelTabs.add(newTab)
    //This selects the new tab
    gui.getAnalysisTabPane().getSelectionModel().select(newTab);    
}

// Remove all the additions made to the Analysis panel, based on the id above
def RemoveTab(panelTabs, id) {
    while(1) {
        hasElements = false
        for (var tabItem : panelTabs) {
            if (tabItem.getId() == id) {
                panelTabs.remove(tabItem)
                hasElements = true
                break
            }
        }
        if (!hasElements) break
    }
}


ScrollPane buildPane() {
    def qupath = QuPathGUI.getInstance()
    
    def pane = new GridPane()
    
    ////////////////////////////
    // Script 1/////////////////
    ////////////////////////////
   

    int row = 0
    row++
    message1 = new Label(" ATTENTION: Accurately segment follicle areas")
    
    message1.setStyle("-fx-underline: true") 
    pane.add(message1, 0,row, 2, 1)
    
    //button
    row++
    row++
    
    
    step1 = new Label("Script 1: Assign follicle ID")
    step1.setStyle("-fx-font-weight: bold")
    pane.add(step1, 0,row, 1, 1)
    
    //button
    row++
    row++
    
    def btnStep1 = new Button("Run Follicle ID")
    btnStep1.setTooltip(new Tooltip("This gives an ID for each follicle"))
    pane.add(btnStep1,0 , row, 2, 1)
    
     btnStep1.setOnAction {e ->
         runScript1()
         Dialogs.showInfoNotification("OvoPath:", " Assign follicle ID Finished!")
         
     }
    
    
    
    
    ////////////////////////////
    // Script 2/////////////////
    ////////////////////////////
    
    row++
    row++
    row++
    
    step2 = new Label("Script 2: Detect GC Nuclei (Stardist)")
    pane.add(step2, 0,row, 1, 1)
    step2.setStyle("-fx-font-weight: bold")
    
    row++
    row++
    
    //path to Stardist
    pathText = new TextField()
    pathText.setPromptText("Path to Stardist .pb file")
    pane.add(pathText, 0, row, 2, 1)
    
    
    //Prob Threshold
    def probThSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1.0, 0.1, 0.05));
    probThSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(probThSpinner.getEditor(), true);
    probThSpinner.setTooltip(new Tooltip("Set a threshold for Stardist detections"))
    def labelProbThSpinner = new Label("Probability TH Detection Stardist")
    
    row++
    pane.add(labelProbThSpinner, 0, row, 1, 1)
    pane.add(probThSpinner, 1, row, 1, 1)
       
    
    //Resolution Spinner
    def resolutionSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1.0, 0.25, 0.01));
    resolutionSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(resolutionSpinner.getEditor(), true);
    resolutionSpinner.setTooltip(new Tooltip("Set the resolution to be used by Stardist"))
    def labelResolution = new Label("Image Resolution Stardist")
    
    row++
    pane.add(labelResolution, 0, row, 1, 1)
    pane.add(resolutionSpinner, 1, row, 1, 1)
    
    row++
    row++
    
    def btnStardist = new Button("Run Detect GC Nuclei")
    btnStardist.setTooltip(new Tooltip("Run Detect GC Nuclei"))
    pane.add(btnStardist,0 , row, 2, 1)
    
    btnStardist.setOnAction {e ->
    
         runStardistScript(pathText.getText(),probThSpinner.getValue(),resolutionSpinner.getValue()  )
         Dialogs.showInfoNotification("OvoPath:", " Stardist inference Finished!")
     }
    
    

    
    ////////////////////////////
    // Script 3/////////////////
    ////////////////////////////
    row++
    row++
    row++
    
    step3 = new Label("Script 3: Clean and assign GC")
    pane.add(step3, 0,row, 1, 1)
    step3.setStyle("-fx-font-weight: bold")
    
    //maxDiameter
    def maxDiameterSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10, 4, 0.1));
    maxDiameterSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxDiameterSpinner.getEditor(), true);
    maxDiameterSpinner.setTooltip(new Tooltip("Maximum Nuclei Diameter"))
    def labelmaxDiameterSpinner = new Label("Maximum Nuclei Diameter (microns)")
    
    row++
    pane.add(labelmaxDiameterSpinner, 0, row, 1, 1)
    pane.add(maxDiameterSpinner, 1, row, 1, 1)
    
    
    //minArea
    def minAreaSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10000, 5.6, 0.5));
    minAreaSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(minAreaSpinner.getEditor(), true);
    minAreaSpinner.setTooltip(new Tooltip("Minimum Nuclei Size in microns"))
    def labelminArea = new Label("Minimum Nuclei Area (microns^2)")
    
    row++
    pane.add(labelminArea, 0, row, 1, 1)
    pane.add(minAreaSpinner, 1, row, 1, 1)
    
     
    //max Area
    def maxAreaSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10000, 100, 0.5));
    maxAreaSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxAreaSpinner.getEditor(), true);
    maxAreaSpinner.setTooltip(new Tooltip("Maximum Nuclei Size in microns"))
    def labelMaxArea = new Label("Maximum Nuclei Area (microns^2)")
    
    row++
    pane.add(labelMaxArea, 0, row, 1, 1)
    pane.add(maxAreaSpinner, 1, row, 1, 1)
    
    //minimum hematoxylin TH
    def hemThSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10, 0.36, 0.01));
    hemThSpinner.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(hemThSpinner.getEditor(), true);
    hemThSpinner.setTooltip(new Tooltip("Minimum mean intensity of Hematoxylin Stain to be considered"))
    def labelHemTh = new Label("Minimum Mean Hematoxylin")
    
    row++
    pane.add(labelHemTh, 0, row, 1, 1)
    pane.add(hemThSpinner, 1, row, 1, 1)
    
    row++
    row++
    def btnClean = new Button("Run Clean and assign GC parameters")
    btnClean.setTooltip(new Tooltip("Remove undesired nuclei and assign GC parameters to parent follicle"))
    pane.add(btnClean,0 , row, 2, 1)
    
    btnClean.setOnAction {e ->
    
         runScript3(maxDiameterSpinner.getValue(), minAreaSpinner.getValue(),maxAreaSpinner.getValue(),hemThSpinner.getValue()  )
         Dialogs.showInfoNotification("OvoPath:", " Clean and assign GC Finished!")
     }
    
    
    
    ////////////////////////
    ////MESSAGE 2//////////
    //////////////////////
    row++
    row++
    
    //message2 = new Label(" ATTENTION: Accurately segment oocyte areas manually \n(leave without annotation if oocyte is not clear)\n Or adjust manually the automatic detection")
    //message2.setStyle("-fx-underline: true") 
    //pane.add(message2, 0,row, 2, 1)
    
    //button
    //row++
    
    
    ////////////////////////////
    // Script 4/////////////////
    ////////////////////////////
   
    row++
    row++
    
    step4 = new Label("Script 4: Add oocyte data")
    step4.setStyle("-fx-font-weight: bold")
    pane.add(step4, 0,row, 1, 1)
    
    row++
    row++
    
    ////////////////////////
    ////MESSAGE Autooocyte//////////
    //////////////////////
    
    oocyteMessage = new Label("NEW! : Automatic Oocyte Detection")
    oocyteMessage.setStyle("-fx-underline: true") 
    pane.add(oocyteMessage, 0,row, 2, 1)
    
    //Number of cells that must be at the centre to try to identify apical cuts of the oocytes
    
    def maxCellsCentre = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 10, 1));
    maxCellsCentre.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxCellsCentre.getEditor(), true);
    maxCellsCentre.setTooltip(new Tooltip("Max Cells in the centre to be considered apical cut"))
    def labelMaxCellsCentre= new Label("Minimum Detections in Center if Apical Cut")
    
    row++
    pane.add(labelMaxCellsCentre, 0, row, 1, 1)
    pane.add(maxCellsCentre, 1, row, 1, 1)
    
    row++
    row++
    
    def btnAddOocyte = new Button("Automatic Oocyte Detection")
    btnAddOocyte.setTooltip(new Tooltip("This estimates the Oocyte"))
    pane.add(btnAddOocyte,0 , row, 2, 1)
    
    
    btnAddOocyte.setOnAction {
       
       automaticOocyteSegmentation(maxCellsCentre.getValue())
       Dialogs.showInfoNotification("OvoPath:", " Automatic Oocyte Detection Finished!")
       
       
    }
    
    
    ////////////////////////
    ////MESSAGE 2//////////
    //////////////////////
    row++
    row++ 
    row++
    
    message2 = new Label("ATTENTION: Accurately segment oocyte areas manually \n(leave without annotation if oocyte is not clear)\n Or adjust manually the automatic detection")
    message2.setStyle("-fx-underline: true") 
    pane.add(message2, 0,row, 2, 1)
    
    //button
    row++
    row++
    
    def btnStep4 = new Button("Run Add Oocyte Data")
    btnStep1.setTooltip(new Tooltip("This puts the data to the oocyte"))
    pane.add(btnStep4,0 , row, 2, 1)
    
    btnStep4.setOnAction {e ->
    
         runScript4()
         Dialogs.showInfoNotification("OvoPath:", " Oocyte data added!")
     }
    
    
    
    
    
    ////////////////////////////
    // Script 5/////////////////
    ////////////////////////////
    
    row++
    row++
    row++
    
    step5 = new Label("Script 5: Assign follicle stages")
    step5.setStyle("-fx-font-weight: bold")
    pane.add(step5, 0,row, 1, 1)
    
    
    row++
    row++
    
    //FILTER 1:
    step5Filter1 = new Label("Primordial Follicle Parameters:")
    step5Filter1.setStyle("-fx-underline: true") 
    pane.add(step5Filter1, 0,row, 1, 1)
    
    //FILTER 1: primordial eccentricity
    def minPrimordialEccentricity = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.86, 0.01));
    minPrimordialEccentricity.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(minPrimordialEccentricity.getEditor(), true);
    minPrimordialEccentricity.setTooltip(new Tooltip("Minimum flat GC Eccentricity"))
    def labelMinPrimordialEccentricity= new Label("Minimum flat GC Eccentricity")
    
    row++
    pane.add(labelMinPrimordialEccentricity, 0, row, 1, 1)
    pane.add(minPrimordialEccentricity, 1, row, 1, 1)
    
    //FILTER 1:granulosa cells
    def maxGranulosaCells = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 30, 10, 1));
    maxGranulosaCells.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxGranulosaCells.getEditor(), true);
    maxGranulosaCells.setTooltip(new Tooltip("Maximum number of Granulosa Cells"))
    def labelMaxGranulosaCells= new Label("Maximum GC Number")
    
    row++
    pane.add(labelMaxGranulosaCells, 0, row, 1, 1)
    pane.add(maxGranulosaCells, 1, row, 1, 1)
    
    def maxCuboidGCNumber = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10, 2, 1));
    maxCuboidGCNumber.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxCuboidGCNumber.getEditor(), true);
    maxCuboidGCNumber.setTooltip(new Tooltip("Maximum cuboidal GC Number"))
    def labelmaxCuboidGCNumber= new Label("Maximum cuboidal GC Number")
    
    row++
    pane.add(labelmaxCuboidGCNumber, 0, row, 1, 1)
    pane.add(maxCuboidGCNumber, 1, row, 1, 1)
    
    row++
    row++
     //FILTER 3:
    step5Filter3 = new Label("Transitional Follicle Parameters:")
    step5Filter3.setStyle("-fx-underline: true") 
    pane.add(step5Filter3, 0,row, 1, 1)
    
    //FILTER 3: Transitional eccentricity
    def maxTransitionalGCEccentricity = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.79, 0.01));
    maxTransitionalGCEccentricity.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxTransitionalGCEccentricity.getEditor(), true);
    maxTransitionalGCEccentricity.setTooltip(new Tooltip("Maximum Median GC Eccentricity"))
    def labelMaxTransitionalGCEccentricity= new Label("Maximum Median GC Eccentricity")
    
    row++
    pane.add(labelMaxTransitionalGCEccentricity, 0, row, 1, 1)
    pane.add(maxTransitionalGCEccentricity, 1, row, 1, 1)
    
    row++
    row++
    
    //FILTER 2:
    
    step5Filter2 = new Label("Primary Follicle Parameters:")
    step5Filter2.setStyle("-fx-underline: true") 
    pane.add(step5Filter2, 0,row, 1, 1)
    
    //FILTER 2: primary eccentricity
    def maxPrimaryEccentricity = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.85, 0.01));
    maxPrimaryEccentricity.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxPrimaryEccentricity.getEditor(), true);
    maxPrimaryEccentricity.setTooltip(new Tooltip("Maximum median GC Eccentricity"))
    def labelMaxPrimordialEccentricity= new Label("Maximum median GC Eccentricity")
    
    row++
    pane.add(labelMaxPrimordialEccentricity, 0, row, 1, 1)
    pane.add(maxPrimaryEccentricity, 1, row, 1, 1)
    
    //FILTER 2: min granulosa primary 
    def minGranulosaCellsPrimary = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 30, 15, 1));
    minGranulosaCellsPrimary.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(minGranulosaCellsPrimary.getEditor(), true);
    minGranulosaCellsPrimary.setTooltip(new Tooltip("Maximum number of Granulosa Cells"))
    def labelMinGranulosaCellsPrimary= new Label("Minimum GC Number")
    
    row++
    pane.add(labelMinGranulosaCellsPrimary, 0, row, 1, 1)
    pane.add(minGranulosaCellsPrimary, 1, row, 1, 1)
    
    
    //FILTER 2: max granulosa primary 
    def maxGranulosaCellsPrimary = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 60, 30, 1));
    maxGranulosaCellsPrimary.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxGranulosaCellsPrimary.getEditor(), true);
    maxGranulosaCellsPrimary.setTooltip(new Tooltip("Maximum number of Granulosa Cells"))
    def labelMaxGranulosaCellsPrimary= new Label("Maximum GC Number")
    
    row++
    pane.add(labelMaxGranulosaCellsPrimary, 0, row, 1, 1)
    pane.add(maxGranulosaCellsPrimary, 1, row, 1, 1)
    
    row++
    row++
    
    
    
    //FILTER 3:granulosa cells
    //def maxGranulosaCellsTransitional = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 20, 10, 1));
    //maxGranulosaCellsTransitional.setEditable(true);
    //FXUtils.restrictTextFieldInputToNumber(maxGranulosaCellsTransitional.getEditor(), true);
    //maxGranulosaCellsTransitional.setTooltip(new Tooltip("Maximum number of Granulosa Cells"))
    //def labelMaxGranulosaCellsTransitional= new Label("Maximum GC Number")
    
    //row++
   //pane.add(labelMaxGranulosaCellsTransitional, 0, row, 1, 1)
    //pane.add(maxGranulosaCellsTransitional, 1, row, 1, 1)
    
    
    //FILTER 4:
    row++
    row++
    step5Filter4 = new Label("Secondary Follicle Parameters:")
    step5Filter4.setStyle("-fx-underline: true") 
    pane.add(step5Filter4, 0,row, 1, 1)
    
    def minNumberCellsSecondary= new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 80, 30, 1));
    minNumberCellsSecondary.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(minNumberCellsSecondary.getEditor(), true);
    minNumberCellsSecondary.setTooltip(new Tooltip("Minimum GC number"))
    def labelMinNumberCellsSecondary= new Label("Minimum GC number")
    
    row++
    pane.add(labelMinNumberCellsSecondary, 0, row, 1, 1)
    pane.add(minNumberCellsSecondary, 1, row, 1, 1)
    
    
    def minFollicleAreaSecondary= new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100000, 3500, 1));
    minFollicleAreaSecondary.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(minFollicleAreaSecondary.getEditor(), true);
    minFollicleAreaSecondary.setTooltip(new Tooltip("Minimum Follicle Area (microns^2)"))
    def labelMinFollicleAreaSecondary= new Label("Minimum Follicle Area (microns^2)")
    
    row++
    pane.add(labelMinFollicleAreaSecondary, 0, row, 1, 1)
    pane.add(minFollicleAreaSecondary, 1, row, 1, 1)
    
    
    def maxFollicleAreaSecondary= new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000000, 5000, 1));
    maxFollicleAreaSecondary.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(maxFollicleAreaSecondary.getEditor(), true);
    maxFollicleAreaSecondary.setTooltip(new Tooltip("Maximum Follicle Area (microns^2)"))
    def labelMaxFollicleAreaSecondary= new Label("Maximum Follicle Area (microns^2)")
    
    row++
    pane.add(labelMaxFollicleAreaSecondary, 0, row, 1, 1)
    pane.add(maxFollicleAreaSecondary, 1, row, 1, 1)
    
    
    
    
    
    
     //FILTER 5:
    row++
    row++
    step5Filter5 = new Label("Antral Follicle Parameters:")
    step5Filter5.setStyle("-fx-underline: true") 
    pane.add(step5Filter5, 0,row, 1, 1)
    
    def minNumberCellsAntral= new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 70, 1));
    minNumberCellsAntral.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(minNumberCellsAntral.getEditor(), true);
    minNumberCellsAntral.setTooltip(new Tooltip("Minimum GC number"))
    def labelMinNumberCellsAntral= new Label("Minimum GC number")
    
    row++
    pane.add(labelMinNumberCellsAntral, 0, row, 1, 1)
    pane.add(minNumberCellsAntral, 1, row, 1, 1)
    
    
    
    def minFollicleAreaAntral= new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100000, 5000, 1));
    minFollicleAreaAntral.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(minFollicleAreaAntral.getEditor(), true);
    minFollicleAreaAntral.setTooltip(new Tooltip("Minimum Follicle Area (microns^2)"))
    def labelMinFollicleAreaAntral= new Label("Minimum Follicle Area (microns^2)")
    
    row++
    pane.add(labelMinFollicleAreaAntral, 0, row, 1, 1)
    pane.add(minFollicleAreaAntral, 1, row, 1, 1)
    
    
    
    
     //FILTER 5:
    row++
    row++
    step5Filter6 = new Label("Ignore Follicle Parameters:")
    step5Filter6.setStyle("-fx-underline: true") 
    pane.add(step5Filter6, 0,row, 1, 1)
    
    def ignoreFollicle= new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 60, 1));
    ignoreFollicle.setEditable(true);
    FXUtils.restrictTextFieldInputToNumber(ignoreFollicle.getEditor(), true);
    ignoreFollicle.setTooltip(new Tooltip("Maximum Follicle GC ratio"))
    def labelIgnoreFollicle= new Label("Maximum Follicle GC ratio")
    
    row++
    pane.add(labelIgnoreFollicle, 0, row, 1, 1)
    pane.add(ignoreFollicle, 1, row, 1, 1)
    
    
    
    row++
    row++
    row++
    
    def btnAssignation = new Button("Run Follicle Classification")
    btnAssignation.setTooltip(new Tooltip("Run Follicle Classification with the provided filters"))
    pane.add(btnAssignation,0 , row, 2, 1)
    
    
    btnAssignation.setOnAction {e ->
    
         runScript5(maxGranulosaCells.getValue(),
         minPrimordialEccentricity.getValue(),
         maxCuboidGCNumber.getValue(),
         
         maxPrimaryEccentricity.getValue(),
         minGranulosaCellsPrimary.getValue(),
         maxGranulosaCellsPrimary.getValue(),
         maxTransitionalGCEccentricity.getValue(),
         minNumberCellsSecondary.getValue(),
         minFollicleAreaSecondary.getValue(),
         maxFollicleAreaSecondary.getValue(),
         minNumberCellsAntral.getValue(),
         minFollicleAreaAntral.getValue(),
         ignoreFollicle.getValue()   
         )
         Dialogs.showInfoNotification("OvoPath:", " Assign follicle stages Finished!")
         
     }
    
    //runScript5(maxNumberGranulosaCellsPrimordial,minFlatGCEccentricity,numCuboidCells,  maxprimaryEccentricity,minNumberGranulosaCellsPrimaries,maxNumberGranulosaCellsPrimaries,minNumberGranulosaCellssecondary,minNumberGranulosaCellsantral,minfollicleareaantral,maxcellnumberfolliclearearatioignore)
    

   ////////////////////////////
    // Script 6/////////////////
    ////////////////////////////
    row++
    row++
    row++
    
    step6 = new Label("Script 6: Extract data")
    step6.setStyle("-fx-font-weight: bold")
    pane.add(step6, 0,row, 1, 1)
    
    //button
    row++
    row++
    
    def btnStep6 = new Button("Run Extract data")
    btnStep1.setTooltip(new Tooltip("This exports follicle and GC annotation data as txt file"))
    pane.add(btnStep6,0 , row, 2, 1)
    
    btnStep6.setOnAction {e ->
        
        saveData()
        Dialogs.showInfoNotification("OvoPath:", " Save Data Finished!")
     }
    
    
    
       
    pane.setHgap(10)
    pane.setVgap(5)
    pane.setPadding(new Insets(5))
    GridPaneUtils.setToExpandGridPaneWidth(message1,
    message2,
    btnStep1,
    pathText,
    probThSpinner,
    resolutionSpinner,
    btnStardist,
    maxDiameterSpinner,
    minAreaSpinner,
    maxAreaSpinner,
    hemThSpinner,
    btnClean,
    maxCellsCentre,
    btnAddOocyte,
    btnStep4,
    minPrimordialEccentricity,
    maxGranulosaCells,
    maxPrimaryEccentricity,
    minGranulosaCellsPrimary,
    maxGranulosaCellsPrimary,
    maxCuboidGCNumber,
    maxTransitionalGCEccentricity,
    minNumberCellsSecondary,
    minFollicleAreaSecondary,
    maxFollicleAreaSecondary,
    minNumberCellsAntral,
    minFollicleAreaAntral,
    ignoreFollicle,
    btnAssignation,
    btnStep6)
    
    ScrollPane sp = new ScrollPane();
    sp.setFitToWidth(true);
    sp.setContent(pane)
    
        return sp
}



/////////////////
//FUNCTIONS//////
/////////////////


def runScript1() {
   
   def oo = getAnnotationObjects().findAll { it.getPathClass() == null }
   
    oo.forEach {
          it.setPathClass(getPathClass('Follicle'))  
       } 
       
    
    def sorted = oo.toSorted{it.getROI().getBoundsY()}

    sorted.eachWithIndex{obj, i->
        obj.setName('F_'+i)
    
    /**This script saves follicle annotations**/

    //selectObjectsByClassification("Follicle")
    //def annot = getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Follicle") }
    
    def name1 = getProjectEntry().getImageName() + '.geojson'
    def path1 = buildFilePath(PROJECT_BASE_DIR, 'Follicle annotations')
    mkdirs(path1)
    path1 = buildFilePath(path1, name1)
    exportObjectsToGeoJson(sorted, path1, "FEATURE_COLLECTION") 
    print 'Follicle annotations exported to ' + path1
    }   
   
   
}



//runSctipt 2
import qupath.ext.stardist.StarDist2D

def runStardistScript(modelPath, detectionProbabilityTH, pixelSizeDetection) {
   
    var imageData = getCurrentImageData()
    var stains = imageData.getColorDeconvolutionStains()
    
    toSegment = getAnnotationObjects().findAll {
       it.getPathClass() == getPathClass("Follicle") 
    }
    
    
    def stardist = StarDist2D
        .builder(modelPath)
        .preprocess( // Extra preprocessing steps, applied sequentially
                 // Extract the first stain (indexing starts at 0)
            ImageOps.Filters.minimum(1),
        // Apply a small median filter (optional!)
        )
        .normalizePercentiles(1, 99) // Percentile normalization
        .threshold(detectionProbabilityTH)              // Probability (detection) threshold
        .pixelSize(pixelSizeDetection)              // Resolution for detection
        .cellExpansion(0)          // Approximate cells based upon nucleus expansion
        .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
        .build()
        
    if (toSegment.isEmpty()) {
       QP.getLogger().error("No parent objects are selected!")
       return
     }
        
    stardist.detectObjects(imageData, toSegment)
    stardist.close() // This can help clean up & regain memory
    println('Done!')
        
   
   
   
}



//runSctipt 3


def runScript3(maxDiameter,minArea, maxArea, minHem) {
   
   toDelete1 = getDetectionObjects().findAll {
        measurement(it, 'Max diameter µm') < maxDiameter ||
        measurement(it, 'Area µm^2') < minArea
     
        }

    removeObjects(toDelete1, true)
    println 'Small GCs removed'
    
    
    toDelete2 = getDetectionObjects().findAll {
            measurement(it, 'Area µm^2') > maxArea
            }

    removeObjects(toDelete2, true)
    println 'Big GCs removed'
    
    toDelete3 = getDetectionObjects().findAll {
        measurement(it, 'Hematoxylin: Mean') < minHem
        }

    removeObjects(toDelete3, true)
    println 'GCs with low hematoxylin staining removed'
    
    
    anno = getAnnotationObjects()
    
    


    anno.forEach {
        
       annoID = it.getName() 
       
       medianArea = []
       medianCircularity =[]
       medianminAxis =[]
       medianEccentricity=[]
   
       child = it.getChildObjects()
   
       if(child.size() == 0) {
       
           it.getMeasurementList().put('ChildMedianArea',0 )
           it.getMeasurementList().put('ChildMedianCircularity',0 ) 
           it.getMeasurementList().put('ChildMedianMinAxis',0 )
           it.getMeasurementList().put('ChildMedianEccentricity',0 )
      
       }else {
           child.forEach {
       
              //put the parent name
              it.setName(annoID)
              it.setPathClass(getPathClass('Granulosa Cell Nuclei'))
              
              Area = it.measurements['Area µm^2']
              Circularity = it.measurements['Circularity']
              majAxis = it.measurements['Max diameter µm']
              minAxis=it.measurements['Min diameter µm']
      
              eccentricity = 2*Math.sqrt(((majAxis * majAxis * 0.25) - (minAxis * minAxis * 0.25))) / majAxis
              it.getMeasurementList().put('eccentricity',eccentricity )
      
              medianArea<< Area
              medianCircularity<<Circularity
              medianminAxis<< minAxis
              medianEccentricity<<eccentricity
      
           }
   
           medianaArea =findMedian(medianArea)
           medianaCircularity = findMedian(medianCircularity)
           medianaminAxis= findMedian(medianminAxis)
           SDminAxis= findSD(medianminAxis)
           medianaEccentricity = findMedian(medianEccentricity) 
   
   
           print(medianaArea)
           print(medianaCircularity)
   
           it.getMeasurementList().put('ChildMedianArea',medianaArea )
           it.getMeasurementList().put('ChildMedianCircularity',medianaCircularity )
           it.getMeasurementList().put('ChildMedianMinAxis',medianaminAxis)
           it.getMeasurementList().put('ChildSDMinAxis',SDminAxis)
           it.getMeasurementList().put('ChildMedianEccentricity',medianaEccentricity )
        
           }
   
    }
    
    //store the data
    //selectObjectsByClassification("Granulosa Cell Nuclei")
    def toExport = getDetectionObjects().findAll {
       it.getPathClass() == getPathClass("Granulosa Cell Nuclei") 
    }
    
    def name1 = getProjectEntry().getImageName() + '.geojson'
    def path1 = buildFilePath(PROJECT_BASE_DIR, 'Granulosa cell detections')
    mkdirs(path1)
    path1 = buildFilePath(path1, name1)
    exportObjectsToGeoJson(toExport, path1, "FEATURE_COLLECTION") 
    print 'Granulosa cell detections exported to ' + path1
    
    
    
   
   
   
}

//Functions related with script 3


def findMedian(list) {
    def sortedList = list.sort()
    def size = sortedList.size()

    if (size % 2 == 1) {
        // If the list has an odd number of elements, return the middle element
        return sortedList[size / 2]
    } else {
        // If the list has an even number of elements, return the average of the two middle elements
        def middle1 = sortedList[size / 2 - 1]
        def middle2 = sortedList[size / 2]
        return (middle1 + middle2) / 2.0
    }
}


// Define a function to calculate standard deviation
def findSD(list) {
    double sum = 0
    double mean = list.sum() / list.size()
    list.each {
        sum += (it - mean) * (it - mean)
    }
    return Math.sqrt(sum / list.size())
}


//AUTOMATIC OOCYTE SEGMENTATION
import org.locationtech.jts.operation.distance.DistanceOp
import org.locationtech.jts.geom.util.GeometryCombiner

def automaticOocyteSegmentation(double maxCellsCenter) {

    // Get all follicle annotations
    def anno = getAnnotationObjects().findAll {
        it.getPathClass() == getPathClass("Follicle")
    }

    def plane = ImagePlane.getPlane(0, 0)

    def toRemove = []
    def oocytes = []

    anno.each { follicle ->
    
        follicleName = follicle.getName().toString()

        def dilationInsideGeom = createDilationSettingFactor(follicle, 0.25)

        def childs = follicle.getChildObjects()

        // remove childs intersecting dilation
        def toRemoveInternal = []
        def cellsNotTouchingGeom = []
        def cellsNotTouchingROI  = []

        int cellsTouching = 0

        childs.each { cell ->
            def geom = cell.getROI().getGeometry()

            if (geom.intersects(dilationInsideGeom)) {
                toRemoveInternal << cell
                cellsTouching++
            } else {
                cellsNotTouchingGeom << geom
                cellsNotTouchingROI  << cell.getROI()
            }
        }

        // No granulosa cells left
        if (cellsNotTouchingGeom.isEmpty()) {
            follicle.setPathClass(getPathClass("Ignore/Disclude"))
            def currentName = follicle.getName().toString()
            follicle.setName(currentName + "_No_Granulosa_Cell")
            return
        }

        // Too many cells touching the center: apical cut
        if (cellsTouching > maxCellsCenter) {
            follicle.setPathClass(getPathClass("Ignore/Disclude"))
            def currentName = follicle.getName().toString()
            follicle.setName(currentName + "_Apical_cut")
            return
        }

        // Accumulate cells to remove
        toRemoveInternal.each { cell ->
            toRemove << cell
        }

        // Compute distance from cells not touching to the dilation
        def distances = cellsNotTouchingGeom.collect { geom ->
            DistanceOp.distance(dilationInsideGeom, geom)
        }

        // Average distance
        def distMax = (distances.sum() / distances.size()).round()

        // Buffer this distance to the centre
        def oocyteExpandedGeom = dilationInsideGeom.buffer(distMax)
        def roiOocyteExpanded = GeometryTools.geometryToROI(oocyteExpandedGeom, plane)

        def subtractedROI = RoiTools.subtract(roiOocyteExpanded, cellsNotTouchingROI)

        if (subtractedROI != null) {
            def dets = PathObjects.createDetectionObject(subtractedROI, getPathClass("Oocyte"))
            dets.setName(follicleName)
            oocytes << dets
        }
    }

    // Final operations
    removeObjects(toRemove)
    addObjects(oocytes)
}


//Adjust dilation factor
def createDilationSettingFactor(parentObject, dilationFactor) {
   
   
   plane = parentObject.getROI().getImagePlane()
   
   result = firstDilation(parentObject, dilationFactor)
   
   finalGeomFeatures = adjustDilationTargetAreaLoop(result)
   
   geomInside = finalGeomFeatures[0]
   print("Parent : " + parentObject.getID())
   // def differenceGeom = geomOutside.difference(geomParent)
    
    //def geomEnd = differenceGeom.intersection(voronoiGeom)
    
        
    
    return geomInside
    
}

// execute first dilation
def firstDilation(parentObject,dilationFactor){

    firstDilationFeatures = [:]

    //Extract the parent info
    parentID = parentObject.getID()

    parentGeom = parentObject.getROI().getGeometry()

    perimeterParent = parentGeom.getLength()

    areaParent = parentGeom.getArea()

    s= (2*areaParent)/perimeterParent

    //Now extract the target area and the amouunt of s to decrease/increase to obtain the dilation: 
    targetArea = areaParent * dilationFactor
    firstDilationPx = s*(1-Math.sqrt(dilationFactor))
    
    firstDilationGeom = parentGeom.buffer( -firstDilationPx)

    //extract the info to know if the loop must be increasing or decreasing
    geomDilationArea = firstDilationGeom.getArea()
    differencePercentArea = (100*(geomDilationArea - targetArea))/targetArea

    if(differencePercentArea < 0 ){

        loopDirection = "IncreasingLoop"
    }else if(differencePercentArea >0 ){
        loopDirection = "DecreasingLoop"
    }

    firstAbsolutePercentDifference = Math.abs(differencePercentArea)
    
    firstDilationFeatures[parentID] = [geometry: firstDilationGeom, targetAreaPx: targetArea, s: s, firstDilationPx:firstDilationPx ,loopDirection:loopDirection,firstAbsolutePercentDifference :firstAbsolutePercentDifference ]
    
    return firstDilationFeatures
}



//adjust dialtion
def adjustDilationTargetAreaLoop(firstDilationFeatures){

    differenceGeomMap = [:]
    def finalGeom = null
    def finalAbsolutePercentDifference = 0
    

    firstDilationFeatures.each { objectID, data ->

        if (data.firstAbsolutePercentDifference < 0.1) {
            print("First Dilation is okay, absolute percent difference: " + data.firstAbsolutePercentDifference)
            finalGeom = data.geometry
            finalAbsolutePercentDifference = data.firstAbsolutePercentDifference
            
        } else {
            loopDirection = data.loopDirection.toString()

            geomFirstDilation = data.geometry
            s = data.s
            dilationPX = 0
            optimizingArea = geomFirstDilation.getArea()
            targetArea = data.targetAreaPx

            differenceGeomMap[Math.abs(optimizingArea - targetArea)] = geomFirstDilation

            if (loopDirection == "DecreasingLoop") {
                while (optimizingArea > (targetArea * 0.99)) {
                    percentDifference = (100 * (Math.abs(optimizingArea - targetArea))) / targetArea
                    valueExpansion = percentDifference > 10 ? 0.02 * s : 0.002 * s
                    dilationPX += valueExpansion
                    geomOptimmizer = geomFirstDilation.buffer(-dilationPX)
                    optimizingArea = geomOptimmizer.getArea()
                    differenceGeomMap[Math.abs(optimizingArea - targetArea)] = geomOptimmizer
                }
            } else if (loopDirection == "IncreasingLoop") {
                while (optimizingArea < (targetArea * 1.01)) {
                    percentDifference = (100 * (Math.abs(optimizingArea - targetArea))) / targetArea
                    valueExpansion = percentDifference > 10 ? 0.02 * s : 0.002 * s
                    dilationPX -= valueExpansion
                    geomOptimmizer = geomFirstDilation.buffer(-dilationPX)
                    optimizingArea = geomOptimmizer.getArea()
                    differenceGeomMap[Math.abs(optimizingArea - targetArea)] = geomOptimmizer
                }
            }
        }
    }

    // Now return finalGeom after processing all entries
    if (finalGeom == null && !differenceGeomMap.isEmpty()) {
        // Fallback to using the minimum difference geometry
        minDifference = differenceGeomMap.keySet().min()
        finalGeom = differenceGeomMap[minDifference]
        finalAbsolutePercentDifference = (100*Math.abs(finalGeom.getArea() - targetArea))/targetArea
    }

    // If still null, print a warning
    if (finalGeom == null) {
        println "Warning: No valid geometry was found."
    }

    return [finalGeom,finalAbsolutePercentDifference]
}




// Define a method to calculate the percentage difference between two regions
def calculatePercentageDifference(area1, area2) {
        return (Math.abs(area1 - area2) / ((area1 + area2) / 2)) * 100
}



//Script 4

def runScript4() {
    
    def server = getCurrentServer()
    def cal = server.getPixelCalibration()
    double pixelWidth = cal.getPixelWidthMicrons()
    double pixelHeight = cal.getPixelHeightMicrons()
    
    //remove detections that are inside of the oocyte
   
    def oo = getAnnotationObjects().findAll { it.getPathClass() == null }
     oo.forEach {
           it.setPathClass(getPathClass('Oocyte'))  
       } 
    oo.forEach {
        
    }
       
    def OOAnnotations = getAnnotationObjects().findAll { it.getPathClass() == getPathClass("Oocyte") }
    def hierarchy = getCurrentHierarchy()
    def Cells = OOAnnotations.collect { anno ->
        def cells = hierarchy.getObjectsForROI(null, anno.getROI()).findAll {it.isDetection()}
        return cells
    }  
    
    selectObjects(Cells.flatten())
    clearSelectedObjects()
    print("Checkpoint1")
    
    
    //for consistency with the new automtic segmentation let's convert all the oocyte annotations to detections
    oocyteAnnotations= getAnnotationObjects().findAll {
       it.getPathClass() == getPathClass("Oocyte") 
    }
    def newDetections = oocyteAnnotations.collect{
        return PathObjects.createDetectionObject(it.getROI(), it.getPathClass())
    }
    // removeObjects(annotations, true) // uncomment to remove original annotations
    addObjects(newDetections)
    removeObjects(oocyteAnnotations)
        
    
    resolveHierarchy()

    selectObjects {p -> p.getPathClass() == getPathClass("Oocyte")} 
    runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons":0.5,"region":"ROI","tileSizeMicrons":25.0,"colorOD":false,"colorStain1":true,"colorStain2":true,"colorStain3":false,"colorRed":false,"colorGreen":false,"colorBlue":false,"colorHue":false,"colorSaturation":false,"colorBrightness":false,"doMean":true,"doStdDev":false,"doMinMax":false,"doMedian":false,"doHaralick":false,"haralickDistance":1,"haralickBins":32}')
     
     //Get all the follicles, and check the ones that contain oocyte inside and add the data.
    anno = getAnnotationObjects().findAll {
        it.getPathClass()!= getPathClass("Oocyte") 
     }
     
      anno.forEach {
    
           childOocyte = it.getChildObjects().findAll {
           it.getPathClass() == getPathClass("Oocyte") 
        }
    
        print("Number Oocyte: " + childOocyte.size())
    
    
        if(childOocyte.size() == 1) {
            areaOocyte = childOocyte[0].getROI().getScaledArea(pixelWidth,pixelHeight)
            print("Area: " + areaOocyte)
            CircularityOocyte = RoiTools.getCircularity(childOocyte[0].getROI())
            print("Circularity: " + CircularityOocyte)
        
            meanHtx = childOocyte[0].measurements["ROI: 0.50 µm per pixel: Hematoxylin: Mean"]
            print("HTX_mean: " + meanHtx)
            meanEosin = childOocyte[0].measurements["ROI: 0.50 µm per pixel: Eosin: Mean"]
            print("Eosin_mean: " + meanEosin)
        
            it.getMeasurementList().put("areaOocyte", areaOocyte)
            it.getMeasurementList().put("circularityOocyte", CircularityOocyte)
            it.getMeasurementList().put("meanHematoxylinOocyte", meanHtx)
            it.getMeasurementList().put("meanEosinOocyte", meanEosin)
        
        }else {
           areaOocyte = 0
           CircularityOocyte = 0
           meanHtx= 0
           meanEosin= 0
           it.getMeasurementList().put("areaOocyte", areaOocyte)
           it.getMeasurementList().put("circularityOocyte", CircularityOocyte)
           it.getMeasurementList().put("meanHematoxylinOocyte", meanHtx)
           it.getMeasurementList().put("meanEosinOocyte", meanEosin)
       
        }
      
     }
     
    selectObjects {p -> p.getPathClass() == getPathClass("Oocyte")} 
    getSelectedObjects().each {it.setLocked(false)} 
    fireHierarchyUpdate()
    def annotations = hierarchy.getAnnotationObjects()
    def anno = annotations.findAll { it.isLocked() }

    anno.forEach {
   
       id = it.getName()
  
       childs = it.getChildObjects()
   
       childs.forEach {
          it.setName(id)
       } 
   
    }

    print("Oocyte have been assigned to their parent follicle!")
     
    
    def oocyteExportData = getDetectionObjects().findAll {
       it.getPathClass()== getPathClass("Oocyte") 
    }
    def name1 = getProjectEntry().getImageName() + '.geojson'
    def path1 = buildFilePath(PROJECT_BASE_DIR, 'Oocyte detections')
    mkdirs(path1)
    path1 = buildFilePath(path1, name1)
    exportObjectsToGeoJson(oocyteExportData, path1, "FEATURE_COLLECTION") 
    print 'Oocyte detections exported to ' + path1
   
   
   
   
}



//Script 5

def runScript5(maxNumberGranulosaCellsPrimordial,minFlatGCEccentricity,numCuboidCellsTH,  maxprimaryEccentricity,minNumberGranulosaCellsPrimaries,maxNumberGranulosaCellsPrimaries,maxTransitionalGCEccentricity,minNumberGranulosaCellssecondary,minFollicleAreaSecondary,maxFollicleAreaSecondary,minNumberGranulosaCellsantral,minfollicleareaantral,maxcellnumberfolliclearearatioignore) {
   
    /**This script unlocks oocyte annotation**/
 
     selectObjects {p -> p.getPathClass() == getPathClass("Oocyte")} 
     getSelectedObjects().each {it.setLocked(false)} 
     fireHierarchyUpdate()
     def hierarchy = getCurrentHierarchy()

    // Get a list of all annotations
    def annotations = hierarchy.getAnnotationObjects()

    // Iterate over the annotations selecting only the locked ones (Follicle) and change the ObjectID
    def anno = annotations.findAll { it.isLocked() }
    def server = getCurrentServer()
    def cal = server.getPixelCalibration()
    double pixelWidth = cal.getPixelWidthMicrons()
    double pixelHeight = cal.getPixelHeightMicrons()

    //We detect follicles that contain no oocyte and classify them as "Ignore"

    ignore = anno.findAll {
        it.measurements['areaOocyte'] == 0 
    }

    ignore.forEach {
       it.setPathClass(getPathClass('Ignore/Disclude')) 
    }

/**This script removes oocyte annotations**/
 
     //selectObjects {p -> p.getPathClass() == getPathClass("Oocyte")} 
     //getSelectedObjects().each {it.setLocked(false)} 
    //clearSelectedObjects()
    
    
    primordial = anno.findAll {
       it.getPathClass() != getPathClass('Ignore/Disclude') && it.getChildObjects().size() <=maxNumberGranulosaCellsPrimordial
    }

    primordial.forEach {
       it.setPathClass(getPathClass('primordial')) 
    }
    
    eccentricityTH = minFlatGCEccentricity
    classifyFolliclesPrimordial(primordial,eccentricityTH,numCuboidCellsTH)
    
    
    primary = getAnnotationObjects().findAll {
       it.getPathClass() != getPathClass('Ignore/Disclude') && 
       it.getPathClass() != getPathClass('primordial')&& 
       it.getPathClass() != getPathClass('Transitional') && 
       it.measurements["ChildMedianEccentricity"] < maxprimaryEccentricity && 
       it.getChildObjects().size() > minNumberGranulosaCellsPrimaries && 
       it.getChildObjects().size() <=maxNumberGranulosaCellsPrimaries
    }

    primary.forEach {
       it.setPathClass(getPathClass('primary')) 
    }
    
    
    //take the transitionals and if the median eccentricity is less than MaximumMedianGCEccentricity
    //classify them as Primary
    
    transitionalToPrimary = getAnnotationObjects().findAll {
       it.getPathClass() == getPathClass("Trasnitional") &&
       it.measuremets["ChildMedianEccentricity"] <= maxTransitionalGCEccentricity
    }
    
    transitionalToPrimary.forEach {
       it.setPathClass(getPathClass("primary")) 
    }
    
    
    
     //We detect secondaries (excluding those already classified) based on GC number 

    secondary = anno.findAll {
        it.getPathClass() != getPathClass('Ignore/Disclude') && 
        it.getPathClass() != getPathClass('primordial') && 
        it.getPathClass() != getPathClass('Transitional') && 
        it.getPathClass() != getPathClass('primary') && 
        it.getChildObjects().size() > minNumberGranulosaCellssecondary &&
        it.getROI().getScaledArea(pixelWidth, pixelHeight) > minFollicleAreaSecondary &&
        it.getROI().getScaledArea(pixelWidth, pixelHeight) <= maxFollicleAreaSecondary 
    }

    secondary.forEach {
       it.setPathClass(getPathClass('secondary')) 
    } 
    
    
    //We detect antral (excluding those already classified) based on GC number

    antral = anno.findAll {
        it.getPathClass() != getPathClass('Ignore/Disclude') && 
        it.getPathClass() != getPathClass('primordial') && 
        it.getPathClass() != getPathClass('Transitional') && 
        it.getPathClass() != getPathClass('primary') &&
        it.getChildObjects().size() > minNumberGranulosaCellsantral && 
        it.getROI().getScaledArea(pixelWidth, pixelHeight) > minfollicleareaantral
    }

    antral.forEach {
       it.setPathClass(getPathClass('Early Antral Follicle')) 
    }
    
    
    //We detect follicles that have higher number of GC nuclei per follicle area and classify them as "Ignore"

    ignore = anno.findAll {
        (it.getPathClass() == getPathClass('secondary') | it.getPathClass() == getPathClass('Early Antral Follicle')) && it.getROI().getScaledArea(pixelWidth, pixelHeight) /it.getChildObjects().size()<maxcellnumberfolliclearearatioignore 
    }
    
    ignore.forEach {
       it.setPathClass(getPathClass('Ignore/Disclude')) 
    }



     
     
  
}

//function that works with script 5
def classifyFolliclesPrimordial(objects,eccentricityTH,numCuboidCellsTH)  {
   
   
   objects.forEach {
       
       childs = it.getChildObjects().findAll {
          it.getPathClass() == getPathClass("Granulosa Cell Nuclei") 
       }
       
       numCuboidCellsCounter = 0
       
       childs.forEach {
          
          eccentricity = it.measurements["eccentricity"]
          
          if(eccentricity < eccentricityTH) {
             numCuboidCellsCounter+=1 
          }
          
       }
       print("Num Cuboid : " + numCuboidCellsCounter)
       
       if(numCuboidCellsCounter <= numCuboidCellsTH) {
          it.setPathClass(getPathClass("primordial")) 
       }else {
          it.setPathClass(getPathClass("Transitional")) 
       }
       
   }
   
      
   
}

//Script 6

def saveData() {
    
   def name1 = getProjectEntry().getImageName() + '.txt'
    def path1 = buildFilePath(PROJECT_BASE_DIR, 'Follicle data')
    mkdirs(path1)
    path1 = buildFilePath(path1, name1)
    saveAnnotationMeasurements(path1)
    print 'Follicle annotation data exported to ' + path1

    /** This script extracts data from detections in annotated follicles as text file**/

    def name2 = getProjectEntry().getImageName() + '.txt'
    def path2 = buildFilePath(PROJECT_BASE_DIR, 'GC detection data')
    mkdirs(path2)
    path2 = buildFilePath(path2, name2)
    saveDetectionMeasurements(path2)
    print 'GC nuclei data exported to ' + path2
   
   
}
