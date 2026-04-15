import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathAnnotationObject

def targetClass = args.length > 0 ? args[0] : "Anthracosis"
// -------------------------------------

def entry = getProjectEntry()

if (entry != null) {
    def imageName = entry.getImageName().replaceFirst(/\.[^.]+/, "")
    def outputFile = new File(buildFilePath(PROJECT_BASE_DIR, imageName + "_anth_anno.csv"))

    // Export measurements with a filter
    def exporter = new MeasurementExporter()
        .imageList([entry])
        .separator(',')
        .exportType(PathAnnotationObject.class)
        .filter(obj -> obj.getPathClass() == getPathClass(targetClass))
        .exportMeasurements(outputFile)

    println "Export complete! Saved only '${targetClass}' measurements to: ${outputFile}"
}
else {
    println "Error: could not find project entry"
}
