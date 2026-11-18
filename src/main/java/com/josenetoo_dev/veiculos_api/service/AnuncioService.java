package com.josenetoo_dev.veiculos_api.service;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException;

import com.josenetoo_dev.veiculos_api.repository.AnuncioFotoRepository;
import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioRequest;
import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioResponse;
import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnuncioService {

    private final AnuncioRepository     anuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnuncioFotoRepository anuncioFotoRepository;
    private final ImageStorage imageStorage;
    private final VerificacaoService verificacaoService;
    private final com.josenetoo_dev.veiculos_api.repository.PropostaRepository propostaRepository;
    private final com.josenetoo_dev.veiculos_api.repository.MensagemRepository mensagemRepository;
    private final VendaDeclaradaService vendaDeclaradaService;

    // Monta o response já com a URL da foto capa (ordem 0)
    private AnuncioResponse toResponse(Anuncio anuncio) {
        String fotoCapa = anuncioFotoRepository
                .findFirstByAnuncioIdOrderByOrdemAsc(anuncio.getId())
                .map(f -> f.getUrl())
                .orElse(null);
        return new AnuncioResponse(anuncio, fotoCapa);
    }

    private Anuncio verificarId(Long id) {
        return anuncioRepository.findById(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
    }

    private Usuario obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new CredenciaisInvalidasException("Usuário não autenticado");
        }

        Long id;
        try {
            id = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new CredenciaisInvalidasException("Usuário autenticado não encontrado");
        }

        return usuarioRepository.findById(id)
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário autenticado não encontrado"));
    }

    private void exigirDonoDoAnuncio(Anuncio anuncio, Usuario usuario) {
        if (!anuncio.getUsuario().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("Somente o dono do anúncio pode alterá-lo");
        }
    }

    /** Dados não publicados só podem ser consultados pelo vendedor ou administração. */
    private void exigirVisibilidade(Anuncio anuncio) {
        if (anuncio.getStatus() == StatusAnuncio.ATIVO) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AnuncioNaoEncontradoException("Anuncio não encontrado");
        }
        boolean staff = authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_REVIEWER".equals(authority.getAuthority()));
        if (!staff && !anuncio.getUsuario().getId().toString().equals(authentication.getName())) {
            throw new AnuncioNaoEncontradoException("Anuncio não encontrado");
        }
    }

    /** Também utilizado pelo endpoint público de fotos para impedir enumeração de anúncios pausados. */
    @Transactional(readOnly = true)
    public void exigirVisibilidadePorId(Long anuncioId) {
        exigirVisibilidade(verificarId(anuncioId));
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> meusAnuncios(Pageable pageable) {
        return anuncioRepository.findByUsuarioId(obterUsuarioAutenticado().getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public AnuncioResponse criarAnuncio(AnuncioRequest request) {
        Anuncio anuncio = new Anuncio();

        Usuario usuario = obterUsuarioAutenticado();

        usuarioRepository.findByIdForUpdate(usuario.getId()).orElseThrow();
        if (anuncioRepository.countByUsuarioIdAndCriadoEmAfter(usuario.getId(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1)) >= 10) {
            throw new AnuncioIndisponivelException("Muitos anúncios enviados em um minuto. Aguarde antes de enviar novamente.");
        }
        if (anuncioRepository.contarDuplicados(usuario.getId(), request.getMarca().trim(), request.getModelo().trim(),
                request.getAno(), request.getQuilometragem(), request.getPreco(),
                java.util.List.of(StatusAnuncio.PENDENTE, StatusAnuncio.ATIVO)) > 0) {
            throw new AnuncioIndisponivelException("Você já tem um anúncio ativo ou em análise com estes dados. Acesse Meus anúncios para editar.");
        }
        anuncio.setUsuario(usuario);

        anuncio.setVersao(request.getVersao() == null || request.getVersao().isBlank() ? "Não informada" : request.getVersao().strip());
        anuncio.setLaudoCautelar(request.getLaudoCautelar());
        anuncio.setDocumentacao(request.getDocumentacao());
        anuncio.setGarantia(request.getGarantia());
        anuncio.setTitulo(request.getTitulo());
        anuncio.setDescricao(request.getDescricao());
        anuncio.setPreco(request.getPreco());
        anuncio.setMarca(request.getMarca());
        anuncio.setModelo(request.getModelo());
        anuncio.setAno(request.getAno());
        anuncio.setQuilometragem(request.getQuilometragem());
        anuncio.setCor(request.getCor());
        anuncio.setCidade(request.getCidade() == null || request.getCidade().isBlank()
                ? null : request.getCidade().strip());
        anuncio.setUf(request.getUf());
        anuncio.setCombustivel(request.getCombustivel());
        anuncio.setSegundaMao(request.getSegundaMao() == null ? true : request.getSegundaMao());
        // Nenhum anúncio novo é publicado automaticamente.
        anuncio.setStatus(StatusAnuncio.PENDENTE);
        anuncio.setCambio(request.getCambio());
        if (request.getCategoria() == com.josenetoo_dev.veiculos_api.enums.Categoria.MOTOS) {
            throw new AnuncioIndisponivelException("A categoria Motos não aceita novos anúncios");
        }
        anuncio.setCategoria(request.getCategoria());
        // Veiculo e campos legados do anuncio são persistidos na mesma transação.
        anuncio.sincronizarVeiculo();

        Anuncio anuncioSalvo = anuncioRepository.save(anuncio);

        anuncioSalvo.setCodigo("AM-" + anuncioSalvo.getId());

        return toResponse(anuncioRepository.save(anuncioSalvo));
    }

    /**
     * Modificar fotos de anúncio aprovado invalida sua moderação.
     * Executado na mesma transação da alteração de mídia.
     */
    @Transactional
    public void reabrirRevisaoDeFotos(Anuncio anuncio) {
        if (anuncio.getStatus() == StatusAnuncio.SUSPENSO || anuncio.getStatus() == StatusAnuncio.ARQUIVADO) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException(
                    "Anúncio suspenso ou arquivado não pode ser reaberto pelo titular");
        }
        if (anuncio.getStatus() != StatusAnuncio.PENDENTE) {
            anuncio.setStatus(StatusAnuncio.PENDENTE);
            anuncio.setRevisadoEm(null);
            anuncio.setRevisadoPorId(null);
            anuncio.setMotivoRejeicao(null);
            anuncioRepository.save(anuncio);
        }
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarAnuncios(Pageable pageable) {
        return anuncioRepository.findByStatus(StatusAnuncio.ATIVO, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> buscarPublicamente(String q, String tipo, String marca, String cidade, String uf,
            Categoria categoria, com.josenetoo_dev.veiculos_api.enums.TipoCombustivel combustivel,
            com.josenetoo_dev.veiculos_api.enums.Cambio cambio, java.math.BigDecimal precoMin,
            java.math.BigDecimal precoMax, Integer anoMin, String sort, int page, int size) {
        if (page < 0 || size < 1 || precoMin != null && precoMin.signum() < 0
                || precoMax != null && precoMax.signum() < 0
                || precoMin != null && precoMax != null && precoMin.compareTo(precoMax) > 0
                || anoMin != null && (anoMin < 1886 || anoMin > java.time.Year.now().getValue() + 1)) {
            throw new IllegalArgumentException("Filtros inválidos");
        }
        Categoria tipoCategoria = null;
        if (tipo != null && !tipo.isBlank()) {
            if ("MOTO".equalsIgnoreCase(tipo)) tipoCategoria = Categoria.MOTOS;
            else if (!"CARRO".equalsIgnoreCase(tipo)) throw new IllegalArgumentException("Tipo inválido");
        }
        org.springframework.data.domain.Sort sortBy = switch (sort == null ? "recentes" : sort) {
            case "menor-preco" -> org.springframework.data.domain.Sort.by("preco").ascending();
            case "maior-preco" -> org.springframework.data.domain.Sort.by("preco").descending();
            case "menor-km" -> org.springframework.data.domain.Sort.by("quilometragem").ascending();
            case "recentes" -> org.springframework.data.domain.Sort.by("criadoEm").descending();
            default -> throw new IllegalArgumentException("Ordenação inválida");
        };
        var pageable = org.springframework.data.domain.PageRequest.of(page, Math.min(size, 48), sortBy);
        final Categoria finalTipoCategoria = tipoCategoria;
        var specification = (org.springframework.data.jpa.domain.Specification<Anuncio>) (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("status"), StatusAnuncio.ATIVO));
            if (finalTipoCategoria != null) predicates.add(cb.equal(root.get("categoria"), finalTipoCategoria));
            else if (tipo != null && "CARRO".equalsIgnoreCase(tipo)) predicates.add(cb.notEqual(root.get("categoria"), Categoria.MOTOS));
            if (categoria != null) predicates.add(cb.equal(root.get("categoria"), categoria));
            if (combustivel != null) predicates.add(cb.equal(root.get("combustivel"), combustivel));
            if (cambio != null) predicates.add(cb.equal(root.get("cambio"), cambio));
            if (precoMin != null) predicates.add(cb.greaterThanOrEqualTo(root.get("preco"), precoMin));
            if (precoMax != null) predicates.add(cb.lessThanOrEqualTo(root.get("preco"), precoMax));
            if (anoMin != null) predicates.add(cb.greaterThanOrEqualTo(root.get("ano"), anoMin));
            if (marca != null && !marca.isBlank()) predicates.add(cb.like(cb.lower(root.get("marca")), "%" + marca.strip().toLowerCase(java.util.Locale.ROOT) + "%"));
            if (cidade != null && !cidade.isBlank()) predicates.add(cb.like(cb.lower(root.get("cidade")), "%" + cidade.strip().toLowerCase(java.util.Locale.ROOT) + "%"));
            if (uf != null && !uf.isBlank()) predicates.add(cb.equal(cb.upper(root.get("uf")), uf.strip().toUpperCase(java.util.Locale.ROOT)));
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.strip().toLowerCase(java.util.Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("marca")), pattern, '\\'),
                        cb.like(cb.lower(root.get("modelo")), pattern, '\\'),
                        cb.like(cb.lower(root.get("versao")), pattern, '\\'),
                        cb.like(cb.lower(root.get("titulo")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return anuncioRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Transactional
    public AnuncioResponse atualizarAnuncio(AnuncioRequest request, Long id) {
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        exigirDonoDoAnuncio(anuncio, obterUsuarioAutenticado());
        if (anuncio.getStatus() == StatusAnuncio.VENDIDO || anuncio.getStatus() == StatusAnuncio.ARQUIVADO
                || anuncio.getStatus() == StatusAnuncio.SUSPENSO) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException(
                    "Anúncio vendido não pode ser editado");
        }

        if (request.getCategoria() == com.josenetoo_dev.veiculos_api.enums.Categoria.MOTOS
                && anuncio.getCategoria() != com.josenetoo_dev.veiculos_api.enums.Categoria.MOTOS) {
            throw new AnuncioIndisponivelException("A categoria Motos não está disponível para novos cadastros");
        }
        String versaoAtualizada = request.getVersao() == null || request.getVersao().isBlank()
                ? anuncio.getVersao() : request.getVersao().strip();
        boolean segundaMaoAtualizada = request.getSegundaMao() == null
                ? anuncio.isSegundaMao() : request.getSegundaMao();

        boolean mudouVeiculo = !java.util.Objects.equals(anuncio.getVersao(), versaoAtualizada)
                || !java.util.Objects.equals(anuncio.getMarca(), request.getMarca())
                || !java.util.Objects.equals(anuncio.getModelo(), request.getModelo())
                || !java.util.Objects.equals(anuncio.getAno(), request.getAno())
                || anuncio.getQuilometragem() != request.getQuilometragem()
                || !java.util.Objects.equals(anuncio.getCor(), request.getCor())
                || anuncio.getCombustivel() != request.getCombustivel()
                || anuncio.getCambio() != request.getCambio()
                || anuncio.isSegundaMao() != segundaMaoAtualizada;
        if (mudouVeiculo && anuncio.getVeiculo() != null) {
            verificacaoService.invalidarVeiculoAlterado(anuncio.getVeiculo().getId());
        }

        anuncio.setVersao(request.getVersao());
        anuncio.setLaudoCautelar(request.getLaudoCautelar());
        anuncio.setDocumentacao(request.getDocumentacao());
        anuncio.setGarantia(request.getGarantia());
        anuncio.setTitulo(request.getTitulo());
        anuncio.setDescricao(request.getDescricao());
        anuncio.setPreco(request.getPreco());
        anuncio.setMarca(request.getMarca());
        anuncio.setModelo(request.getModelo());
        anuncio.setAno(request.getAno());
        anuncio.setQuilometragem(request.getQuilometragem());
        anuncio.setCor(request.getCor());
        anuncio.setCidade(request.getCidade() == null || request.getCidade().isBlank()
                ? null : request.getCidade().strip());
        anuncio.setUf(request.getUf());
        anuncio.setCombustivel(request.getCombustivel());
        anuncio.setSegundaMao(request.isSegundaMao());
        // Qualquer edição exige nova moderação; o próprio vendedor não aprova.
        anuncio.setStatus(StatusAnuncio.PENDENTE);
        anuncio.setRevisadoEm(null);
        anuncio.setRevisadoPorId(null);
        anuncio.setMotivoRejeicao(null);
        anuncio.setCambio(request.getCambio());
        anuncio.setCategoria(request.getCategoria());
        anuncio.sincronizarVeiculo();

        return toResponse(anuncioRepository.save(anuncio));
    }

    /**
     * Arquivamento em vez de DELETE real: propostas, mensagens e auditoria
     * permanecem disponíveis para fins de disputa e histórico.
     */
    private void arquivar(Anuncio anuncio, Long actorId) {
        if (anuncio.getStatus() == StatusAnuncio.ARQUIVADO) return;
        anuncio.setStatus(StatusAnuncio.ARQUIVADO);
        anuncio.setDestaque(false);
        anuncio.setArquivadoEm(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        anuncio.setArquivadoPorId(actorId);
        // Arquivamento não apaga evidências da negociação: preserva os registros
        // e arquivos das fotos. FotoPublicaService verifica o status do anúncio
        // antes de servir qualquer imagem, inclusive por URL direta.
        // Exclusão definitiva deve seguir uma política separada de retenção/LGPD.
        anuncioRepository.save(anuncio);
    }

    /**
     * Controle de estado pelo próprio vendedor; exige autenticação e ownership.
     * Pausa remove anúncio da vitrine e de novas propostas imediatamente.
     */
    @Transactional
    public AnuncioResponse pausarAnuncio(Long id) {
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        Usuario dono = obterUsuarioAutenticado();
        exigirDonoDoAnuncio(anuncio, dono);
        if (anuncio.getStatus() != StatusAnuncio.ATIVO) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException(
                    "Somente anúncios publicados podem ser pausados");
        }
        atualizarStatusProprietario(anuncio, StatusAnuncio.PAUSADO, dono.getId());
        return toResponse(anuncioRepository.saveAndFlush(anuncio));
    }

    /**
     * Reativação nunca publica automaticamente um anúncio pausado:
     * ele retorna a PENDENTE, sujeito à moderação humana antes da vitrine.
     */
    @Transactional
    public AnuncioResponse solicitarReativacao(Long id) {
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        Usuario dono = obterUsuarioAutenticado();
        exigirDonoDoAnuncio(anuncio, dono);
        if (anuncio.getStatus() != StatusAnuncio.PAUSADO) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException(
                    "Somente anúncios pausados podem solicitar reativação");
        }
        atualizarStatusProprietario(anuncio, StatusAnuncio.PENDENTE, dono.getId());
        anuncio.setRevisadoEm(null);
        anuncio.setRevisadoPorId(null);
        anuncio.setMotivoRejeicao(null);
        return toResponse(anuncioRepository.saveAndFlush(anuncio));
    }

    /** 
     * Encerramento irrevogável do anúncio como vendido, preservando
     * propostas e histórico de mensagens sem reabrir novas negociações.
     */
    @Transactional
    public AnuncioResponse marcarVendido(Long id, com.josenetoo_dev.veiculos_api.dto.comercial.DeclararVendaRequest request) {
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        Usuario dono = obterUsuarioAutenticado();
        exigirDonoDoAnuncio(anuncio, dono);
        if (anuncio.getStatus() != StatusAnuncio.ATIVO
                && anuncio.getStatus() != StatusAnuncio.PAUSADO) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException(
                    "Somente anúncios publicados ou pausados podem ser marcados como vendidos");
        }
        vendaDeclaradaService.declarar(id, request);
        atualizarStatusProprietario(anuncio, StatusAnuncio.VENDIDO, dono.getId());
        anuncio.setDestaque(false);
        return toResponse(anuncioRepository.saveAndFlush(anuncio));
    }

    private void atualizarStatusProprietario(Anuncio anuncio, StatusAnuncio novo, Long donoId) {
        anuncio.setStatus(novo);
        anuncio.setStatusAtualizadoEm(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        anuncio.setStatusAtualizadoPorId(donoId);
    }

    @Transactional
    public void deletarAnuncio(Long id) {
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        Usuario owner = obterUsuarioAutenticado();
        exigirDonoDoAnuncio(anuncio, owner);
        arquivar(anuncio, owner.getId());
    }

    /**
     * Chamado quando o usuário encerra a conta. Nunca exclui propostas nem chats.
     */
    @Transactional
    public void arquivarAnunciosDeUsuario(Long usuarioId) {
        for (Anuncio anuncio : anuncioRepository.findByUsuarioId(usuarioId, Pageable.unpaged())) {
            Anuncio locked = anuncioRepository.findByIdForUpdate(anuncio.getId())
                    .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
            arquivar(locked, usuarioId);
        }
    }

    @Transactional(readOnly = true)
    public AnuncioResponse buscarPorId(Long id) {
        Anuncio anuncio = verificarId(id);
        exigirVisibilidade(anuncio);
        return toResponse(anuncio);
    }

    @Transactional(readOnly = true)
    public AnuncioResponse buscarPorCodigo(String codigo) {
        Anuncio anuncio = anuncioRepository.findByCodigo(codigo)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        exigirVisibilidade(anuncio);
        return toResponse(anuncio);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarDestaques(Pageable pageable) {
        return anuncioRepository.findByDestaqueTrueAndStatus(StatusAnuncio.ATIVO, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarPorStatus(StatusAnuncio status, Pageable pageable) {
        if (status == StatusAnuncio.ATIVO) {
            return anuncioRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Page.empty(pageable);
        }
        boolean staff = auth.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_REVIEWER".equals(authority.getAuthority()));
        if (staff) return anuncioRepository.findByStatus(status, pageable).map(this::toResponse);
        try {
            Long userId = Long.valueOf(auth.getName());
            return anuncioRepository.findByStatusAndUsuarioId(status, userId, pageable).map(this::toResponse);
        } catch (NumberFormatException e) {
            return Page.empty(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarPorCategoria(Categoria categoria, Pageable pageable) {
        return anuncioRepository.findByCategoriaAndStatus(categoria, StatusAnuncio.ATIVO, pageable)
                .map(this::toResponse);
    }

}
