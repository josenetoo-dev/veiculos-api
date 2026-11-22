package com.josenetoo_dev.veiculos_api.staging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josenetoo_dev.veiculos_api.enums.TipoFoto;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.AnuncioFoto;
import com.josenetoo_dev.veiculos_api.repository.AnuncioFotoRepository;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.service.ImageStorage;
import com.josenetoo_dev.veiculos_api.service.ImageValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Substitui ilustrações DEMO por fotografias reais de REFERÊNCIA DOS MODELOS,
 * não dos veículos individuais. Fontes selecionadas no Wikimedia Commons.
 * Requer opt-in STAGING_REAL_PHOTOS_ENABLED=true; nunca ativa no profile prod.
 *
 * Falha de download não interrompe inicialização, não apaga imagem anterior.
 * Cuidado: o autor, a licença e a URL original são recuperados no Wikimedia API.
 */
@Component
@Profile("staging & !prod")
@Order(20)
@RequiredArgsConstructor
public class StagingRealPhotoImporter implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(StagingRealPhotoImporter.class);
    private static final String[] FILES = {
        "2022 Chevrolet Onix 1.2 LT hatchback.jpg",
        "2019-2022 Toyota Corolla.jpg",
        "2023 Hyundai HB20 1.0 T-GDi Platinum Plus (Brazil) front view.png",
        "2020 Volkswagen Gol 1.6 Trendline.jpg",
        "Fiat Argo.jpg",
        "2023 Volkswagen Polo 1.6 Track, 1st edition.jpg",
        "Fiat Mobi.jpg",
        "Renault Kwid 1.0 Outsider 2022.jpg",
        "Peugeot 208 (2022).jpg",
        "2019 Citroën C3 in Cumulus Grey, front left, 06-08-2025.jpg",
        "Honda Civic 2.0 EX 2021 (52091298297).jpg",
        "Versa2021p1.jpg",
        "0 Chevrolet Cruze (second generation) 1.jpg",
        "Toyota Yaris S (Brazil).jpg",
        "Hyundai Creta 1.5 GLS 2023.jpg",
        "Volkswagen T-Cross (2023) 1X7A2500.jpg",
        "Brazilian Jeep Renegade.jpg",
        "Jeep Compass 193727.jpg",
        "Chevrolet Tracker Turbo LS 2023.jpg",
        "Honda HR-V (2024) (53970199505).jpg",
        "Nissan Kicks 1.6 Sense 2019.jpg",
        "Renault Duster 1.6 Zen 2022 (53699304515).jpg",
        "Fiat Pulse 1.3 Drive 2023.jpg",
        "Fiat Fastback.jpg",
        "Toyota Corolla Cross SE-G 2023.jpg",
        "Volkswagen Nivus Brazil front.jpg",
        "2020 Ford Ka 1.5 SEL, rear view (Brazil).jpg",
        "Ford EcoSport 1.0 EcoBoost (2019) (53333797561).jpg",
        "Chevrolet Spin 1.8 Premier 2022.jpg",
        "2022 Fiat Cronos 1.3 GSE Drive.jpg"
    };

    private final AnuncioRepository ads;
    private final AnuncioFotoRepository photos;
    private final ImageStorage storage;
    private final TransactionTemplate transactions;
    private final ObjectMapper mapper = new ObjectMapper();
    @Value("${STAGING_REAL_PHOTOS_ENABLED:false}")
    private boolean enabled;

    private record RealImage(byte[] jpeg, String source, String credit, String license) {}

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(6)).build();
        int updated = 0;
        for (int group = 0; group < FILES.length; group++) {
            RealImage image;
            try {
                image = fetch(http, FILES[group]);
            } catch (Exception e) {
                LOG.warn("Foto de referência indisponível para modelo DEMO grupo {}: {}", group + 1, e.toString());
                continue;
            }
            for (int variation = 0; variation < 5; variation++) {
                String code = String.format(Locale.ROOT, "DEMO-%03d", variation * FILES.length + group + 1);
                try {
                    Boolean changed = transactions.execute(status -> replace(code, image));
                    if (Boolean.TRUE.equals(changed)) updated++;
                } catch (Exception e) {
                    LOG.warn("Falha ao atualizar foto real de {}: {}", code, e.toString());
                }
            }
        }
        LOG.info("Fotos demonstrativas reais: {} anúncios atualizados; máximo possível 150", updated);
    }

    private boolean replace(String code, RealImage image) {
        Anuncio ad = ads.findByCodigo(code).orElse(null);
        if (ad == null) return false;
        var previous = photos.findByAnuncioId(ad.getId(), Pageable.unpaged()).getContent();
        if (ad.getDescricao() != null && ad.getDescricao().startsWith("ANÚNCIO FICTÍCIO — FOTO REAL DE REFERÊNCIA")
                && previous.size() == 1) return false;
        AnuncioFoto photo = new AnuncioFoto();
        photo.setAnuncio(ad);
        photo.setOrdem(0);
        photo.setTipoFoto(TipoFoto.OUTRO);
        photo.setUrl(storage.save(new ImageValidator.ValidatedImage(image.jpeg(), "jpg")));
        photos.saveAndFlush(photo);
        // Foto real é representativa do modelo, NÃO do ano, versão, cor ou unidade anunciados.
        String description = "ANÚNCIO FICTÍCIO — FOTO REAL DE REFERÊNCIA do modelo, não do veículo à venda. " +
                "Foto: " + image.credit() + " (" + image.license() + "), redimensionada. " +
                "Fonte: " + image.source();
        ad.setDescricao(description.substring(0, Math.min(255, description.length())));
        ads.save(ad);
        for (AnuncioFoto old : previous) {
            photos.delete(old);
            storage.deleteAfterCommit(old.getUrl());
        }
        photos.flush();
        return true;
    }

    private RealImage fetch(HttpClient http, String filename) throws IOException, InterruptedException {
        String title = "File:" + filename;
        String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
        URI api = URI.create("https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2" +
                "&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=960&titles=" + encoded);
        HttpRequest metadataRequest = request(api);
        HttpResponse<String> response = http.send(metadataRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Wikimedia API: HTTP " + response.statusCode());
        JsonNode page = mapper.readTree(response.body()).path("query").path("pages").path(0);
        if (page.has("missing")) throw new IOException("Arquivo Wikimedia inexistente: " + filename);
        JsonNode info = page.path("imageinfo").path(0);
        JsonNode ext = info.path("extmetadata");
        String license = ext.path("LicenseShortName").path("value").asText();
        if (!(license.startsWith("CC BY ") || license.startsWith("CC BY-SA ") ||
                license.equals("CC0") || license.equals("Public domain"))) {
            throw new IOException("Licença não liberada para importação: " + license);
        }
        String artist = ext.path("Artist").path("value").asText("Autor não identificado");
        artist = artist.replaceAll("<[^>]*>", " ").replaceAll("&[^;]{1,12};", " ")
                .replaceAll("\\s+", " ").trim();
        if (artist.isBlank()) artist = "Wikimedia Commons";
        if (artist.length() > 42) artist = artist.substring(0, 42);
        String url = info.path("thumburl").asText(info.path("url").asText());
        URI media = URI.create(url);
        if (!"https".equals(media.getScheme()) || !"upload.wikimedia.org".equals(media.getHost())) {
            throw new IOException("Host de imagem inesperado: " + media.getHost() + "; Wikimedia JSON: " + response.body().substring(0, Math.min(200, response.body().length())));
        }
        HttpResponse<byte[]> file = http.send(request(media), HttpResponse.BodyHandlers.ofByteArray());
        if (file.statusCode() != 200 || file.body().length > 15_000_000) {
            throw new IOException("Download Wikimedia inválido: " + file.statusCode());
        }
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(file.body()));
        if (decoded == null || decoded.getWidth() > 7000 || decoded.getHeight() > 7000)
            throw new IOException("Arquivo de imagem inválido");
        int w = Math.min(decoded.getWidth(), 960);
        int h = Math.max(1, (int) Math.round(decoded.getHeight() * (w / (double) decoded.getWidth())));
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, w, h);
            graphics.drawImage(decoded, 0, 0, w, h, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        if (!ImageIO.write(output, "jpeg", jpeg) || jpeg.size() > ImageValidator.MAX_BYTES)
            throw new IOException("Falha ao converter imagem JPEG");
        String source = "https://commons.wikimedia.org/?curid=" + page.path("pageid").asLong();
        return new RealImage(jpeg.toByteArray(), source, artist, license);
    }

    private HttpRequest request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12))
                .header("User-Agent", "AutoMinasDemoPhotoImporter/1.0 (https://github.com/josenetoo-dev/veiculos-api)")
                .GET().build();
    }
}
