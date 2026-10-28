package com.instakill.imageprocessing;

import com.instakill.common.error.BadRequestException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Java2dImageProcessorTest {

    private static final Color BACKGROUND = new Color(30, 90, 160);

    private final Java2dImageProcessor processor =
            new Java2dImageProcessor(new ImageProcessingProperties(300, "Instakill Demo"));

    @Test
    void scalesWideImageToMaxWidthPreservingAspectRatio() throws IOException {
        BufferedImage result = process(solidImage(1200, 600), "png");

        assertThat(result.getWidth()).isEqualTo(300);
        assertThat(result.getHeight()).isEqualTo(150);
    }

    @Test
    void doesNotUpscaleSmallImages() throws IOException {
        BufferedImage result = process(solidImage(200, 100), "png");

        assertThat(result.getWidth()).isEqualTo(200);
        assertThat(result.getHeight()).isEqualTo(100);
    }

    @Test
    void drawsWatermarkInBottomRightCornerOnly() throws IOException {
        BufferedImage result = process(solidImage(600, 400), "png");

        assertThat(countChangedPixels(result, 150, 150, 150, 50)).isPositive();
        assertThat(countChangedPixels(result, 0, 0, 150, 100)).isZero();
    }

    @Test
    void shrinksWatermarkToFitOnNarrowImages() throws IOException {
        BufferedImage result = process(solidImage(60, 40), "png");

        assertThat(countChangedPixels(result, 0, 0, 8, 40)).isZero();
        assertThat(countChangedPixels(result, 8, 0, 52, 40)).isPositive();
    }

    @Test
    void handlesJpeg() throws IOException {
        BufferedImage result = process(solidImage(900, 300), "jpg");

        assertThat(result.getWidth()).isEqualTo(300);
        assertThat(result.getHeight()).isEqualTo(100);
    }

    @Test
    void rejectsBytesThatAreNotAnImage() {
        assertThatThrownBy(() -> processor.process(new ByteArrayInputStream(new byte[]{1, 2, 3}), "png"))
                .isInstanceOf(BadRequestException.class);
    }

    private BufferedImage process(BufferedImage source, String format) throws IOException {
        byte[] output = processor.process(new ByteArrayInputStream(encode(source, format)), format);
        return ImageIO.read(new ByteArrayInputStream(output));
    }

    static BufferedImage solidImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    /** Counts pixels in the [x, x+w) × [y, y+h) region that differ noticeably from the background. */
    private static int countChangedPixels(BufferedImage image, int x, int y, int w, int h) {
        int changed = 0;
        for (int px = x; px < x + w; px++) {
            for (int py = y; py < y + h; py++) {
                Color c = new Color(image.getRGB(px, py));
                int diff = Math.abs(c.getRed() - BACKGROUND.getRed())
                        + Math.abs(c.getGreen() - BACKGROUND.getGreen())
                        + Math.abs(c.getBlue() - BACKGROUND.getBlue());
                if (diff > 30) {
                    changed++;
                }
            }
        }
        return changed;
    }
}
