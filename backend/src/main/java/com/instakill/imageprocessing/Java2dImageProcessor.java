package com.instakill.imageprocessing;

import com.instakill.common.error.BadRequestException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;

/**
 * Scales images down to a maximum width and stamps a watermark in the bottom-right corner,
 * using only the JDK's built-in imaging (ImageIO + Java2D).
 */
@Component
public class Java2dImageProcessor implements ImageProcessor {

    private static final int PADDING = 8;
    private static final int MIN_FONT_SIZE = 10;

    private final int maxWidth;
    private final String watermarkText;

    public Java2dImageProcessor(ImageProcessingProperties properties) {
        this.maxWidth = properties.maxWidth();
        this.watermarkText = properties.watermarkText();
    }

    @Override
    public byte[] process(InputStream in, String format) {
        BufferedImage source = read(in);
        boolean opaque = isJpeg(format);
        BufferedImage result = scale(source, opaque);
        drawWatermark(result);
        return write(result, format);
    }

    private BufferedImage read(InputStream in) {
        try {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new BadRequestException("Not a valid image");
            }
            return image;
        } catch (IOException e) {
            throw new BadRequestException("Not a valid image");
        }
    }

    /**
     * Returns a copy no wider than {@code maxWidth}, preserving aspect ratio. Never upscales.
     */
    private BufferedImage scale(BufferedImage source, boolean opaque) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width > maxWidth) {
            height = Math.max(1, Math.round((float) height * maxWidth / width));
            width = maxWidth;
        }
        // JPEG has no alpha channel, so draw onto an RGB canvas with a white background.
        BufferedImage target = new BufferedImage(width, height,
                opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (opaque) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
            }
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private void drawWatermark(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int fontSize = Math.max(MIN_FONT_SIZE, image.getWidth() / 20);
            int available = image.getWidth() - 2 * PADDING;
            // Very narrow images: shrink the text so all of it stays inside the image.
            while (fontSize > 1 && g.getFontMetrics(watermarkFont(fontSize)).stringWidth(watermarkText) > available) {
                fontSize--;
            }
            g.setFont(watermarkFont(fontSize));
            FontMetrics metrics = g.getFontMetrics();
            if (metrics.stringWidth(watermarkText) > available) {
                return; // too small for any readable watermark
            }
            int x = image.getWidth() - metrics.stringWidth(watermarkText) - PADDING;
            int y = image.getHeight() - metrics.getDescent() - PADDING;

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            // Dark shadow first so the white text stays readable on light images.
            g.setColor(Color.BLACK);
            g.drawString(watermarkText, x + 1, y + 1);
            g.setColor(Color.WHITE);
            g.drawString(watermarkText, x, y);
        } finally {
            g.dispose();
        }
    }

    private static Font watermarkFont(int size) {
        return new Font(Font.SANS_SERIF, Font.BOLD, size);
    }

    private byte[] write(BufferedImage image, String format) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, format, out)) {
                throw new BadRequestException("Unsupported image format: " + format);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not encode image", e);
        }
        return out.toByteArray();
    }

    private static boolean isJpeg(String format) {
        String f = format.toLowerCase(Locale.ROOT);
        return f.equals("jpg") || f.equals("jpeg");
    }
}
