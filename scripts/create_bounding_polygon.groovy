import java.awt.image.BufferedImage
import java.awt.Graphics2D
import java.awt.Color
import java.awt.BasicStroke
import java.awt.Polygon
import javax.imageio.ImageIO
import qupath.lib.gui.commands.ProjectCommands
import qupath.fx.dialogs.Dialogs
import qupath.lib.gui.tools.GuiTools
import qupath.lib.regions.RegionRequest

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
    if (lineNumber > 1) {
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
def projectDir = project.getPath().toFile().getParentFile()

// Loop through all image entries in the project
for (entry in project.getImageList()) {
    def imageName = entry.getImageName()
    def matchingKey = polygonMap.keySet().find { key ->
        new File(key).getName().equalsIgnoreCase(new File(imageName).getName()) ||
        key.toLowerCase().contains(imageName.toLowerCase())
    }

    if (matchingKey) {
        def polygonData = polygonMap[matchingKey]
        def imageData = getCurrentImageData()
        def hierarchy = imageData.getHierarchy()

        // Create and save the annotation
        def boundingClass = getPathClass("BoundingPolygon") ?: PathClassFactory.getPathClass("BoundingPolygon")
        def roi = ROIs.createPolygonROI(polygonData.xcoords, polygonData.ycoords, ImagePlane.getDefaultPlane())
        def bounding_poly = PathObjects.createAnnotationObject(roi)
        bounding_poly.setPathClass(boundingClass)
        bounding_poly.setName("BoundingPolygon")
        hierarchy.addObject(bounding_poly)

        // Fire hierarchy update so the annotation is fully registered before saving
        hierarchy.fireHierarchyChangedEvent(null)
 
        entry.saveImageData(imageData)
        project.syncChanges()
        
        println "Added BoundingPolygon for ${imageName}; total annotations: ${hierarchy.getAnnotationObjects().size()}"
 
        // Verify the annotation was saved correctly
        def verifyData = entry.readImageData()
        def savedCount = verifyData.getHierarchy().getAnnotationObjects().size()
        def savedNames = verifyData.getHierarchy().getAnnotationObjects().collect { it.getName() }
        println "  Verification - saved annotations: ${savedCount}, names: ${savedNames}"

        // Save downsampled image
        def snapshotDir = new File(projectDir, "downsampled_snapshots")
        snapshotDir.mkdirs()

        try {
            def server = imageData.getServer()
            def request = RegionRequest.createInstance(
                server.getPath(),
                10,                  // downsample factor
                0, 0,
                server.getWidth(),
                server.getHeight()
            )
            def outputPath = new File(snapshotDir, "${imageName}_downsampled.jpg").toString()
            writeImageRegion(server, request, outputPath)
            println "Downsampled image saved: ${outputPath}"
        } catch (Exception e) {
            println "Warning: Could not save downsampled image for ${imageName}: ${e.getMessage()}"
        }
    }
}

println "Done!"
