import qupath.lib.projects.Projects
import qupath.lib.objects.PathObject
import qupath.lib.objects.hierarchy.PathObjectHierarchy
import qupath.lib.gui.viewer.OverlayOptions
import qupath.lib.gui.images.servers.RenderedImageServer
import qupath.lib.gui.viewer.overlays.HierarchyOverlay
import qupath.lib.images.writers.ImageWriter
import qupath.lib.images.writers.ome.OMEPyramidWriter

if (args.size() > 0) {
    // Split comma-separated string (if applicable) and clean up each class name
    exportClasses = args[0].toString().split(',').collect { className ->
        className.trim().capitalize()  // remove whitespace and ensure first letter is capitalized
    }
} else {
    println "Classification to export annotations was not provided. Exporting 'Anthracosis' annotations by default!"
    exportClasses = ["Anthracosis"]
}

if (exportClasses == null) {
    exportClasses = ["Anthracosis"]
}

// Check validity of provided exportClasses
def validClasses = ["Tissue", "Positive", "Anthracosis", "Other"]
if (!exportClasses.every { it in validClasses }) {
    throw new IllegalArgumentException("At least one classification provided does not exist. Supported classifications include: " + validClasses.collect { it.toLowerCase() }.join(", "))
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

    // Extract specified annotations only, overlay on image, then export
    def specifiedAnnotations = annotations.findAll { annotation ->
        def classification = annotation.getPathClass()
        return classification != null && classification.getName() in exportClasses
    }

    if (specifiedAnnotations.isEmpty()) {
        println "No " + exportClasses.collect { it.toLowerCase() }.join(", ") + "annotations found for: " + imageName
    } else {
        def outputTiff = buildFilePath(PROJECT_BASE_DIR, imageName + "." + exportClasses.collect { it.toLowerCase() }.join("-") + "-annotated.ome.tiff")
        overlayAnnotations(imageData, outputTiff, specifiedAnnotations, exportClasses)
    }
}

// Function to overlay annotations on image
def overlayAnnotations(ImageData imageData, String outputPath, List<PathObject> annotations, List<String> exportClasses) {
    // Set annotation colour depending on class
    annotationColourMap = [:]
    exportClasses.each { className ->
        if (className == "Anthracosis") {
            annotationColourMap[className] = [255, 255, 0]
        } else {
            annotationColourMap[className] = [200, 0, 0]
        }
    }

    // Create temporary image data with hierarchy only containing annotations of interest (in yellow)
    def tempHierarchy = new PathObjectHierarchy()
    annotations.each {
        def annotationColour = annotationColourMap[it.getPathClass()?.getName()]
        it.setColor(annotationColour[0], annotationColour[1], annotationColour[2])
    }
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