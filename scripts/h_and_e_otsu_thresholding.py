"""
Code for H&E Otsu Thresholding.
Taken from: https://gitlab.developers.cam.ac.uk/bas43/h_and_e_otsu_thresholding.
"""

from typing import Union, Tuple
import numpy as np
from skimage.filters import threshold_otsu # pylint: disable=no-name-in-module
from scipy.signal import convolve2d

def he_otsu(
    image: np.ndarray,
    blur_kernel_width: int = 0,
    nbins: int = 256,
    hist: Union[np.ndarray, Tuple[np.ndarray, np.ndarray]] = None
) -> Tuple[np.ndarray, float]:
    """Calculates the H&E Otsu threshold.

    Args:
        image (np.ndarray): An RGB image
        blur_kernel_width (int): Blurs the mask with the given blur kernel
        nbins (int, optional): Number of bins used to calculate histogram.
                            This value is ignored for integer arrays.
                            Defaults to 256.
        hist (Union[np.ndarray, Tuple[np.ndarray,np.ndarray]], optional):
                            Histogram from which to determine the threshold, and optionally a
                            corresponding array of bin center intensities. If no hist provided,
                            this function will compute it from the image. Default to None.

    Returns:
        Tuple[np.ndarray, float]: Tissue mask and upper threshold value. All pixels with an
        intensity higher than this value are assumed to be tissue.
    """

    red_channel = image[:, :, 0].astype(float)
    green_channel = image[:, :, 1].astype(float)
    blue_channel = image[:, :, 2].astype(float)

    red_to_green_mask = np.maximum(red_channel - green_channel, 0)
    blue_to_green_mask = np.maximum(blue_channel - green_channel, 0)

    tissue_heatmap = red_to_green_mask * blue_to_green_mask

    threshold = threshold_otsu(
        red_to_green_mask * blue_to_green_mask, nbins=nbins, hist=hist
    )

    mask = tissue_heatmap > threshold

    if blur_kernel_width != 0:
        blur_kernel = np.ones((blur_kernel_width, blur_kernel_width))
        mask = convolve2d(mask, blur_kernel, mode = "same")
        mask = mask > 0
        # mask = mask > 0

    return mask, threshold
