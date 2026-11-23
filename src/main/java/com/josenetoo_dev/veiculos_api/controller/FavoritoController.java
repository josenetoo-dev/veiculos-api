package com.josenetoo_dev.veiculos_api.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/favoritos")
public class FavoritoController {
    private final JdbcTemplate db;
    public FavoritoController(JdbcTemplate db) { this.db = db; }

    @GetMapping
    public List<Long> meus(Principal principal) {
        return db.queryForList("""
            SELECT f.anuncio_id FROM anuncio_favorito f JOIN anuncio a ON a.id=f.anuncio_id
            WHERE f.usuario_id=? AND a.status='ATIVO' ORDER BY f.criado_em DESC LIMIT 100
            """,Long.class,Long.parseLong(principal.getName()));
    }

    @GetMapping("/{id}")
    public boolean existe(Principal principal,@PathVariable long id) {
        return !db.queryForList("SELECT anuncio_id FROM anuncio_favorito WHERE usuario_id=? AND anuncio_id=?",
                Long.class,Long.parseLong(principal.getName()),id).isEmpty();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void adicionar(Principal principal,@PathVariable long id) {
        long user=Long.parseLong(principal.getName());
        db.queryForObject("SELECT id FROM usuario WHERE id=? FOR UPDATE",Long.class,user);
        if (db.queryForList("SELECT id FROM anuncio WHERE id=? AND status='ATIVO' AND (codigo IS NULL OR codigo NOT LIKE 'DEMO-%')",
                Long.class,id).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Anúncio indisponível");
        if (existe(principal,id)) return;
        if (db.queryForObject("SELECT COUNT(*) FROM anuncio_favorito WHERE usuario_id=?",Long.class,user)>=100)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Limite de favoritos atingido");
        db.update("INSERT INTO anuncio_favorito(usuario_id,anuncio_id,criado_em) VALUES(?,?,UTC_TIMESTAMP(6))",user,id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(Principal principal,@PathVariable long id) {
        db.update("DELETE FROM anuncio_favorito WHERE usuario_id=? AND anuncio_id=?",Long.parseLong(principal.getName()),id);
    }
}
