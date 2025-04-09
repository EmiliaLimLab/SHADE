import argparse
import sys
import os
from time import strftime
import numpy as np
import openslide
from PIL import ImageDraw
import cv2

sys.path.append(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'scripts'))
from h_and_e_otsu_thresholding import he_otsu


class HESlide:
    def __init__(self, file_path=None, openslide_obj=None):
        if openslide_obj:
            self.file_path = file_path
            self.image_name = os.path.basename(file_path).removesuffix('.svs')
            self.openslide_obj = openslide_obj
        else:
            sys.stderr.write(
                strftime("%Y-%m-%d %H:%M:%S") + f": Invalid OpenSlide object for {file_path}, skipping.\n")
            sys.stderr.flush()


def load_slides(he_path):
    """
    Look for slide images (*.svs) in he_path and create HEslide objects for each
    :param he_path: directory to search for slide images
    :return: array of HESlide objects
    """
    all_slides = []
    not_supported_ext = ['.ome.tif', '.ome.tiff', '.tif', '.tiff']
    for file in os.listdir(he_path):
        if file.endswith('.svs'):
            file_path = os.path.join(he_path, file)
            openslide_obj = openslide.OpenSlide(file_path)
            all_slides.append(HESlide(file_path, openslide_obj))
        elif file.endswith(tuple(not_supported_ext)):
            sys.stderr.write(
                strftime("%Y-%m-%d %H:%M:%S") + f': tif/tiff formats not supported, skipping.\n')
            sys.stderr.flush()
    return all_slides


def calc_coord_downsample_factors(openslide_obj, level):
    """
    Calculate downsample factor and offsets for x and y dimensions.
    :param openslide_obj: OpenSlide object created from H&E slide image
    :param level: level of image resolution that black bordal detection was performed on
    :return: tuple of downsample factors and offset (x_downsample_factor, x_offset, y_downsample_factor, y_offset)
    """
    # Get lvl 0 and downsampled image dimensions
    downsampled_max_coords = tuple(dim - 1 for dim in openslide_obj.level_dimensions[level])
    lvl0_max_coords = tuple(dim - 1 for dim in openslide_obj.level_dimensions[0])

    # Calculate downsample factors
    x_downsample_factor = lvl0_max_coords[0] / downsampled_max_coords[0]
    y_downsample_factor = lvl0_max_coords[1] / downsampled_max_coords[1]

    return x_downsample_factor, y_downsample_factor


def convert_to_original_coordinates(downsample_factors, x_coords, y_coords):
    """
    Given to downsample factor, transform x_coords and y_coords to lvl0 coordinates.
    :param downsample_factor: tuple of downsample factors (x_factor, y_factor)
    :param x_coords: list of x coordinates
    :param y_coords: list of y coordinates
    :return: x_coords_full, y_coords_full
    """
    x_coords_full = [int(x * downsample_factors[0]) for x in x_coords]
    y_coords_full = [int(y * downsample_factors[0]) for y in y_coords]
    return x_coords_full, y_coords_full


def enhance_sv_channels(rgb_img_arr):
    """
    Enhance pink and dark pixels in RGB image to emphasize tissue and artifacts.
    :param rgb_img: 2D array representation of image in RGB format
    :return: 2D array representation of enhanced RGB image
    """
    # Convert RGB image to HSV, then save as array (dimensions of array equal to dimensions of image)
    hsv_img_arr = cv2.cvtColor(rgb_img_arr, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv_img_arr)

    # Increase saturation and value to enhance contrast between pink (tissue) and dark pixels
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    s_enhanced = clahe.apply(s)
    v_enhanced = clahe.apply(v)

    # Merge and convert back to RGB image (represented as array)
    return cv2.cvtColor(cv2.merge((h, s_enhanced, v_enhanced)), cv2.COLOR_HSV2RGB)


