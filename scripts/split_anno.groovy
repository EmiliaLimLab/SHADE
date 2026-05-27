// Ensure arguments are provided from the CLI
if (args == null || args.length < 1) {
    println "ERROR: Missing required command-line arguments!"
    println "Usage: QuPath script <script_name> -a <classifierDir> -a [className]"
    return
}

// Map the CLI arguments safely
def classifierDir = args[0].toString()
def className = (args.length > 1) ? args[1].toString() : "Anthracosis"

println "--- Running Headless Automation ---"
println "Classifier Directory: ${classifierDir}"
println "Target Class Name:    ${className}"

def imageData = getCurrentImageData()
if (imageData == null) {
    println "No image data available!"
    return
}

def bloodClassifier = classifierDir + File.separator + "hematoxylin_hires_t0.75.json"

selectObjectsByClassification(className)
runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')
selectObjectsByClassification(className)
addPixelClassifierMeasurements(bloodClassifier, bloodClassifier)

// Save the changes back to the project
getProject().getEntry(imageData).saveImageData(imageData)
println "${className} annotations split!"
