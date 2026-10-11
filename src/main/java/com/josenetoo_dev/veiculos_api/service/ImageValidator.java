package com.josenetoo_dev.veiculos_api.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Component
public class ImageValidator {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public record ValidatedImage(byte[] bytes, String extension) {}

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw invalid();
        }
        String name = file.getOriginalFilename();
        if (name == null || name.length() > 255 || name.contains("/") || name.contains("\\") || name.indexOf('\0') >= 0) {
            throw invalid();
        }
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String format;
        if ((extension.equals("jpg") || extension.equals("jpeg")) && "image/jpeg".equals(file.getContentType())) format = "jpeg";
        else if (extension.equals("png") && "image/png".equals(file.getContentType())) format = "png";
        else throw invalid(); // WebP/SVG/GIF não são aceitos sem codec seguro de leitura e escrita.

        try (var stream = file.getInputStream()) {
            byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
            if (bytes.length == 0 || bytes.length > MAX_BYTES || !signatureMatches(bytes, format)) throw invalid();
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalid();
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    if (!reader.getFormatName().equalsIgnoreCase(format)) throw invalid();
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 6000 || height > 6000 || (long) width * height > 20_000_000) throw invalid();
                    BufferedImage image = reader.read(0);
                    if (image == null) throw invalid();
                    // Escrita sem metadata: bytes anexados, EXIF e estruturas originais não são publicados.
                    var output = new ByteArrayOutputStream();
                    if (!ImageIO.write(image, format, output) || output.size() > MAX_BYTES) throw invalid();
                    return new ValidatedImage(output.toByteArray(), format.equals("jpeg") ? "jpg" : "png");
                } finally { reader.dispose(); }
            }
        } catch (IOException | RuntimeException e) {
            throw invalid();
        }
    }

    private boolean signatureMatches(byte[] bytes, String format) {
        if (format.equals("jpeg")) return bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255;
        byte[] signature = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        return bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), signature);
    }
    private IllegalArgumentException invalid() { return new IllegalArgumentException("Imagem inválida: envie JPEG ou PNG de até 5 MiB dentro dos limites de dimensão"); }
}
