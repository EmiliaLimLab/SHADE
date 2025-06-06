# Anthracosis quantification

This repository includes scripts to perform automatic anthracotic pigment quantification from whole slide images using pixel classifiers.

## Installation

Install QuPath v0.5.1: [https://github.com/qupath/qupath/releases/tag/v0.5.1](https://github.com/qupath/qupath/releases/tag/v0.5.1).

Necessary packages (for image pre-processing) can be installed using conda and the `environment.yml` included in this repo: [https://www.anaconda.com/docs/getting-started/miniconda/install](https://www.anaconda.com/docs/getting-started/miniconda/install). 

```bash
conda env create --file=environment.yml
```

### Dependencies

* External tools:
  * [QuPath](https://github.com/qupath/qupath/releases/tag/v0.5.1) v0.5.1
* Python packages:
  * numpy (tested with v2.2.3)
  * opencv (tested with v4.11.0)
  * openslide (tested with v4.0.0)
  * openslide-python (tested with v1.4.1)
  * pillow (tested with v11.1.0)
  * scikit-image (tested with v0.25.0)


## Running anthracosis quantification

### Description of scripts

There are two main scripts included in this repo, both of which can be found in `bin/`:

* `preprocess_he_otsu.py`: outputs bounding polygon coordinates around tissue region
  * **Note:** this script can only be run on slide images in SVS format
* `sbatch-qupath`: SLURM script for performing anthracotic pigment quantification

### Step-by-step guide

Prior to quantifying anthracotic pigments in slide images, an adaptation of Otsu thresholding, presented by [Schreiber _et al._ 2024](https://www.nature.com/articles/s41598-023-50183-4), can be used to identify artifacts in the slides. **As these artifacts also appear dark, they will inflate anthracotic pigment measurements**; thus, a bounding polygon can be drawn around the tissue region to reduce the effects of such artifacts in downstream quantification.

```bash
python preprocess_he_otsu.py -he ${img_path} -o ${out_path}
```

This will output three files, two of which should be manually reviewed before proceeding:

* `*.polyROI-annotated.png`: slide image in PNG format with bounding polygon annotated
* `polyROI_coordinates.tsv`: TSV file with bounding polygon x and y coordinates for each slide image in `${img_path}`

If the bounding polygon annotated in the slide image PNGs do not appear to be correct, manually edit the coordinates in `polyROI_coordinates.tsv`.

Anthracotic pigment quantification can then be performed using QuPath. To do this, first specify the path to your QuPath and Anthracosis quantification installations (`QUPATH_DIR` and `ANTHRACOSIS_QUANT_DIR`, respectively) in the `sbatch-qupath` script, then run as follows:

```bash
sbatch sbatch-qupath -d ${img_path} -b ${polyROI_coords_tsv}
```

The `-e` option can also be provided to export anthracosis annotations (default) overlayed on slide images as OME.TIFF files. 

```bash
# Anthracosis annotations exported by default
sbatch sbatch-qupath -d ${img_path} -b ${polyROI_coords_tsv} -e
# Can also be passed as an argument to -e
sbatch sbatch-qupath -d ${img_path} -b ${polyROI_coords_tsv} -e anthracosis

# Similarly, tissue annotations can be exported if passed as an argument
sbatch sbatch-qupath -d ${img_path} -b ${polyROI_coords_tsv} -e tissue
```

Several annotation types can be provided as input. This will export all specified annotations overlayed as a single OME.TIFF file.

```bash
sbatch sbatch-qupath -d ${img_path} -b ${polyROI_coords_tsv} -e anthracosis,tissue
```

**Note:** Based on our experience, OME.TIFF files tend to be >10GB in size with file size scaling with amount of anthracosis.

It is recommended that a helper script be used to launch the quantification SLURM script separately for each slide image. Refer to `tests/` for an example.

## Output files

Main output files can be found in the `QuPathProject` directory:
* `{sample}.csv`: CSV file with measurements for each component 
* `{sample}.geojson`: GeoJSON file containing x,y coordinates for each component
  * If QuPath GUI is installed, the GeoJSON files can be imported, enabling interactive visualization of each component overlayed on the slide image
* `{sample}.{anthracosis/tissue/positive/other}-annotated.ome.tiff` (optional): Static OME.TIFF file with anthracosis, tissue, positive and/or other annotations overlayed on the slide image
  * Only output if `-e` option is provided

 ## Test demo

To test your installation and familiarize yourself with the scripts, a test demo can be found in `tests/`. Refer to corresponding `README` for how to run.

## Tips

Based on several test runs, we have noticed that a SVS slide image that is 1-2GB in size takes ~300GB of RAM and 4-6 hrs to complete. Optimizations will be made in the near future to reduce resource usage!
