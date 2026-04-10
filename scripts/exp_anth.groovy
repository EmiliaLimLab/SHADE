import qupath.lib.gui.scripting.QPEx
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathAnnotationObject

def targetClass = "Anthracosis" 
// -------------------------------------

def project = getProject()
def currentImage = getCurrentImageData().getServer().getMetadata().getName()
def entry = project.getImageList().find { it.getImageName() == currentImage }

if (entry != null) {
    def imageName = entry.getImageName().replaceFirst(/\.[^.]+/, "")
    def outputFile = new File(buildFilePath(PROJECT_BASE_DIR, imageName + "anth_anno.csv"))

    // Export measurements with a filter
    def exporter = new MeasurementExporter()
        .imageList([entry])
        .separator(',')
        .exportType(PathAnnotationObject.class)
        .filter(obj -> obj.getPathClass() == getPathClass("Anthracosis"))
        .exportMeasurements(outputFile)

    println "Export complete! Saved only '${targetClass}' measurements to: ${outputFile}"
}