def define_polygon_roi(tissue_mask):
    """
    Define polygon coordinates that encompasses tissue pixels, as indicated in tissue mask.
    :param tissue_mask: binary mask where tissue pixels are set to True, otherwise False
    :return: polygon x_coords, y_coords
    """
    # Find contours (curve joining all continuous points with same value) in binary tissue mask
    # RETR_EXTERNAL flag so only outer bondaries returned - doesn't capture internal holes/structures
    contours, _ = cv2.findContours(tissue_mask.astype(np.uint8) * 255, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    # Concatenate points for all contours: for all contours, convert 3D array (N, 1, 2) to 2D array (N, 2) then concatenate
    all_points = []
    for contour in contours:
        all_points.extend(contour.reshape(-1, 2))

    # Create convex hull (smallest convex polygon that encloses all points)
    hull = cv2.convexHull(np.array(all_points).reshape(-1, 1, 2).astype(np.int32))  # conversion back to 3D array required for cv2 operations
    hull_points = hull.reshape(-1, 2)

    return hull_points[:, 0], hull_points[:, 1]


def exclude_artifacts(he_slide, out_path):
    """
    Identify black borders in HEslide image and return x_min, x_max, y_min, y_max coordinates (bounding box) with
    borders excluded.
    :param he_slide: HEslide object
    :param should_refine: boolean value indicating whether iterative refinment of borders should be performed
    :param out_path: directory to write files to
    :param border_threshold: threshold for border detection ranging from 0 to 255 [30]
    :return: x_min, x_max, y_min, y_max coordinates
    """
    slide_name = he_slide.image_name
    openslide_obj = he_slide.openslide_obj

    # Get image at lowest resolution for faster processing; openslide.read_region returns img in RGBA format
    level = openslide_obj.level_count - 1
    rgba_img = openslide_obj.read_region((0, 0), level, openslide_obj.level_dimensions[level])
    
    # Convert image to 2D array (dimensions of array equal to dimensions of image) and remove alpha channel (RGBA > RGB), 
    # then enhance pink and dark pixels
    rgba_img_arr = np.array(rgba_img)
    enhanced_rgb_img_arr = enhance_sv_channels(cv2.cvtColor(rgba_img_arr, cv2.COLOR_RGBA2RGB))
    cv2.imwrite(f'{out_path}/{slide_name}.tissue-enhanced.png', enhanced_rgb_img_arr)

    # Calculate otsu threshold: threshold that differentiates tissue from background
    # Pixels with value higher than otsu_threshold assumed to be tissue
    tissue_mask, otsu_threshold = he_otsu(enhanced_rgb_img_arr)
    sys.stderr.write(f'\t{he_slide.image_name} with otsu threshold={otsu_threshold}\n')
    sys.stderr.flush()

    # Define polygon around tissue pixels
    x_coords, y_coords = define_polygon_roi(tissue_mask)

    # Save annotated images
    ImageDraw.Draw(rgba_img).polygon(list(zip(x_coords, y_coords)), outline='black', fill=None)
    rgba_img.save(f'{out_path}/{slide_name}.polyROI-annotated.png')

    # Convert back to lvl0 coordinates
    return convert_to_original_coordinates(calc_coord_downsample_factors(openslide_obj, level), x_coords, y_coords)


def main():
    parser = argparse.ArgumentParser("Pre-process H&E slide images (*.svs) in given directory.")
    parser.add_argument('-he', '--he_path',
                        help='Path to one or multiple H&E slide images (*.svs).',
                        required=True)
    parser.add_argument('-o', '--out_path',
                        help='Path to write output files to.',
                        required=True)

    # Parse and check validity of args
    args = parser.parse_args()
    he_path = args.he_path
    out_path = args.out_path

    # Find all slide images in he_path and load
    sys.stderr.write(
        strftime("%Y-%m-%d %H:%M:%S") + f': Loading all slide images found in {he_path}\n')
    sys.stderr.flush()
    all_slides = load_slides(he_path)
    sys.stderr.write(
        strftime("%Y-%m-%d %H:%M:%S") + f': {len(all_slides)} slide image(s) found!\n')
    sys.stderr.flush()

    # Find bounding box that exclude black borders for each slide image
    sys.stderr.write(
        strftime("%Y-%m-%d %H:%M:%S") + f': Looking for artifacts in...\n')
    sys.stderr.flush()
    with open(f'{out_path}/polyROI_coordinates.tsv', 'w') as outfile:
        outfile.write('file_path\tx_coords\ty_coords\n')  # write header

        for he_slide in all_slides:
            x_coords, y_coords = exclude_artifacts(he_slide, out_path)
            x_coords_str = [str(x) for x in x_coords]
            y_coords_str = [str(y) for y in y_coords]
            outfile.write(f'{he_slide.file_path}\t{','.join(x_coords_str)}\t{','.join(y_coords_str)}\n')

    sys.stderr.write(
        strftime("%Y-%m-%d %H:%M:%S") + f': Done!\n')
    sys.stderr.flush()


if __name__ == '__main__':
    main()

