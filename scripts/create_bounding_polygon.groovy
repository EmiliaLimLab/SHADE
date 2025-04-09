// Parse provided args for polygonFile
if (args.size() > 0)
    polygonFile = args[0].toString()
else
    bpolygonFile = Dialogs.promptForDirectory(null)

if (polygonFile == null)
    return

println "Parsing polygon coordinates from ${polygonFile} and adding polygon annotation for each image."

// Parse all lines in polygonFile
def polygonMap = [:]
new File(polygonFile).eachLine { line, lineNumber ->
    if (lineNumber > 1) {  // Check that current line is not the header
        def fields = line.split(/\t/)
        if (fields.size() >= 3) {
            def imagePath = fields[0].toString()
            def polygonData = [
                xcoords: fields[1].split(',').collect { it.toDouble() } as double[],
                ycoords: fields[2].split(',').collect { it.toDouble() } as double[]
            ]
        polygonMap[imagePath] = polygonData
        }
    }
}

// Get the current project
def project = getProject()

// Loop through all image entries in the project
for (entry in project.getImageList()) {
    // Extract bounding polygon coordinates from polygonMap
    def imageName = entry.getImageName()
    def matchingKey = polygonMap.keySet().find { key ->
        key =~ /${imageName}/
    }

    // Create bounding box if imageName is found in polygonMap keys
    if (matchingKey) {
        def polygonData = polygonMap[matchingKey]
        def imageData = entry.readImageData()
        def hierarchy = imageData.getHierarchy()
        def roi = ROIs.createPolygonROI(polygonData.xcoords, polygonData.ycoords, ImagePlane.getDefaultPlane())
        def bounding_poly = PathObjects.createAnnotationObject(roi)
        bounding_poly.setPathClass(getPathClass("BoundingPolygon"))
        hierarchy.addObject(bounding_poly)
        entry.saveImageData(imageData)
    }
}

println "Done!"