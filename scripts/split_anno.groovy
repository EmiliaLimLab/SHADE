//Get arguments
def className = args.length > 0 ? args[0] : "Anthracosis"

//Split annotations
selectObjectsByClassification("Anthracosis")
runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')

//Save image data
def imageData = getCurrentImageData()
getProject().getEntry(imageData)saveImageData()
println "Anthracosis annotations split!"
