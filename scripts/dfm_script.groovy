// Parse provided args for classifierDir
if (args.size() > 0)
    classifierDir = args[0].toString()
else
    classifierDir = Dialogs.promptForDirectory(null)

if (classifierDir == null)
    return

def tissueClassifier = classifierDir + File.separator + "tissue_detection_annotation.json"
def darkPixelClassifier = classifierDir + File.separator + "max_hires_t75.json"
def heStainDeconvolution = classifierDir + File.separator + "residual_hires_t0.19.json"
def bloodClassifier = classifierDir + File.separator + "blood_hires_t0.75.json"

// Select bounding box to run pixel classifier on
selectObjectsByClassification("BoundingPolygon")
if (getSelectedObjects().isEmpty()) {
    println "No bounding polygon selected! Running classifiers on the entire image."
} else {
    println "Running classifiers within the selected bounding polygon(s)."
}

// Run pixel classifier to detect tissue region, then quantify
println "Detecting tissue using PixelClassifier: " + tissueClassifier
createAnnotationsFromPixelClassifier(tissueClassifier, 0.0, 0.0)
selectObjectsByClassification("Tissue")
addPixelClassifierMeasurements(heStainDeconvolution, heStainDeconvolution)

// Apply dual filter to quantify anthracotic pigment
println "Detecting anthracotic pigments using PixelClassifier: " + darkPixelClassifier
createAnnotationsFromPixelClassifier(darkPixelClassifier, 0.0, 0.0)
selectObjectsByClassification("Positive")
createAnnotationsFromPixelClassifier(heStainDeconvolution, 0.0, 0.0)
addPixelClassifierMeasurements(heStainDeconvolution, heStainDeconvolution)
selectObjectsByClassification("Anthracosis")
createAnnotationsFromPixelClassifier(bloodClassifier, 0.0, 0.0)
addPixelClassifierMeasurements(bloodClassifier, bloodClassifier)
