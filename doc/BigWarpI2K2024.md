# Deformable 2D and 3D big data image registration and transformation with BigWarp 

I2K 2024 From Images to Knowledge<br>
Milan, Italy<br>
Oct 24, 2024 9:45 AM<br>

* [Workshop page](https://events.humantechnopole.it/event/1/contributions/42/)


> BigWarp is an intuitive tool for non-linear manual image registration that can scale to terabyte-sized image data. In this
> workshop, participants will learn to perform image registration with BigWarp, apply transformations to large images, import and
> export transformations to and from other tools, and fine-tune the results of automatic registration algorithms. BigWarp makes
> heavy use of the N5-API to store and load large image and transformation data and meta-data using the current NGFF formats HDF5,
> Zarr, and N5. In addition to basic usage, the concepts, tips, and best practices discussed will extend to other registration
> tools and help users achieve practical success for realistic and challenging data. Users will also get an introduction into an
> excellent use case for OME-NGFF.

## Prerequisites

* [Download Future Fiji for your platform](https://downloads.imagej.net/fiji/future/)
* If necessary download [bigwarp 9.3.0](https://maven.scijava.org/service/local/repositories/releases/content/sc/fiji/bigwarp_fiji/9.3.0/bigwarp_fiji-9.3.0.jar)
* Download the sample data sets
    * [xray data (medium 1.4G)](https://figshare.com/s/408097a1651139088651)
    * [xray data (small 80M)](https://figshare.com/s/f7b2bdb6492678114fe4)
    * [CLEM samples](https://figshare.com/s/442b7686fe02a0839ef7)
    * [bigwarp supplementary files](https://figshare.com/s/4d95d3cf9ed9be123c1a)

### About this sample data

>  Kidney tissue from a wild-type, postnatal day 7 mouse, strain: C57BL/6J from Jackson Lab

Details can be found [on open organelle](https://openorganelle.janelia.org/datasets/jrc_mus-kidney-3)

* An xray of the sample was imaged (`xray/xray-2`)
* The sample was trimmed and imaged again (`xray/xray-3`)
* The sample was trimmed for a final time and imaged again (`xray/xray-1`)
* The sample was imaged using FIB-SEM (`em/fibsem-uint8`)

All data are stored using [OME-Zarr](https://ngff.openmicroscopy.org/0.4/).


## Running BigWarp

* Modern: `Plugins > BigDavaViewer > Big Warp Command`
    * This is what we'll use in this workshop
* Legacy: `Plugins > BigDavaViewer > Big Warp`

## Outline


### Masked transformations

* From the `clemSampleData`, open `em.tif` and `PALM_532nm_lo.tif` images with Fiji.
* Run BigWarp using EM as the fixed image and the PALM as the moving image.
* Notice a mis-aligned mitochondrion near the "top" of the image
* Click some landmarks around that mito only
* Notice that the rest of the image is now mis-aligned
* Turn on "masked" transformations
* Press `M` to edit the mask


## Export a displacement field

* `File > Export transformation`
* select `Moving (warped)` as `reference`
* Specify a Root folder
    * Recommended: Make a folder called `tforms.zarr` in the same folder as the clem sample data.
* Specify a new Root folder
    * Recommended: Make a folder called `tforms_small.zarr` in the same folder as the clem sample data.
* Open the result in Fiji with `Plugins > Transform > Read Displacement Field` 
    * What do you notice?

Let's do it again but with a different field of view:

* `File > Export transformation`
* select `Specified` as `reference`
* Specify a Root folder
    * Recommended: Make a folder called `tforms_new.zarr` in the same folder as the clem sample data.
* Use these settings for min and size

```
min(nm)     size(nm)
  29000   	  8000
   7000	      7000
   2500	      2500

```

* Open the result in Fiji with `Plugins > Transform > Read Displacement Field` 
    * What do you notice?

