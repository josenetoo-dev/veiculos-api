package com.josenetoo_dev.veiculos_api.staging;

import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DADOS FICTÍCIOS só para HOMOLOGAÇÃO. Nunca usar banco real nem perfil prod.
 * Habilitação DUPLA: @Profile(staging & !prod) e STAGING_DEMO_ENABLED=true.
 * Os anúncios de exemplo são marcados como ATIVO apenas para exercitar a vitrine,
 * não por terem sido verificados oficialmente. NÃO copiar este banco para produção.
 */
@Component
@Profile("staging & !prod")
@ConditionalOnProperty(name = "app.staging.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StagingDemoData implements ApplicationRunner {
    private final UsuarioRepository users;
    private final AnuncioRepository ads;
    private final PasswordEncoder encoder;

    @Value("${STAGING_DEMO_ADMIN_PASSWORD:}")
    private String adminPassword;
    @Value("${STAGING_DEMO_BUYER_PASSWORD:}")
    private String buyerPassword;

    private static final String ADMIN_EMAIL = "admin-demo@auto-minas.test";
    private static final String SELLER_EMAIL = "vendedor-demo@auto-minas.test";
    private static final String BUYER_EMAIL = "comprador-demo@auto-minas.test";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword.length() < 16 || buyerPassword.length() < 16) {
            throw new IllegalStateException("Credenciais de homologação não configuradas");
        }
        Usuario admin = createIfMissing(ADMIN_EMAIL, "Administração — Teste",
                adminPassword, Role.ADMIN);
        Usuario seller = createIfMissing(SELLER_EMAIL, "Vendedor de demonstração",
                UUID.randomUUID().toString(), Role.USER);
        createIfMissing(BUYER_EMAIL, "Comprador de teste",
                buyerPassword, Role.USER);

        // O site mostra claramente "ambiente de testes": são apenas carros fictícios.
        record DemoCar(String id, String brand, String model, String version, int year,
                int mileage, String color, TipoCombustivel fuel, Cambio transmission, long price) {}
        List<DemoCar> examples = List.of(
            new DemoCar("DEMO-001","Chevrolet","Onix","LT 1.0 Flex",2022,58400,
                "Branco",TipoCombustivel.FLEX,Cambio.MANUAL,64900),
            new DemoCar("DEMO-002","Toyota","Corolla","XEi 2.0",2021,41200,
                "Prata",TipoCombustivel.FLEX,Cambio.AUTOMATICO,109900),
            new DemoCar("DEMO-003","Hyundai","HB20","Comfort 1.0",2023,28600,
                "Preto",TipoCombustivel.FLEX,Cambio.MANUAL,72900),
            new DemoCar("DEMO-004","Volkswagen","Gol","1.0 MPI",2020,69700,
                "Cinza",TipoCombustivel.FLEX,Cambio.MANUAL,47900),
            new DemoCar("DEMO-005","Fiat","Argo","Drive 1.3",2022,34500,
                "Vermelho",TipoCombustivel.FLEX,Cambio.MANUAL,65900)
        );
        for (DemoCar v:examples) {
            if (ads.existsByCodigo(v.id())) continue;
            Anuncio ad = new Anuncio();
            ad.setCodigo(v.id());
            ad.setUsuario(seller);
            ad.setTitulo("[DEMO] " + v.brand() + " " + v.model() + " " + v.year());
            ad.setDescricao("ANÚNCIO FICTÍCIO — exemplo de interface para testes. " +
                "O veículo não está à venda. Valores e dados meramente ilustrativos.");
            ad.setMarca(v.brand());
            ad.setModelo(v.model());
            ad.setVersao(v.version());
            ad.setAno(v.year());
            ad.setQuilometragem(v.mileage());
            ad.setCor(v.color());
            ad.setCombustivel(v.fuel());
            ad.setCambio(v.transmission());
            ad.setPreco(BigDecimal.valueOf(v.price()));
            ad.setSegundaMao(true);
            ad.setCategoria(Categoria.SEMINOVOS);
            ad.setDestaque(true);
            ad.setDocumentacao("Exemplo fictício — não analisada");
            ad.setGarantia("Não se aplica: demonstração");
            ad.setStatus(StatusAnuncio.ATIVO);
            ad.sincronizarVeiculo();
            ads.save(ad);
        }
        ads.flush();
    }

    private Usuario createIfMissing(String email, String name, String password, Role role) {
        return users.findByEmail(email).map(existing -> {
            if (existing.getRole() != role || existing.getStatus() != StatusUsuario.ACTIVE) {
                throw new IllegalStateException("Colisão com conta de teste pré-existente");
            }
            return existing;
        }).orElseGet(() -> {
            Usuario u=new Usuario();
            u.setNome(name);
            u.setEmail(email);
            u.setTelefone("38000000000");
            u.setSenha(encoder.encode(password));
            u.setStatus(StatusUsuario.ACTIVE);
            u.setRole(role);
            return users.saveAndFlush(u);
        });
    }
}
