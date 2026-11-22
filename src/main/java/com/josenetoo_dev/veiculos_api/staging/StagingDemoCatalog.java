package com.josenetoo_dev.veiculos_api.staging;

import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.model.*;
import com.josenetoo_dev.veiculos_api.repository.*;
import com.josenetoo_dev.veiculos_api.service.ImageStorage;
import com.josenetoo_dev.veiculos_api.service.ImageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Catálogo sintético para staging. Ilustrações originais, NÃO fotografias reais
 * dos modelos anunciados. Invocado apenas por StagingDemoData (profile staging).
 */
@Component
@RequiredArgsConstructor
public class StagingDemoCatalog {
    private final AnuncioRepository ads;
    private final AnuncioFotoRepository photos;
    private final ImageStorage imageStorage;

    private record Model(String marca, String modelo, String versao, int preco, boolean suv) {}

    private static final List<Model> MODELS = List.of(
        new Model("Chevrolet", "Onix", "LT 1.0", 64900, false),
        new Model("Toyota", "Corolla", "XEi 2.0", 109900, false),
        new Model("Hyundai", "HB20", "Comfort 1.0", 72900, false),
        new Model("Volkswagen", "Gol", "MPI 1.0", 47900, false),
        new Model("Fiat", "Argo", "Drive 1.3", 65900, false),
        new Model("Volkswagen", "Polo", "Track 1.0", 72900, false),
        new Model("Fiat", "Mobi", "Like 1.0", 47900, false),
        new Model("Renault", "Kwid", "Zen 1.0", 46900, false),
        new Model("Peugeot", "208", "Allure", 76900, false),
        new Model("Citroen", "C3", "Feel", 62900, false),
        new Model("Honda", "Civic", "EXL", 114900, false),
        new Model("Nissan", "Versa", "Advance", 89900, false),
        new Model("Chevrolet", "Cruze", "LT", 94900, false),
        new Model("Toyota", "Yaris", "XL", 81900, false),
        new Model("Hyundai", "Creta", "Comfort", 112900, true),
        new Model("Volkswagen", "T-Cross", "Sense", 114900, true),
        new Model("Jeep", "Renegade", "Sport", 98900, true),
        new Model("Jeep", "Compass", "Longitude", 149900, true),
        new Model("Chevrolet", "Tracker", "LT", 108900, true),
        new Model("Honda", "HR-V", "EX", 139900, true),
        new Model("Nissan", "Kicks", "Sense", 92900, true),
        new Model("Renault", "Duster", "Intense", 94900, true),
        new Model("Fiat", "Pulse", "Drive", 92900, true),
        new Model("Fiat", "Fastback", "Audace", 119900, true),
        new Model("Toyota", "Corolla Cross", "XRE", 159900, true),
        new Model("Volkswagen", "Nivus", "Comfortline", 112900, true),
        new Model("Ford", "Ka", "SE", 49900, false),
        new Model("Ford", "EcoSport", "SE", 69900, true),
        new Model("Chevrolet", "Spin", "LT", 82900, true),
        new Model("Fiat", "Cronos", "Drive", 74900, false)
    );

    private static final String[] COLORS = {"Branco", "Prata", "Preto", "Cinza", "Azul", "Vermelho"};
    private static final Color[] PAINTS = {
        new Color(228, 232, 237), new Color(173, 182, 192), new Color(48, 55, 65),
        new Color(116, 129, 141), new Color(45, 95, 161), new Color(180, 49, 55)
    };
    private static final String[] VIEWS = {"LATERAL", "DIANTEIRA", "TRASEIRA"};

    /** Chamar na transação do seeder; idempotente por código DEMO-NNN e três fotos por anúncio. */
    public void ensure(Usuario seller) {
        for (int index = 1; index <= 150; index++) {
            final String code = String.format(Locale.ROOT, "DEMO-%03d", index);
            Model model = MODELS.get((index - 1) % MODELS.size());
            final int ordinal = index;
            int variation = (index - 1) / MODELS.size();
            int year = 2020 + variation;
            String color = COLORS[(index - 1) % COLORS.length];
            Anuncio ad = ads.findByCodigo(code).orElseGet(() -> {
                Anuncio a = new Anuncio();
                a.setCodigo(code);
                a.setUsuario(seller);
                a.setTitulo("[DEMO] " + model.marca + " " + model.modelo + " " + year);
                a.setDescricao("ANÚNCIO FICTÍCIO — NÃO ESTÁ À VENDA. Imagens são ilustrações " +
                    "originais genéricas, não fotografias do modelo. Dados e valores ilustrativos.");
                a.setMarca(model.marca);
                a.setModelo(model.modelo);
                a.setVersao(model.versao);
                a.setAno(year);
                a.setQuilometragem(12000 + (ordinal * 733) % 89000);
                a.setCor(color);
                a.setCidade("Várzea da Palma");
                a.setUf("MG");
                a.setCombustivel(TipoCombustivel.FLEX);
                a.setCambio(ordinal % 3 == 0 ? Cambio.MANUAL : Cambio.AUTOMATICO);
                a.setPreco(BigDecimal.valueOf(model.preco + variation * 2200));
                a.setSegundaMao(true);
                a.setCategoria(Categoria.SEMINOVOS);
                a.setDocumentacao("Demonstração — não verificada");
                a.setGarantia("Não se aplica: anúncio fictício");
                a.setStatus(StatusAnuncio.ATIVO);
                a.setDestaque(false);
                a.sincronizarVeiculo();
                return ads.saveAndFlush(a);
            });
            long existingPhotos = photos.countByAnuncioId(ad.getId());
            if (existingPhotos >= 3) continue;
            for (int view = (int) existingPhotos; view < 3; view++) {
                byte[] png = draw(model, year, PAINTS[(index - 1) % PAINTS.length], view);
                AnuncioFoto photo = new AnuncioFoto();
                photo.setAnuncio(ad);
                photo.setUrl(imageStorage.save(new ImageValidator.ValidatedImage(png, "png")));
                photo.setOrdem(view);
                photo.setTipoFoto(view == 0 ? TipoFoto.OUTRO : view == 1 ? TipoFoto.FRENTE : TipoFoto.TRASEIRA);
                photos.save(photo);
            }
        }
        photos.flush();
    }

