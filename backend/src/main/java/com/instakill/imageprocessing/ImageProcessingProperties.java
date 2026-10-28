package com.instakill.imageprocessing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "image-processing")
public record ImageProcessingProperties(
        @DefaultValue("300") int maxWidth,
        @DefaultValue("Instakill Demo") String watermarkText
) {
}
