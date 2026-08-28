/* Auto Minas — cliente do backend veiculos-api (Java + Spring Boot).
   Este arquivo é a ÚNICA porta de dados do site. Nada aqui é inventado:
   cada função corresponde a um endpoint documentado do backend.

   Base URL: http://localhost:8080 (editável na tela do site; fica no localStorage).
   Auth: JWT no header "Authorization: Bearer <token>", validade de 24h, sem refresh.
   Qualquer 401/403 fora de /auth = sessão expirada -> login.
*/
(function () {
  const CHAVE_URL = "am_base_url";
  const CHAVE_TOKEN = "am_token";
  const CHAVE_USER = "am_usuario";
  const PADRAO_URL = "https://veiculos-api-production-e50d.up.railway.app";

  const ls = {
    get(k) { try { return localStorage.getItem(k); } catch (e) { return null; } },
    set(k, v) { try { localStorage.setItem(k, v); } catch (e) {} },
    del(k) { try { localStorage.removeItem(k); } catch (e) {} }
  };

  /* Enums — valores exatos aceitos pelo backend */
  const ENUMS = {
    cambio: ["AUTOMATICO", "MANUAL", "AUTOMATIZADO"],
    categoria: ["SEMINOVOS", "PREMIUM", "BLINDADOS", "MOTOS", "CONSIGNACAO"],
    statusAnuncio: ["ATIVO", "PAUSADO", "VENDIDO"],
    statusProposta: ["PENDENTE", "CANCELADA", "ACEITA", "NEGADA", "CONTRAPROPOSTA"],
    combustivel: ["GASOLINA", "ETANOL", "FLEX", "DIESEL", "ELETRICO", "HIBRIDO"],
    tipoFoto: ["FRENTE", "TRASEIRA", "INTERIOR", "PAINEL", "OUTRO"]
  };

  const ROTULOS = {
    AUTOMATICO: "Automático", MANUAL: "Manual", AUTOMATIZADO: "Automatizado",
    SEMINOVOS: "Seminovos", PREMIUM: "Premium", BLINDADOS: "Blindados", MOTOS: "Motos", CONSIGNACAO: "Consignação",
    ATIVO: "Ativo", PAUSADO: "Pausado", VENDIDO: "Vendido",
    PENDENTE: "Pendente", CANCELADA: "Cancelada", ACEITA: "Aceita", NEGADA: "Negada", CONTRAPROPOSTA: "Contraproposta",
    GASOLINA: "Gasolina", ETANOL: "Etanol", FLEX: "Flex", DIESEL: "Diesel", ELETRICO: "Elétrico", HIBRIDO: "Híbrido"
  };

  class ErroApi extends Error {
    constructor(mensagem, status, sessaoExpirada) {
      super(mensagem);
      this.status = status || 0;
      this.sessaoExpirada = !!sessaoExpirada;
    }
  }

  const baseUrl = () => (ls.get(CHAVE_URL) || PADRAO_URL).replace(/\/+$/, "");
  const token = () => ls.get(CHAVE_TOKEN);

  async function req(metodo, caminho, corpo, publico) {
    const headers = { "Accept": "application/json" };
    if (corpo !== undefined) headers["Content-Type"] = "application/json";
    if (!publico && token()) headers["Authorization"] = "Bearer " + token();

    let r;
    try {
      r = await fetch(baseUrl() + caminho, {
        method: metodo,
        headers,
        body: corpo !== undefined ? JSON.stringify(corpo) : undefined
      });
    } catch (e) {
      throw new ErroApi("Não foi possível falar com o backend em " + baseUrl() + ". Ele está rodando?", 0);
    }

    /* Só 401 (token ausente/inválido/expirado) é sessão expirada.
       403 é permissão negada numa ação pontual (ex.: mensagem de uma conversa
       que não é sua) — não deve derrubar a sessão nem mandar pro login. */
    if (!publico && r.status === 401) {
      ls.del(CHAVE_TOKEN); ls.del(CHAVE_USER);
      throw new ErroApi("Sessão expirada. Entre de novo.", r.status, true);
    }
    if (r.status === 204) return null;

    let dados = null;
    const txt = await r.text();
    if (txt) { try { dados = JSON.parse(txt); } catch (e) { dados = { mensagem: txt }; } }

    if (!r.ok) {
      /* formato do backend: { mensagem, status } — ou o erro padrão do Spring (@Valid) */
      const msg = (dados && (dados.mensagem || dados.message))
        || (dados && Array.isArray(dados.errors) && dados.errors[0] && dados.errors[0].defaultMessage)
        || "Erro " + r.status + " na chamada " + metodo + " " + caminho;
      throw new ErroApi(msg, r.status);
    }
    return dados;
  }

  /* AnuncioResponse chega do backend; o site só lê pelos nomes normalizados aqui. */
  function normalizarAnuncio(a) {
    if (!a) return null;
    const g = (...ks) => { for (const k of ks) if (a[k] !== undefined && a[k] !== null) return a[k]; return null; };
    const dono = g("usuario") || {};
    return {
      id: g("id"),
      codigo: g("codigo") || "",
      titulo: g("titulo") || "",
      versao: g("versao") || "",
      descricao: g("descricao") || "",
      marca: g("marca") || "",
      modelo: g("modelo") || "",
      ano: Number(g("ano") || 0),
      quilometragem: Number(g("quilometragem") || 0),
      preco: Number(g("preco") || 0),
      cor: g("cor") || "",
      combustivel: g("combustivel") || "",
      cambio: g("cambio") || "",
      categoria: g("categoria") || "",
      status: g("status") || "",
      segundaMao: !!g("segundaMao", "segunda_mao"),
      laudoCautelar: g("laudoCautelar", "laudo_cautelar") || "",
      documentacao: g("documentacao") || "",
      garantia: g("garantia") || "",
      criadoEm: g("criadoEm", "criado_em") || "",
      usuarioId: g("usuarioId", "usuario_id") || dono.id || null,
      usuarioNome: dono.nome || g("usuarioNome") || "",
      fotoCapaUrl: g("fotoCapaUrl", "foto_capa_url") || null
    };
  }

  function normalizarUsuario(u) {
    if (!u) return null;
    return { id: u.id, nome: u.nome || "", email: u.email || "", telefone: u.telefone || "", criadoEm: u.criadoEm || u.criado_em || "" };
  }

  function normalizarProposta(p) {
    if (!p) return null;
    const anuncio = p.anuncio || p.anunciante || null;
    return {
      id: p.id,
      valor: Number(p.valor || 0),
      descricao: p.descricao || "",
      status: p.status || "",
      criadoEm: p.criadoEm || p.criado_em || "",
      anuncioId: p.anuncioId || p.anuncio_id || (anuncio && anuncio.id) || null,
      anuncioTitulo: (anuncio && (anuncio.titulo || anuncio.codigo)) || "",
      compradorId: p.compradorId || p.comprador_id || (p.comprador && typeof p.comprador === 'object' ? p.comprador.id : p.comprador) || null,
      contrapropostaValor: p.contrapropostaValor != null ? Number(p.contrapropostaValor) : null,
      contrapropostaDescricao: p.contrapropostaDescricao || "",
      contrapropostaFeita: !!p.contrapropostaFeita
    };
  }

  function normalizarMensagem(m) {
    if (!m) return null;
    return {
      id: m.id,
      propostaId: m.propostaId,
      remetenteId: m.remetenteId,
      remetenteNome: m.remetenteNome || "",
      conteudo: m.conteudo || "",
      criadoEm: m.criadoEm || ""
    };
  }

  /* Resposta paginada do Spring Data */
  function pagina(r, mapear) {
    const conteudo = (r && (r.content || r.itens)) || [];
    return {
      itens: conteudo.map(mapear),
      total: (r && r.totalElements) || conteudo.length,
      paginas: (r && r.totalPages) || 1,
      pagina: (r && r.number) || 0,
      tamanho: (r && r.size) || conteudo.length,
      primeira: !!(r && r.first),
      ultima: r ? !!r.last : true,
      vazia: conteudo.length === 0
    };
  }

  function qs(o) {
    const p = [];
    Object.keys(o).forEach(k => { if (o[k] !== undefined && o[k] !== null && o[k] !== "") p.push(k + "=" + encodeURIComponent(o[k])); });
    return p.length ? "?" + p.join("&") : "";
  }

  const API = {
    ENUMS, ROTULOS, ErroApi,
    PADRAO_URL,
    baseUrl,
    definirBaseUrl(u) { ls.set(CHAVE_URL, (u || PADRAO_URL).trim()); },

    /* Só existe rota pública de POST em /auth, então o teste de vida bate em
       /v1/anuncio e aceita qualquer resposta HTTP como "backend de pé". */
    async ping() {
      try { const r = await fetch(baseUrl() + "/v1/anuncio", { method: "GET" }); return { online: true, status: r.status }; }
      catch (e) { return { online: false, status: 0 }; }
    },

    /* ---------- sessão (guardada no navegador; o backend não tem sessão) ---------- */
    autenticado: () => !!token(),
    usuarioSalvo() { try { return JSON.parse(ls.get(CHAVE_USER) || "null"); } catch (e) { return null; } },
    sair() { ls.del(CHAVE_TOKEN); ls.del(CHAVE_USER); },

    /* ---------- AUTENTICAÇÃO (público) ---------- */
    /* POST /auth/register -> { id, nome, email, telefone, criadoEm } */
    async registrar({ nome, email, senha, telefone }) {
      const u = normalizarUsuario(await req("POST", "/auth/register", { nome, email, senha, telefone }, true));
      await API.entrar({ email, senha });
      return u;
    },

    /* POST /auth/login -> { token, tipo }. Não devolve id/nome: buscamos em seguida. */
    async entrar({ email, senha }) {
      const r = await req("POST", "/auth/login", { email, senha }, true);
      if (!r || !r.token) throw new ErroApi("O backend não devolveu token.", 0);
      ls.set(CHAVE_TOKEN, r.token);
      const u = await API.meusDados();
      if (u) ls.set(CHAVE_USER, JSON.stringify(u));
      return u || { id: null, nome: email.split("@")[0], email, telefone: "" };
    },

    /* GET /v1/usuario/me -> dados do usuário autenticado (descoberto pelo token) */
    async meusDados() {
      return normalizarUsuario(await req("GET", "/v1/usuario/me"));
    },

    /* ---------- USUÁRIO ---------- */
    async usuario(id) { return normalizarUsuario(await req("GET", "/v1/usuario/" + id)); },
    async usuarios({ page = 0, size = 10, sort } = {}) { return pagina(await req("GET", "/v1/usuario" + qs({ page, size, sort })), normalizarUsuario); },
    async buscarUsuarios(nome, { page = 0, size = 10 } = {}) { return pagina(await req("GET", "/v1/usuario/buscar" + qs({ nome, page, size })), normalizarUsuario); },
    async atualizarUsuario(id, { nome, email, telefone }) {
      const u = normalizarUsuario(await req("PUT", "/v1/usuario/" + id, { nome, email, telefone }));
      if (u) ls.set(CHAVE_USER, JSON.stringify(u));
      return u;
    },
    async trocarSenha(id, { senhaAtual, novaSenha }) {
      return normalizarUsuario(await req("PUT", "/v1/usuario/" + id + "/senha", { senhaAtual, novaSenha }));
    },
    async excluirUsuario(id) { return req("DELETE", "/v1/usuario/" + id); },

    /* ---------- ANÚNCIO ---------- */
    async anuncios({ page = 0, size = 12, sort } = {}) { return pagina(await req("GET", "/v1/anuncio" + qs({ page, size, sort })), normalizarAnuncio); },
    async anuncio(id) { return normalizarAnuncio(await req("GET", "/v1/anuncio/" + id)); },
    async anuncioPorCodigo(codigo) { return normalizarAnuncio(await req("GET", "/v1/anuncio/codigo/" + encodeURIComponent(codigo))); },
    async anunciosPorCategoria(categoria, { page = 0, size = 12, sort } = {}) {
      const r = await req("GET", "/v1/anuncio/categoria/" + categoria + qs({ page, size, sort }));
      return Array.isArray(r) ? { itens: r.map(normalizarAnuncio), total: r.length, paginas: 1, pagina: 0, ultima: true, vazia: !r.length } : pagina(r, normalizarAnuncio);
    },
    async anunciosPorStatus(status, { page = 0, size = 12, sort } = {}) {
      const r = await req("GET", "/v1/anuncio/status/" + status + qs({ page, size, sort }));
      return Array.isArray(r) ? { itens: r.map(normalizarAnuncio), total: r.length, paginas: 1, pagina: 0, ultima: true, vazia: !r.length } : pagina(r, normalizarAnuncio);
    },
    async criarAnuncio(dados) { return normalizarAnuncio(await req("POST", "/v1/anuncio", dados)); },
    /* PUT força o status de volta para ATIVO — comportamento do backend */
    async atualizarAnuncio(id, dados) { return normalizarAnuncio(await req("PUT", "/v1/anuncio/" + id, dados)); },
    async excluirAnuncio(id) { return req("DELETE", "/v1/anuncio/" + id); },

    /* ---------- PROPOSTA ---------- */
    async criarProposta({ valor, descricao, anuncioId }) {
      return normalizarProposta(await req("POST", "/v1/proposta", { valor, descricao, anuncioId }));
    },
    async proposta(id) { return normalizarProposta(await req("GET", "/v1/proposta/" + id)); },
    /* GET /v1/proposta/minhas -> propostas em que sou comprador ou dono do anúncio */
    async minhasPropostas({ size = 100 } = {}) {
      return pagina(await req("GET", "/v1/proposta/minhas" + qs({ size })), normalizarProposta).itens;
    },
    async aceitarProposta(id) { return normalizarProposta(await req("PUT", "/v1/proposta/" + id + "/aceitar")); },
    async negarProposta(id) { return normalizarProposta(await req("PUT", "/v1/proposta/" + id + "/negar")); },
    async cancelarProposta(id) { return normalizarProposta(await req("PUT", "/v1/proposta/" + id + "/cancelar")); },
    async fazerContraproposta(id, { valor, descricao }) {
      return normalizarProposta(await req("PUT", "/v1/proposta/" + id + "/contraproposta", { valor, descricao }));
    },

    /* ---------- CHAT (mensagens vinculadas a uma proposta) ---------- */
    async mensagens(propostaId, { size = 200, sort } = {}) {
      return pagina(await req("GET", "/v1/proposta/" + propostaId + "/mensagens" + qs({ size, sort })), normalizarMensagem).itens;
    },
    async ultimaMensagem(propostaId) {
      const msgs = await API.mensagens(propostaId, { size: 1, sort: "criadoEm,desc" });
      return msgs[0] || null;
    },
    async enviarMensagem(propostaId, conteudo) {
      return normalizarMensagem(await req("POST", "/v1/proposta/" + propostaId + "/mensagens", { conteudo }));
    },

    /* ---------- FOTOS DE ANÚNCIO ---------- */
    async fotosDoAnuncio(anuncioId, { size = 20 } = {}) {
      return pagina(await req("GET", "/v1/anuncio/" + anuncioId + "/fotos" + qs({ size })), f => f).itens;
    },
    async apagarFotoAnuncio(anuncioId, fotoId) {
      return req("DELETE", "/v1/anuncio/" + anuncioId + "/fotos/" + fotoId);
    },
    async uploadFotos(anuncioId, arquivos) {
      const formData = new FormData();
      arquivos.forEach(f => formData.append("fotos", f));

      let r;
      try {
        r = await fetch(baseUrl() + "/v1/anuncio/" + anuncioId + "/fotos", {
          method: "POST",
          headers: { "Authorization": "Bearer " + (token() || "") },
          body: formData
        });
      } catch(e) {
        throw new ErroApi("Erro ao enviar fotos: " + e.message, 0);
      }
      if (!r.ok) throw new ErroApi("Erro " + r.status + " ao enviar fotos", r.status);
      return r.json();
    }
  };

  window.AutoMinasAPI = API;
})();