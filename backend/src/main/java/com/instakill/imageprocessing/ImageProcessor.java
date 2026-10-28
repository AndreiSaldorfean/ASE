package com.instakill.imageprocessing;

import java.io.InputStream;

/**
 * Transforms an uploaded image before it is stored.
 * Kept free of web concerns so it could later run in a separate worker (e.g. behind a message queue).
 */
public interface ImageProcessor {

    /**
     * @param in     the original image bytes
     * @param format the output format, e.g. "png" or "jpg"
     * @return the processed image encoded in {@code format}
     */
    byte[] process(InputStream in, String format);
}
