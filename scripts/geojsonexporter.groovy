import qupath.lib.objects.PathObject
import qupath.lib.projects.Projects
import qupath.lib.io.GsonTools

// Get the current project
def project = getProject()

// Loop through all image entries in the project
for (entry in project.getImageList()) {
    // Open each image
    def imageData = entry.readImageData()

    // Get the image name (without the extension)
    def imageName = entry.getImageName().replaceFirst(/\.[^.]+/, "")

    // Define the output path (same folder as the project, with .geojson extension)
    def outputPath = buildFilePath(PROJECT_BASE_DIR, imageName + ".geojson")

    // Set the current image data for processing
    imageData.getHierarchy().getSelectionModel().clearSelection()

    // Get the list of annotations from the image
    def annotations = imageData.getHierarchy().getAnnotationObjects()


    if (!annotations.isEmpty()) {
        // Write annotations to GeoJSON
        //exportAnnotationsAsGeoJson(outputPath, annotations)
        exportObjectsToGeoJson(annotations, outputPath, "FEATURE_COLLECTION")
        print("Exported annotations for: " + imageName + " to " + outputPath)
    } else {
        print("No annotations found for: " + imageName)
    }
}

// Function to export annotations as GeoJSON
def exportAnnotationsAsGeoJson(String outputPath, List<PathObject> annotations) {
    def gson = GsonTools.getInstance(true)  // Get a Gson instance (true enables pretty printing)

    def file = new File(outputPath)
    file.withWriter('UTF-8') { writer ->
        gson.toJson(annotations, writer)  // Convert the annotations to JSON and write to file
    }
}