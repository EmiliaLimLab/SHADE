import qupath.lib.projects.Projects
import qupath.lib.objects.PathObject
import qupath.lib.objects.hierarchy.PathObjectHierarchy
import qupath.lib.gui.viewer.OverlayOptions
import qupath.lib.gui.images.servers.RenderedImageServer
import qupath.lib.gui.viewer.overlays.HierarchyOverlay
import qupath.lib.images.writers.ImageWriter
import qupath.lib.images.writers.ome.OMEPyramidWriter

if (args.size() > 0) {
    exportClass = args[0].toString().capitalize()  // ensure first letter is capitalized
} else {
    println "Classification to export annotations was not provided. Exporting 'Anthracosis' annotations by default!"
    exportClass = "Anthracosis"
}

if (exportClass == null) {
    exportClass = "Anthracosis"
}

// Get the current project
def project = getProject()

// Get current image entry in project
def currentImage = getCurrentImageData().getServer().getMetadata().getName()
def entry = project.getImageList().find { it.getImageName() == currentImage }

if (entry != null) {
    // Open image and set image data for processing
    def imageData = entry.readImageData()
    def imageName = entry.getImageName().replaceFirst(/\.[^.]+/, "")
    def imageHierarchy = imageData.getHierarchy()

    // Get list of annotations from image
    def annotations = imageHierarchy.getAnnotationObjects()
    if (annotations.isEmpty()) {
        println "No annotations found for: " + imageName
    }

    // Extract 'Positive' annotations (anthracosis) only, overlay on image, then export
    def positiveAnnotations = annotations.findAll { annotation ->
        def classification = annotation.getPathClass()
        return classification != null && classification.getName() == exportClass
    }

    if (positiveAnnotations.isEmpty()) {
        println "No " + exportClass.toLowerCase() + "annotations found for: " + imageName
    } else {
        def outputTiff = buildFilePath(PROJECT_BASE_DIR, imageName + "." + exportClass.toLowerCase() + "-annotated.ome.tiff")
        overlayAnnotations(imageData, outputTiff, positiveAnnotations, exportClass)
    }
}

// Function to overlay annotations on image
def overlayAnnotations(ImageData imageData, String outputPath, List<PathObject> annotations, String exportClass) {
    // Set annotation colour depending on class
    if (exportClass == "Anthracosis") {
        annotationColour = [255, 255, 0]
    } else {
        annotationColour = [200, 0, 0]
    }

    // Create temporary image data with hierarchy only containing annotations of interest (in yellow)
    def tempHierarchy = new PathObjectHierarchy()
    annotations.each { it.setColor(annotationColour[0], annotationColour[1], annotationColour[2]) }
    tempHierarchy.addObjects(annotations)
    def tempImageData = new ImageData(imageData.getServer(), tempHierarchy)

    // Create renderedServer with annotations overlayed
    def overlayOptions = new OverlayOptions()
    overlayOptions.setShowAnnotations(true)

    def renderedServer = new RenderedImageServer.Builder(tempImageData)
        .layers(new HierarchyOverlay(null, overlayOptions, tempImageData))
        .build()

    // Export
    def imageWriter = new OMEPyramidWriter.Builder(renderedServer)
        .tileSize(256)
        .channelsInterleaved()  // interleave, don't store channels in separate planes
        .parallelize(Runtime.getRuntime().availableProcessors())  // use parallel processing, if available
        .pixelType('uint8')
        .losslessCompression()
        .build()
        .writePyramid(outputPath)
}