/*
 * OvoPath project is licensed under the GNU GENERAL PUBLIC LICENSE (GPL-3.0).
 * Please check https://github.com/Sograap/OvoPath if you have any doubts about licensing.
 *
 * Script to run a custom Semantic Segmentation model with QuPath, using Deep Java Library (DJL) and the PyTorch engine.
 * Hence, it is necessary to install the DJL extension for QuPath and download the PyTorch engine.
 *
 * We provide one model checkpoints:
 *   * "Follicle_HE_unet_effnet": segments follicles in H&E-stained samples
 * 
 *
 * @Parameter "modelPath": Full path to the traced TorchScript model that ends with ".pt".
 *
 * @Parameter "parentAnnotationsClass": String with the parent classification. Example: "Tissue".
 *
 * @Parameter "outputClassName": String with the output classification. Example: "Follicle".
 *
 * @Parameter "overlap": The model is trained on 512×512 tiles. Adjust the amount of overlap between tiles.
 *   * IMPORTANT: If it is less than 10 or more than 200, the script will not run.
 *
 * @Parameter "originalPxSize": Double containing the pixel size (µm/px) of the image.
 *   * IMPORTANT: Models have been trained with images with tiles of 0.23µm/px and  0.465 µm/px.
 *   * We do not recommend a resolution higuer than 0.5 µm/px.
 *   * A warning will appear, but the model will run.
 *
 * @Parameter "numThreads": Integer containing the number of threads to use on CPU.
 *   * We have not seen many benefits from using more than 4 threads.
 *   * IMPORTANT:If you set more threads than available an error will appear
 *
 * 
 */

 
 //To complete
 
 String modelPath = "path/to/Follicle_HE_unet_effnet/Follicle_HE_unet_effnet.pt"
 
 String parentAnnotationsClass = "Tissue"
 
 String outputClassName = null
 
 int overlap = 30

 double originalPxSize = 0.23
 
 int numThreads = 4
 
 
 
  
 
 
 
 
 
 
 /////////////////////////////////////////////
 ///////////DO NOT TOUCH FROM HERE ///////////
 /////////////////////////////////////////////
 
 
 
 runOvopathUnet(
     modelPath,
     parentAnnotationsClass,
     outputClassName,
     overlap,
     originalPxSize,
     numThreads
     
 )
 
 
 print("Inference Done!")
 
  
def runOvopathUnet(
        String modelPath,
        String parentClassName,
        String outputClassName,
        int overlap,
        double originalPxSize,
        int numThreads
) {
     Logger LOG = LoggerFactory.getLogger("OvopathUnetRunner")
    // --- constants you can tweak if needed ---
     int TILE_SIZE_TRAINING = 512

    // --- basic checks ---
    def imageData = getCurrentImageData()
    if (imageData == null) { LOG.error("No image open."); return }
    if (!modelPath)        { LOG.error("modelPath is empty."); return }
    def f = new File(modelPath)
    if (!f.exists())       { LOG.error("Model file not found: ${f.absolutePath}"); return }

    def parents = getAnnotationObjects().findAll { it.getPathClass() == getPathClass(parentClassName) }
    if (parents.isEmpty()) { LOG.error("No parent annotations found for class '${parentClassName}'."); return }

    if (overlap < 10 || overlap > 200) {
        LOG.error("Overlap must be between 10 and 200 pixels (got ${overlap})."); return
    }

    if (originalPxSize > 0.5) {
        println "WARN: We do not recommend using the model at resolutions > 0.5 µm/px (requested ${originalPxSize})."
    }

    int maxThreads = Runtime.runtime.availableProcessors()
    if (numThreads < 1)       numThreads = 1
    if (numThreads > maxThreads) {
        LOG.error("You set more threads (${numThreads}) than available (${maxThreads}); reduce the number of threads.")
        return
    }

    // --- prepare geometry & tiling parameters ---
    def server = imageData.getServer()
    def cal    = server.getPixelCalibration()
    double ds  = originalPxSize / cal.getPixelWidthMicrons()
    int tilePx = Math.max(8, Math.round((float)(TILE_SIZE_TRAINING * ds)))
    def gf     = new GeometryFactory()
    
    LOG.info("U-Net inference starts")
    LOG.info("Overlap (px): ${overlap}  |  Threads: ${numThreads}  | Running on  CPU only")

    // --- build model (CPU-only) ---
    def model = createModel_CPU(modelPath)

    // --- task runner on CPU ---
    TaskRunner runner = TaskRunnerUtils.getDefaultInstance().createTaskRunner(numThreads)

    def created = []
    parents.eachWithIndex { anno, idx ->
        def plane   = anno.getROI().getImagePlane()
        def roi     = anno.getROI()
        def geom    = roi.getGeometry()

        // grid of tiles over parent bbox, keep those intersecting the parent geometry
        def tiles = []
        double xMin = roi.getBoundsX(), xMax = xMin + roi.getBoundsWidth()
        double yMin = roi.getBoundsY(), yMax = yMin + roi.getBoundsHeight()
        for (double x = xMin; x < xMax; x += (tilePx - overlap))
            for (double y = yMin; y < yMax; y += (tilePx - overlap)) {
                def env = new Envelope(x, x + tilePx, y, y + tilePx)
                if (geom.intersects(gf.toGeometry(env)))
                    tiles << ROIs.createRectangleROI(x, y, tilePx, tilePx, plane)
            }

        int nTiles = tiles.size()
        LOG.info(String.format("[%d/%d] Parent '%s' — %d tiles",
                idx + 1, parents.size(), anno.getPathClass()?.getName() ?: "—", nTiles))

        // progress bookkeeping (~5% steps)
        def q     = new ConcurrentLinkedQueue(tiles)
        def parts = java.util.Collections.synchronizedList(new ArrayList<Geometry>())
        def done  = new AtomicInteger(0)
        final int STEP = Math.max(5, Math.max(1, (int)Math.ceil(nTiles / 20.0)))

        // tasks for each thread share the same (thread-local) predictor
        def tasks = (0..<numThreads).collect {
            (Runnable) {
                def predictor = model.newPredictor()
                try {
                    while (true) {
                        def tile = q.poll()
                        if (tile == null) break
                        def g = inferTile_CPU(predictor, server, tile, roi)
                        if (g != null) parts.add(g)

                        int k = done.incrementAndGet()
                        if (k % STEP == 0 || k == nTiles)
                            LOG.info(String.format("Progress: %d/%d (%.1f%%)", k, nTiles, 100.0 * k / Math.max(1, nTiles)))
                    }
                } finally {
                    predictor.close()
                }
            }
        }

        // run inference
        runner.runTasks(null, tasks)

        // merge parts using the same runner
        if (!parts.isEmpty()) {
            LOG.info("Merging ${parts.size()} geometries")
            def merged = reduceUnion_TaskRunner(parts, runner)
            if (merged != null && !merged.isEmpty()) {
                def outROI = GeometryTools.geometryToROI(merged, plane)
                created << PathObjects.createAnnotationObject(outROI, getPathClass(outputClassName))
            }
        }
    }

    // commit
    model.close()
    if (!created.isEmpty()) addObjects(created)
    resolveHierarchy()
    LOG.info("Done. Created ${created.size()} annotation(s) with class '${outputClassName}'.")
}

