#!/usr/bin/env python3
"""
Helper script to convert manually defined bounding polygons exported as geoJSON files to
an appropriate input format (polyROI_coordinates.tsv) for SHADE.

Written by Theodora Lo (@theottlo)
"""

import argparse
import sys
from time import strftime
import re
from pathlib import Path
import json


def extract_polygon_roi_coords(file_path, geojson_feature_collection):
    bounding_polygon = None
    for feature in geojson_feature_collection['features']:
        geometry = feature.get('geometry', {})
        if geometry.get('type') == 'Polygon':
            if bounding_polygon is None:
                bounding_polygon = geometry
            else:
                sys.stderr.write(
                    strftime("%Y-%m-%d %H:%M:%S") + f": [WARN] Multiple polygon features detected for {file_path} - "
                                                    f"using first polygon as bounding polygon.\n")
                sys.stderr.flush()
                break

    if bounding_polygon is None:
        sys.stderr.write(
            strftime("%Y-%m-%d %H:%M:%S") + f": [WARN] No polygon features detected for {file_path}... skipping.\n")
        sys.stderr.flush()

    return bounding_polygon


def parse_geojson(file_path, geojson_path):
    with open(geojson_path, 'r') as geojson:
        feature_collection = json.load(geojson)

        # Retrieve polygon feature - corresponds to manually defined bounding polygon
        polygon_obj = extract_polygon_roi_coords(file_path, feature_collection)

        x_coords, y_coords = [], []
        if polygon_obj is not None:
            polygon_outer_ring = polygon_obj['coordinates'][0]  # ensure that inner holes are not included
            x_coords, y_coords = zip(*polygon_outer_ring)
        return list(x_coords), list(y_coords)


def main():
    parser = argparse.ArgumentParser("Split x and y polygon coordinates in geoJSON file: "
                                     "[(x1,y1), (x2,y2)...] to [x1,x2,...] and [y1,y2,...]. ")
    parser.add_argument('-i', '--input',
                        help='Tab-delimited samplesheet, where the first column column contains absolute paths to SVS '
                             'images and the second column contains absolute paths to the geoJSON file with bounding '
                             'polygon coordinates.',
                        required=True)

    # Parse and check validity of args
    args = parser.parse_args()
    samplesheet_path = args.input

    # Iterate through each slide image and corresponding geoJSON file
    sys.stderr.write(
        strftime("%Y-%m-%d %H:%M:%S") + f": [INFO] Parsing geoJSON files provided in {samplesheet_path}.\n")
    sys.stderr.flush()

    out_polyroi_tsv = open('polyROI_coordinates.tsv', 'w')
    out_polyroi_tsv.write('file_path\tx_coords\ty_coords\n')  # write header
    with open(samplesheet_path, 'r') as samplesheet:
        for line in samplesheet:
            #fields = line.split('\t')
            fields = re.split(r'[\t,]', line.strip())
            file_path = fields[0]
            geojson_path = fields[1]

            if Path(file_path).exists() and Path(geojson_path).exists():
                polyroi_x_coords, polyroi_y_coords = parse_geojson(file_path, geojson_path)

                if polyroi_x_coords and polyroi_y_coords:
                    out_polyroi_tsv.write(f"{file_path}\t{','.join(str(x) for x in polyroi_x_coords)}\t"
                                          f"{','.join(str(y) for y in polyroi_y_coords)}\n")
            else:
                sys.stderr.write(
                    strftime("%Y-%m-%d %H:%M:%S") + f": [WARN] Either file_path ({file_path}) or geojson_path "
                                                    f"{geojson_path} does not exist... skipping \n")
                sys.stderr.flush()

    out_polyroi_tsv.close()

    sys.stderr.write(
        strftime("%Y-%m-%d %H:%M:%S") + f': [INFO] Done!\n')
    sys.stderr.flush()


if __name__ == '__main__':
    main()

