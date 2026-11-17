# Lossless Qist artwork

Both Android applications use Android vector XML drawables for scalable icons and
adaptive icon components. The customer app also has density-specific **PNG**
launcher fallbacks, and the Play Store artwork is **512 × 512 PNG**.

All 12 committed raster artwork files were checked as PNGs (not JPEG/WebP files
renamed to PNG). PNG retains its encoded pixels exactly; unlike vector artwork,
enlarging a small PNG cannot recreate additional detail. We therefore preserve
the existing launcher resolutions and visual identity rather than resaving,
upscaling, tracing, or converting assets just for the sake of conversion.

Run `python3 qa/verify_lossless_assets.py` from any directory inside the checkout.
The check validates PNG chunk integrity and compressed data, validates vector
resource XML, and rejects lossy-capable image extensions. CI runs it before
compiling the customer application. If any future photographic artwork must use
another format, document its fidelity requirements and extend the validator
deliberately rather than silently introducing lossy files.
