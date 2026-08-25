# Test demo

Example slide image was obtained from the NCI Genomic Data Commons TCGA: https://portal.gdc.cancer.gov/cases/9240d5fc-de23-4436-8099-da9bd3054860.

To run the demo, first activate your conda environment:

```bash
conda activate anthracosis_quant
```

Then, define a bounding polygon around the tissue for each slide image using the script `preprocess_he_otsu.py`.

```bash
python preprocess_he_otsu.py -he . -o .
```

To quantify the anthracotic pigments, it is recommended to use a helper script to launch a `sbatch-qupath` job for each slide image. The script included in this directory, `submit_sbatch-qupath` can be used for this. It takes three arguments:

* `dir`: directory with slide image(s)
* `bounding_poly_coords`: TSV with bounding polygon x and y coords, output by `preprocess_he_otsu.py`
* `file_list`: list of specific slide image filenames to perform quantification on, one name per line (optional)

As there is only one image in this test directory, we don't need to specify `file_list` can can simply run as follows:

```bash
./submit_sbatch-qupath . polyROI_coordinates.tsv
```
Optionally, if you want to quantify the anthracotic pigments without the use of the bounding box, you can remove this section `-b ${bounding_poly_tsv}` on line 31 of the `submit_sbatch-qupath` script. Then you can run the script as follows:

```bash
./submit_sbatch-qupath .
```

That's it! The output files should match those found in `expected_output/`.

## Notes

Specifying 50GB of RAM, this test demo should takes 10-15 minutes to complete. This can be shortened by removing the `-e` option in `submit_sbatch-qupath` when launching `sbatch-qupath`.
