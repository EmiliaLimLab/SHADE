# Test demo

Example slide image was obtained from the OpenSlide data repository: https://openslide.cs.cmu.edu/download/openslide-testdata/Aperio/.

To run the demo, first activate your conda environment:

```bash
conda activate anthracosis_quant
```

Then, define a bounding polygon around the tissue for each slide image using the script `preprocess_he_otsu.py`.

```bash
python preprocess_he_otsu.py -he . -out .
```

To quantify the anthracotic pigments, it is recommended to use a helper script to launch a `sbatch-qupath` job for each slide image. The script included in this directory, `submit_sbatch-qupath` can be used for this. It takes three arguments:

* `dir`: directory with slide image(s)
* `bounding_poly_coords`: TSV with bounding polygon x and y coords, output by `preprocess_he_otsu.py`
* `file_list`: list of specific slide image filenames to perform quantification on, one name per line (optional)

As there is only one image in this test directory, we don't need to specify `file_list` can can simply run as follows:

```bash
./submit_sbatch-qupath . polyROI_coordinates.tsv
```

That's it! The output files should match those found in `expected_output/`.

## Notes

Specifying 50GB of RAM, this test demo should only take a couple minutes to complete.
