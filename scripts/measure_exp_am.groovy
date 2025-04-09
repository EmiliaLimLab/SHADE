import qupath.lib.gui.scripting.QPEx
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathAnnotationObject

// Parse provided args for output file
if (args.size() > 0)
    outputFile = new File(args[0].toString())
else
    outputFile = Dialogs.promptForDirectory(null)

if (outputFile == null)
    return

def project = getProject()
def imagesToExport = project.getImageList()
def exportType = PathAnnotationObject.class

def exporter = new MeasurementExporter()
    .imageList(imagesToExport)
    .separator(',')
    .exportType(exportType)
    .exportMeasurements(outputFile)

print "Export complete! Measurements saved to: ${outputFile}"