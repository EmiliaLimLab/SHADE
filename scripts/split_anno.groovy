///split annotations
selectObjectsByClassification("Anthracosis")
runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')

println "Anthracosis annotations split!"
