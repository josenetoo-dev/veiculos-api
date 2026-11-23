package com.josenetoo_dev.veiculos_api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/buscas-salvas")
public class BuscaSalvaController {
    public record Input(String nome, Map<String,String> filtros) {}
    public record Saved(long id, String nome, Map<String,String> filtros, LocalDateTime atualizadoEm) {}
    private static final Set<String> KEYS=Set.of("q","brand","city","state","fuel","transmission",
            "minPrice","maxPrice","yearMin","sort");
    private final JdbcTemplate db;
    private final ObjectMapper json=new ObjectMapper();
    public BuscaSalvaController(JdbcTemplate db) { this.db=db; }

    private long user(Principal principal) { return Long.parseLong(principal.getName()); }
    private void validate(Input input) {
        if (input==null || input.nome()==null || input.nome().strip().isEmpty() || input.nome().length()>60
                || input.filtros()==null || input.filtros().isEmpty() || input.filtros().size()>KEYS.size()) invalid();
        for (var entry: input.filtros().entrySet()) {
            String value=entry.getValue();
            if (!KEYS.contains(entry.getKey()) || value==null || value.isBlank() || value.length()>80
                    || (Set.of("minPrice","maxPrice","yearMin").contains(entry.getKey()) && !value.matches("\\d{1,12}"))
                    || (entry.getKey().equals("state") && !value.matches("[A-Za-z]{2}"))
                    || (entry.getKey().equals("sort") && !Set.of("recentes","menor-preco","maior-preco","menor-km").contains(value))) invalid();
        }
    }
    private static void invalid() { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Busca inválida"); }
    private String encode(Map<String,String> filters) {
        try { return json.writeValueAsString(filters); } catch (JsonProcessingException e) { throw new IllegalArgumentException(e); }
    }
    private Saved row(java.sql.ResultSet result,int ignored) throws java.sql.SQLException {
        try {
            return new Saved(result.getLong("id"),result.getString("nome"),
                    json.readValue(result.getString("filtros_json"),new TypeReference<Map<String,String>>() {}),
                    result.getTimestamp("atualizado_em").toLocalDateTime());
        } catch (JsonProcessingException e) { throw new java.sql.SQLException("Filtros salvos inválidos",e); }
    }

    @GetMapping
    public List<Saved> minhas(Principal principal) {
        return db.query("SELECT id,nome,filtros_json,atualizado_em FROM busca_salva WHERE usuario_id=? ORDER BY atualizado_em DESC,id DESC LIMIT 20",
                this::row,user(principal));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Saved criar(Principal principal,@RequestBody Input input) {
        validate(input);
        long owner=user(principal);
        db.queryForObject("SELECT id FROM usuario WHERE id=? FOR UPDATE",Long.class,owner);
        if (db.queryForObject("SELECT COUNT(*) FROM busca_salva WHERE usuario_id=?",Long.class,owner)>=20)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Limite de buscas salvas atingido");
        var keys=new GeneratedKeyHolder();
        db.update(connection -> {
            var statement=connection.prepareStatement("INSERT INTO busca_salva(usuario_id,nome,filtros_json) VALUES(?,?,?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1,owner);
            statement.setString(2,input.nome().strip());
            statement.setString(3,encode(input.filtros()));
            return statement;
        },keys);
        return db.queryForObject("SELECT id,nome,filtros_json,atualizado_em FROM busca_salva WHERE usuario_id=? AND id=?",
                this::row,owner,keys.getKey().longValue());
    }
    @PutMapping("/{id}")
    public Saved editar(Principal principal,@PathVariable long id,@RequestBody Input input) {
        validate(input);
        long owner=user(principal);
        int changed=db.update("UPDATE busca_salva SET nome=?,filtros_json=?,atualizado_em=UTC_TIMESTAMP(6) WHERE id=? AND usuario_id=?",
                input.nome().strip(),encode(input.filtros()),id,owner);
        if (changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Busca não encontrada");
        return db.queryForObject("SELECT id,nome,filtros_json,atualizado_em FROM busca_salva WHERE usuario_id=? AND id=?",
                this::row,owner,id);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(Principal principal,@PathVariable long id) {
        db.update("DELETE FROM busca_salva WHERE id=? AND usuario_id=?",id,user(principal));
    }
}