    private static byte[] draw(Model model, int year, Color paint, int view) {
        BufferedImage img = new BufferedImage(960, 640, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(18, 27, 44), 0, 640, new Color(43, 56, 73)));
            g.fillRect(0, 0, 960, 640);
            g.setColor(new Color(255,255,255,16));
            for (int y=80; y<560; y+=48) g.drawLine(50,y,910,y);
            g.setColor(new Color(230,235,245,60));
            g.fill(new Ellipse2D.Double(120, 465, 720, 53));
            g.setColor(new Color(244, 193, 79));
            g.fillRoundRect(68, 60, 120, 8, 8, 8);

            if (view == 0) {
                int roof = model.suv ? 278 : 312;
                Path2D chassis = new Path2D.Double();
                chassis.moveTo(139, 400); chassis.curveTo(155, 365, 225, 347, 293, 345);
                chassis.lineTo(378, roof); chassis.lineTo(593, roof);
                chassis.lineTo(692, 351); chassis.curveTo(769, 357, 816, 382, 825, 414);
                chassis.lineTo(812, 448); chassis.lineTo(144, 448); chassis.closePath();
                g.setColor(paint); g.fill(chassis);
                g.setColor(new Color(6, 20, 36)); g.setStroke(new BasicStroke(5)); g.draw(chassis);
                Path2D glass = new Path2D.Double();
                glass.moveTo(320,342);glass.lineTo(392,roof+18);glass.lineTo(582,roof+18);
                glass.lineTo(665,350);glass.closePath();
                g.setColor(new Color(36, 68, 89));g.fill(glass);
                g.setColor(new Color(210,220,230,150));g.setStroke(new BasicStroke(3));g.draw(glass);
                g.setColor(new Color(25,32,42));g.fillOval(242,398,117,117);g.fillOval(642,398,117,117);
                g.setColor(new Color(200,209,217));g.fillOval(270,426,61,61);g.fillOval(670,426,61,61);
                g.setColor(new Color(255,241,198));g.fillRoundRect(800,388,23,14,4,4);
            } else {
                g.setColor(paint);
                g.fill(new RoundRectangle2D.Double(245, 280, 470, 180, 60, 60));
                g.fill(new RoundRectangle2D.Double(295, 235, 370, 100, 65, 65));
                g.setColor(new Color(37, 66, 87));g.fillRoundRect(310,247,340,69,26,26);
                g.setColor(new Color(28,34,42));g.fillRoundRect(282,398,396,45,20,20);
                g.setColor(new Color(232,240,244));g.fillOval(258,341,62,51);g.fillOval(640,341,62,51);
                if (view == 2) {g.setColor(new Color(193,44,52));g.fillRoundRect(258,342,72,40,8,8);g.fillRoundRect(630,342,72,40,8,8);}
                g.setColor(new Color(20,27,34));g.fillRoundRect(265,447,94,35,10,10);g.fillRoundRect(601,447,94,35,10,10);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 36)); g.setColor(Color.WHITE);
            g.drawString(model.marca + " " + model.modelo, 68, 112);
            g.setFont(new Font("SansSerif", Font.PLAIN, 23));
            g.setColor(new Color(216, 226, 238));
            g.drawString(year + "  •  " + VIEWS[view], 69, 149);
            g.setColor(new Color(255, 255, 255, 225));
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.drawString("IMAGEM ILUSTRATIVA — VEÍCULO FICTÍCIO", 165, 577);
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.drawString("AUTO MINAS • AMBIENTE DE DEMONSTRAÇÃO • NÃO É OFERTA REAL", 184, 606);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(img, "png", output)) throw new IllegalStateException("PNG encoder indisponível");
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar imagem demonstrativa", e);
        } finally { g.dispose(); }
    }
}
