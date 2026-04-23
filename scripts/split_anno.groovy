// Split annotations

def className = args.length > 0 ? args[0] : "Anthracosis"

def imageData = getCurrentImageData()
if (imageData == null) {
    println "No image data available!"
    return
}

selectObjectsByClassification(className)
runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')

// Save the changes back to the project
getProject().getEntry(imageData).saveImageData(imageData)
println "${className} annotations split!"