// ----------------------- helpers (CPU-only) -----------------------

/** Build DJL Criteria & load model on CPU (PyTorch engine). */
private static def createModel_CPU(String modelPath) {
    def translator = SemanticSegmentationTranslator.builder()
            .addTransform(new ResizeShort(512, 512, Image.Interpolation.BILINEAR))
            .addTransform(a -> a.transpose(2, 0, 1).toType(DataType.FLOAT32, false))
            .build()

    def criteria = Criteria.builder()
            .setTypes(Image.class, CategoryMask.class)
            .optModelPath(java.nio.file.Paths.get(modelPath))
            .optTranslator(translator)
            .optEngine("PyTorch")
            .optProgress(new ProgressBar())
            .optDevice(ai.djl.Device.cpu())   // <-- CPU ONLY
            .build()

    return criteria.loadModel()
}

/** Run inference on one tile and return geometry for class "cluster". */
private static Geometry inferTile_CPU(predictor, server, tileROI, annotationROI) {
    def req = RegionRequest.createInstance(server.getPath(), 1, tileROI)
    def buf = server.readRegion(req)
    def out = DjlZoo.segmentROIs(predictor, buf, req, annotationROI, true)
    return out["follicle"]?.getGeometry()
}

/** Reduce union of geometries in batches using the provided TaskRunner. */
private static Geometry reduceUnion_TaskRunner(List<Geometry> geoms, TaskRunner runner, int batchSize = 64) {
    if (geoms == null || geoms.isEmpty()) return null
    if (geoms.size() == 1) return geoms[0]
    def current = new ArrayList<Geometry>(geoms)

    while (current.size() > 1) {
        int bs = Math.max(1, Math.min(batchSize, current.size()))
        int groups = (int)Math.ceil(current.size() / (double)bs)
        def results = java.util.Collections.synchronizedList(new ArrayList<Geometry>(groups))
        def tasks   = new ArrayList<Runnable>(groups)

        for (int i = 0; i < current.size(); i += bs) {
            int j = Math.min(current.size(), i + bs)
            def slice = new ArrayList<Geometry>(current.subList(i, j))
            tasks.add((Runnable){ results.add(UnaryUnionOp.union(slice)) })
        }
        runner.runTasks(null, tasks)
        current = results
    }
    return current.get(0)
}



//imports

import static qupath.lib.gui.scripting.QPEx.*

import qupath.lib.plugins.TaskRunner
import qupath.lib.plugins.TaskRunnerUtils

// DJL / QuPath-DJL
import ai.djl.repository.zoo.Criteria
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.output.CategoryMask
import ai.djl.modality.cv.translator.SemanticSegmentationTranslator
import ai.djl.modality.cv.transform.ResizeShort
import ai.djl.ndarray.types.DataType
import ai.djl.training.util.ProgressBar
import qupath.ext.djl.DjlZoo

// QuPath & JTS
import qupath.lib.regions.RegionRequest
import qupath.lib.roi.ROIs
import qupath.lib.roi.GeometryTools
import qupath.lib.objects.PathObjects
import org.locationtech.jts.geom.*
import org.locationtech.jts.operation.union.UnaryUnionOp

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import org.slf4j.Logger
import org.slf4j.LoggerFactory

 
