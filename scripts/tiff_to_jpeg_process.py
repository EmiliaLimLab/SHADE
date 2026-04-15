import tifffile as tiff
from PIL import Image
import numpy as np
import os
import sys
from time import strftime
import cv2
import argparse

# execute command with python3 tiff_to_jpeg_process.py in correct directory
#level_index = -2

#manual downsampling
def manual_downsample(arr, factor=4):
    """
    Manually downsamples a NumPy RGB image array using OpenCV.
    """
    h, w = arr.shape[:2]
    new_h = max(1, h // factor)
    new_w = max(1, w // factor)
    return cv2.resize(arr, (new_w, new_h), interpolation=cv2.INTER_AREA)

def find_tiff_files(path, recursive=True):
    """
    Return a list of TIFF/OME-TIFF file paths.
    If path is a file, return [path]. If path is a directory, search it
    (recursively by default) for .tiff/.ome.tiff files.
    """
    exts = ('.tiff', '.tif', '.ome.tiff', '.ome.tif')
    path = os.path.abspath(path)

    if os.path.isfile(path):
        if path.lower().endswith(exts):
            return [path]
        return []

    matches = []
    if recursive:
        for root, _, files in os.walk(path):
            for f in files:
                if f.lower().endswith(exts):
                    matches.append(os.path.join(root, f))
    else:
        for f in os.listdir(path):
            if f.lower().endswith(exts):
                matches.append(os.path.join(path, f))

    return matches

def convert_tiff_files(file_list, output_dir, downsample_level_index=-2):
    os.makedirs(output_dir, exist_ok=True)
    for tiff_file_path in file_list:
        filename = os.path.basename(tiff_file_path)
        jpeg_file_path = os.path.join(
            output_dir,
            os.path.splitext(filename)[0] + '.jpg'
        )

        print(f"\n Processing {filename} . . .")
        try:
            with tiff.TiffFile(tiff_file_path) as tif:
                series = tif.series[0]
                if len(series.levels) > 1:
                    lvl = max(0, len(series.levels) + downsample_level_index)
                    print(f"🪶 Using downsampled level {lvl} of {len(series.levels)}")
                    img = series.levels[lvl].asarray()
                else:
                    print("⚠️ No pyramid levels found, reading full image (may be large!)")
                    img = series.asarray()
                    if img.dtype != np.uint8:
                        img = (img / img.max() * 255).astype(np.uint8)
                    print("Performing manual downsampling (factor = 4)")
                    img = manual_downsample(img, factor=4)

            pil_image = Image.fromarray(img)
            rgb_image = pil_image.convert("RGB")
            rgb_image.save(jpeg_file_path, format='JPEG')
            print(f"Saved: {jpeg_file_path}")

        except Exception as e:
            print(f" Failed to process {filename}: {e}", file=sys.stderr)

#image processing
class Jpeg:
    def __init__(self, file_path:None, image_obj:None):
        """
        Wrapper for jpeg images
        :param file_path: Full path to image
        :param image_obj: Image object (PIL.Image)
        """
        if image_obj is not None:
            self.file_path = file_path            
            self.image_name = os.path.splitext(os.path.basename(file_path))[0]
            self.image_obj = image_obj
        else:
            sys.stderr.write(
                strftime("%Y-%m-%d %H:%M:%S") + f": Invalid image object for {file_path}, skipping.\n")
            sys.stderr.flush()

##Load slides
def load_slides(output_dir):
    all_slides = []
    supported_ext = ('.jpg', '.jpeg')

    for file in os.listdir(output_dir):
        if file.lower().endswith(supported_ext):
            file_path = os.path.join(output_dir, file)

            try:
                # Load JPEG with PIL 
                pil_img = Image.open(file_path).convert("RGB")
                all_slides.append(Jpeg(file_path, pil_img))
            except Exception as e:
                sys.stderr.write(strftime("%Y-%m-%d %H:%M:%S") + f": Failed to open {file_path} ({e})\n")
                sys.stderr.flush()
        else:
            sys.stderr.write(strftime("%Y-%m-%d %H:%M:%S") + f": Skipping non-JPEG file {file}\n")
            sys.stderr.flush()

    return all_slides

def enhance_sv_channels(rgb_img_arr, 
                        lower_yellow=(20, 60, 100),
                        upper_yellow=(40, 255, 255),
                        background_gray=200,
                        alpha_bg=100,
                        alpha_fg=255,
                        bg_mix_ratio=0.6):
    """
    Mask yellow highlights with red and replace everything else with semi-transparent grey background
    :param rgb_img: 2D array representation of image in RGB format
    :return: 2D array representation of enhanced RGB image
    """
    # Convert RGB image to HSV, then save as array (dimensions of array equal to dimensions of image)
    hsv = cv2.cvtColor(rgb_img_arr, cv2.COLOR_RGB2HSV)

    #boost yellows saturation and suppress others
    mask = cv2.inRange(hsv, np.array(lower_yellow), np.array(upper_yellow))
    mask = cv2.medianBlur(mask, 7)
    #change into rgba image to add transparency effect
    rgba_output = np.zeros((*rgb_img_arr.shape[:2], 4), dtype=np.uint8)
    blended_bg = (bg_mix_ratio * rgb_img_arr + 
                  (1 - bg_mix_ratio) * np.array(background_gray)).astype(np.uint8)
    #background turn to gray and semi-transparent
    rgba_output[:, :, :3] = blended_bg
    rgba_output[:, :, 3] = alpha_bg
    #add red mask on top of the yellow highlights
    rgba_output[mask > 0, :3] = [220, 20, 60]
    rgba_output[mask > 0, 3] = alpha_fg

    return rgba_output

def main():
    parser = argparse.ArgumentParser(description="Convert OME-TIFF/TIFF to JPEG and optionally enhance.")
    parser.add_argument("-i", "--input", default=os.getcwd(),
                        help="Input file or directory to search for TIFF files (default: current dir).")
    parser.add_argument("-o", "--output", default=None,
                        help="Output directory. If omitted, 'jpeg_output' inside input dir is used.")
    parser.add_argument("-n", "--no-recursive", action="store_true",
                        help="Do not search directories recursively (only top-level).")
    parser.add_argument("--downsample-level-index", type=int, default=-2,
                        help="Pyramid level index to use (negative indexes count from end). Default -2.")
    args = parser.parse_args()

    input_path = args.input
    recursive = not args.no_recursive

    files = find_tiff_files(input_path, recursive=recursive)
    if not files:
        sys.stderr.write(strftime("%Y-%m-%d %H:%M:%S") + f": No TIFF files found under {input_path}\n")
        sys.exit(1)

    # Determine output directories
    # If user provided -o use that, else place jpeg_output next to provided input (if input is file use its parent)
    if args.output:
        jpeg_output = args.output
    else:
        base_dir = input_path if os.path.isdir(input_path) else os.path.dirname(input_path)
        jpeg_output = os.path.join(base_dir, 'jpeg_output')

    convert_tiff_files(files, jpeg_output, downsample_level_index=args.downsample_level_index)

    # After conversion, continue with existing enhancement pipeline (unchanged)
    jpeg_path = jpeg_output
    output_path = os.path.join(os.path.dirname(jpeg_path), 'enhanced_output')
    os.makedirs(output_path, exist_ok=True)

    sys.stderr.write(
        strftime("%Y-%m-%d %H:%M:%S") + f': Loading all slide images found in {jpeg_path}\n')
    sys.stderr.flush()
    all_slides = load_slides(jpeg_path)
    sys.stderr.write(
        strftime("%Y-%m-%d %H:%M:%S") + f': {len(all_slides)} slide image(s) found!\n')
    sys.stderr.flush()

    for slide in all_slides:
        rgb_img_arr = np.array(slide.image_obj)
        enhanced_rgba = enhance_sv_channels(rgb_img_arr)
        save_path = os.path.join(output_path, slide.image_name + "_enhanced.png")
        Image.fromarray(enhanced_rgba, mode='RGBA').save(save_path)
        print(f"Saved enhanced image: {save_path}")

    sys.stderr.write(
        strftime("%Y-%m-%d %H:%M:%S") + f': Done!\n')
    sys.stderr.flush()

if __name__ == '__main__':
    main()