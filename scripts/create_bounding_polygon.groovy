// Parse provided args for polygonFile
if (args.size() > 0)
    polygonFile = args[0].toString()
else
    polygonFile = Dialogs.promptForFile(null)

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

        // Take snapshot after bounding polygon is created and saved
        try {
            // Open the image in viewer to capture the snapshot
            def viewer = getBatchProjectData(imageData)
            
            // Get the viewer snapshot
            def img = GuiTools.makeViewerSnapshot()
            
            // Create snapshots directory if it doesn't exist
            def snapshotDir = new File("${qproj_path}/bounding_polygon_snapshots")
            snapshotDir.mkdirs()
            
            // Save the snapshot with image name
            def outputPath = "${snapshotDir}/${imageName}_bounding_polygon.png"
            writeImage(img, outputPath)
            println "Snapshot saved: ${outputPath}"
        } catch (Exception e) {
            println "Warning: Could not save snapshot for ${imageName}: ${e.getMessage()}"
        }
    }
}

println "Done!"
