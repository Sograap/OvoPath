## OvoPath: A graphical user interface tool for digital analysis of ovarian follicles

**This protocol explains the steps to analyse Haematoxylin and Eosin (H&E)-stained ovarian follicles with an open-source software (QuPath).**

Analyses include but are not limited to:

*   Follicle Area (μm2)
*   Average granulosa cell (GC) area per follicle (μm2)
*   Median granulosa cell circularity
*   Medial granulosa cell eccentricity
*   Number of granulosa cells per follicle
*   Oocyte Area
*   Follicle classification (primordial, transitional, primary, secondary, antral follicle)

## Resources and Materials

Requirements to perform these tasks:

*   Digital images of (H&E)-stained ovarian tissue. A wide range of image formats can be opened in QuPath ([Supported image formats — QuPath 0.5 documentation](https://qupath.readthedocs.io/en/0.5/docs/intro/formats.html)). If possible, avoid generic image formats such as jpeg, png as they will typically not preserve the required pixel size. 
*   QuPath v0.5.0 https://qupath.github.io/
*   Scripts:
    *   OvoPath\_GUI.groovy (This script works with QuPath version v0.5.0 and above )
*   Stardist:
    *   Install the extension:
        *   Read me: [https://github.com/qupath/qupath-extension-stardist?tab=readme-ov-file#installing](https://github.com/qupath/qupath-extension-stardist?tab=readme-ov-file#installing)
        *   File: https://github.com/qupath/qupath-extension-stardist/releases/download/v0.5.0/qupath-extension-stardist-0.5.0.jar
    *   Model: he\_heavy\_augment.pb (you will find it in the “scripts” folder)
        *   Read me: [https://github.com/qupath/models/tree/main/stardist](https://github.com/qupath/models/tree/main/stardist)
        *   File: [https://github.com/qupath/models/raw/main/stardist/he\_heavy\_augment.pb](https://github.com/qupath/models/raw/main/stardist/he_heavy_augment.pb)
*   Computer:
    *   Minimum (recommended) requirements:
        *   64-bit Windows, Linux or Mac
        *   RAM ≥ 16 GB
        *   Multicore processor (e.g., Intel Core i7)
    *   For further information, see Resources - QuPath workstation discussion.

## Cautions and warnings

1.Do not delete anything inside a QuPath project if you have not done a backup.

2.The annotations must be exquisite selecting follicle area excluding the surrounding stromal cells as well as oocyte area. Any error in the annotation process is very difficult and lengthy to be solved after cell segmentation.

Precise follicle and oocyte segmentation must be performed prior to running the analyses as classification and granular analysis of follicle features are determined using granulosa cell area and number of cells per follicle.

## Original image storage

1.  Create a folder where all your digital analysis data will be stored
    1.  Create a principal empty folder with the name of your experiment
    2.  Create a folder inside for the images.
    3.  Create a folder where all your QuPath data will be stored
2.  Open QuPath v0.5.0
    1.  On the top left of the window click “Create project”
        1.  Go to folder created in 1.3.
    2.  On the top left of the window click “Add images”
        1.  Click “Choose files” and select the images stored in 1.2
        2.  Parameters:
            1.  Image provider: Default (let QuPath decide)
            2.  Set image type: Brightfield (H&E)
            3.  Leave ticked Auto-generate pyramids
        3.  Click “Import”

## Follicle annotation

1.Donwload “OvoPath\_GUI.groovy” and “he\_augment.pb “ files. Drag and drop OvoPath\_GUI.groovy directly onto QuPath screen and press the “Run” button at the bottom right corner of the window. A set of steps to perform the analysis will appear on the right of the QuPath screen.

2.Carefully annotate follicle area using the brush and wand tools.

**NOTE**: Exclude, as much as possible, the surrounding stromal cells (especially in the smallest follicles) and theca cells (from secondary follicles onwards) as they may be considered as granulosa cells in the classification script. Inclusion of extrafollicular cells will create improper classification and analyses moving forward.

3.Press “Run Assign follicle\_ID” to assign each the class “Follicle” to the annotations and provide an ID code based on its coordinate values in the tissue section (F\_0, F\_1…). This script also saves follicle annotations that will appear in the QuPath project folder. In case you lose your annotations in your project you can drag and drop the .geojson file on your screen and there they are! **NOTE**: This will take a little time. Please be patient and wait for your IDs to appear on the screen.

Follicle Annotation Representative Image

![image](https://github.com/user-attachments/assets/1445bd65-040e-4d05-8a71-2749ce77c175)


## Granulosa cell annotation

1.  Go to “Script 2. Detect\_GC\_nuclei”. Introduce in the white bar the file path for **he\_augment.pb**. Please make sure that your file path extension uses “/” and not “\\”. **make sure you copy the .pb file and not the folder in which the .pb file is located!** ****Press “Run Detect GC nuclei”. Unspecific detections will appear inside the oocyte but these will be removed automatically in the following steps. If granulosa cells fail to be accurately detected, two parameters can be also tailored at this step: “Probability TH Detection Stardist” and “Image Resolution Stardist”. If parameters are changed, the script can be rerun as new detections will replace previous ones. If issues are found at this step create an “Issue” in our repository (https://docs.github.com/es/issues/tracking-your-work-with-issues/using-issues/creating-an-issue). **NOTE**: This will take a little time. Please be patient and wait for granulosa cell segmentations to appear. Depending on the number of follicles in the tissue slice, segmentation can take up to ~10 minutes.

Granuolsa Cell Nuclei segmentation 

![image](https://github.com/user-attachments/assets/a8f60796-572b-4048-ba1c-a1b336f5ffd1)

Granulosa Cell nuclei cleaning

![image](https://github.com/user-attachments/assets/6e32d340-d988-4d82-8870-97c360c809e9)

## Oocyte annotation

1.  Annotate oocyte area using the brush and wand tools. **NOTE**: Annotate ONLY follicles with clear/visible oocyte cytoplasms. Classifications of follicles without a clear oocyte will NOT reflect accurate feature data.
2.  Go to “Script 3. Clean\_and\_assign\_GC\_parameters and press “Run Clean\_and\_assign\_GC\_parameters.groovy. Unspecific detections inside the oocyte are removed and key parameters from granulosa cells are stored in the follicle parent annotation. If unspecific detections remain, cleaning can be modified by changing the values of three parameters: minimum nuclei area, maximum nuclei area and mean hematoxylin. They refer to the minimum/ maximum nuclei area values and minimum mean hematoxylin intensity value from which detected nuclei are considered specific. You can click on the unspecific detection and look for these values by scrolling down on the bottom left side of the QuPath screen. Modify any default value if necessary. This script also saves granulosa cell detections. **NOTE**. Each granulosa also contains the name of the follicle parent.
3.  Press “Run script 4. Add oocyte data”. Oocyte area, circularity, mean hematoxylin and mean eosin values are stored in the follicle parent annotation. This script also saves oocyte annotations.
4.  Go to “Script 5. Assign follicle stages”. Follicles are staged based on granulosa cell number per follicle, granulosa cell eccentricity and ratio of granulosa cell number per follicle area. You can run the default values or modify them depending on the tissue origin (e.g mouse ovarian tissue). Press “Run Assign follicle stages”. Follicles with no oocyte segmentation are classified as “Ignore”. Oocyte annotations will disappear from the project but they are saved after running script 4 in case you want them back. **NOTE:** If errors or issues are found, create an “Issue” in our repository. You can also post suggestions about which parameters to include that can improve the script.
5.  Press “Run Extract\_data”. This script extracts data from follicle parent annotations, so each line is a follicle with all integrated data from oocyte and granulosa cells. A folder with individual GC detection data will be also generated in case we want to explore the parameters of each cell nuclei.

Oocyte segmentation and data integration

![image](https://github.com/user-attachments/assets/438d3415-a767-4154-a551-04be223572e6)

Follicle classification and data extraction 

![image](https://github.com/user-attachments/assets/7c223a42-ff1b-4942-b8d0-e32d58265537)


# Resources

## QuPath workstation discussion

[https://forum.image.sc/t/designing-a-qupath-workstation/54849](https://forum.image.sc/t/designing-a-qupath-workstation/54849)

[https://qupath.readthedocs.io/en/stable/docs/tutorials/projects.html](https://qupath.readthedocs.io/en/stable/docs/tutorials/projects.html)

## QuPath download links

Windows:

[https://github.com/qupath/qupath/releases/download/v0.5.0/QuPath-v0.5.0-Windows.msi](https://github.com/qupath/qupath/releases/download/v0.5.0/QuPath-v0.5.0-Windows.msi)

Mac:

[https://github.com/qupath/qupath/releases/download/v0.5.0/QuPath-v0.5.0-Mac-x64.pkg](https://github.com/qupath/qupath/releases/download/v0.5.0/QuPath-v0.5.0-Mac-x64.pkg)

Linux:

https://github.com/qupath/qupath/releases/download/v0.5.0/QuPath-v0.5.0-Linux.tar.xz

## Annotations in QuPath

[https://qupath.readthedocs.io/en/stable/docs/starting/annotating.html](https://qupath.readthedocs.io/en/stable/docs/starting/annotating.html)
