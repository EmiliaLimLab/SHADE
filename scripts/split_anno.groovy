///split annotations
selectObjectsByClassification("Anthracosis")
runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')

getProject().getEntry(getCurrentImageData()).saveImageData()
println "Anthracosis annotations split!"
