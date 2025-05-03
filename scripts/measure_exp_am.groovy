import qupath.lib.gui.scripting.QPEx
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathAnnotationObject

// Get the current project
def project = getProject()

// Get current image entry in project
def currentImage = getCurrentImageData().getServer().getMetadata().getName()
def entry = project.getImageList().find { it.getImageName() == currentImage }

if (entry != null) {
    // Open image and set image data for exporting
    def imageData = entry.readImageData()
    def imageName = entry.getImageName().replaceFirst(/\.[^.]+/, "")

    // Define output filename
    def outputFile = new File(buildFilePath(PROJECT_BASE_DIR, imageName + ".csv"))

    // Export measurements
    def exportType = PathAnnotationObject.class
    def exporter = new MeasurementExporter()
    .imageList([entry])  // pass single image as list
    .separator(',')
    .exportType(exportType)
    .exportMeasurements(outputFile)

    println "Export complete! Measurements saved to: ${outputFile}"
}