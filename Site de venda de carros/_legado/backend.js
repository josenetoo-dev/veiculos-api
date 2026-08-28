/* Auto Minas — camada de dados (back-end).
   TODO o conteúdo que a tela mostra sai daqui. O site não guarda dado nenhum:
   ele só formata e desenha o que esta camada devolve.

   Campos seguem contrato-de-dados.md (snake_case, valores em número puro).
   Para ligar no back-end real, troque só o corpo de AutoMinasAPI.carregar():
       return fetch("/api/bootstrap").then(r => r.json());
   Nada no site precisa mudar, desde que os nomes de campo sejam estes. */

(function () {
  const config_financiamento = {
    taxa_mensal: 1.79,
    entrada_minima_pct: 20,
    desconto_avista_pct: 4,
    prazos_disponiveis: [24, 36, 48, 60],
    vigente_desde: "2026-01-01"
  };

  const unidades = [
    { id: 1, nome: "Unidade Cidade Industrial", curto: "Cidade Industrial", endereco: "Av. Juscelino K. de Oliveira, 3200 — CIC, Curitiba/PR", cidade: "Curitiba", uf: "PR", horario: "Seg a sex 9h–19h · sáb 9h–15h", telefone_whatsapp: "(41) 3333-0900", ativa: true },
    { id: 2, nome: "Unidade Batel", curto: "Batel", endereco: "Av. do Batel, 1440 — Batel, Curitiba/PR", cidade: "Curitiba", uf: "PR", horario: "Seg a sex 9h–19h · sáb 9h–14h", telefone_whatsapp: "(41) 3333-0910", ativa: true }
  ];

  const anunciantes = [
    { id: 1, tipo: "loja", nome: "Diego Manfrin", papel: "Consultor Auto Minas · Batel", inicial: "D", unidade_id: 2, desde: 2015, anuncios_ativos: 9, avaliacao: 4.9, resposta_media: "responde em ~10 min", cidade: "Curitiba/PR",
      bio: "Trabalha com premium e blindados na unidade Batel desde 2015. Atende de segunda a sábado e faz a negociação inteira pelo chat quando o cliente é de fora de Curitiba, incluindo envio de laudo e vídeo do carro por WhatsApp.",
      atendimento: "Seg a sex 9h–19h · sáb 9h–14h, na unidade Batel. Responde pelo chat em cerca de 10 minutos no horário comercial.",
      verificacoes: ["Identidade e CNPJ da loja verificados", "Todos os anúncios com laudo cautelar", "Transferência feita pela loja"] },
    { id: 2, tipo: "loja", nome: "Marcelo Aoki", papel: "Consultor Auto Minas · Cidade Industrial", inicial: "M", unidade_id: 1, desde: 2012, anuncios_ativos: 11, avaliacao: 4.8, resposta_media: "responde em ~25 min", cidade: "Curitiba/PR",
      bio: "Consultor mais antigo da casa, cuida do estoque de seminovos e motos da Cidade Industrial. Especialista em troca com financiamento: monta a conta da diferença na hora, com carta de crédito de três bancos.",
      atendimento: "Seg a sex 9h–19h · sáb 9h–15h, na unidade Cidade Industrial. Responde pelo chat em cerca de 25 minutos.",
      verificacoes: ["Identidade e CNPJ da loja verificados", "Todos os anúncios com laudo cautelar", "Avaliação de usado sem custo"] },
    { id: 3, tipo: "particular", nome: "Anúncio de particular", papel: "Vendedor particular · intermediado pela loja", inicial: "P", unidade_id: null, desde: 2024, anuncios_ativos: 1, avaliacao: 4.6, resposta_media: "responde em ~2 h", cidade: "Curitiba/PR",
      bio: "Pessoa física anunciando pela plataforma. O carro fica no pátio da Auto Minas em consignação: a loja faz a vistoria de entrada, guarda o veículo e cuida da documentação, mas o preço e as propostas passam pelo proprietário.",
      atendimento: "Visitas agendadas pela loja, de seg a sáb. O proprietário responde pelo chat em até 2 horas.",
      verificacoes: ["Documento do proprietário conferido pela loja", "Vistoria de entrada feita no pátio", "Transferência acompanhada pela Auto Minas"] }
  ];

  const opcionais_catalogo = [
    "Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento",
    "Piloto automático", "Rodas de liga", "Bancos em couro", "Faróis full LED", "Start-stop", "Chave presencial"
  ];

  const v = (o) => Object.assign({ status: "disponivel", destaque: false, cor: "Preto metálico", final_placa: "7", laudo_cautelar: "Aprovado — sem sinistro", documentacao: "IPVA 2026 pago, sem multas", garantia: "3 meses motor e câmbio", fotos: [], opcionais: [] }, o);
  const foto = (url) => [{ id: 1, url: url, ordem: 0, tipo: "frente" }];

  const veiculos = [
    v({ id: 1, codigo: "AM-1041", nome: "Fiat Stilo Sporting", versao: "1.8 8V Dualogic · gasolina", ano_modelo: 2008, km: 96400, preco: 38900, categoria: "Seminovos", cambio: "Automatizado", combustivel: "Gasolina", cor: "Amarelo Interlagos", final_placa: "3", unidade_id: 1, anunciante_id: 2, fotos: foto("img/stilo-amarelo.webp"), garantia: "3 meses motor e câmbio", laudo_cautelar: "Aprovado — sem sinistro", descricao: "Stilo Sporting em cor original de fábrica, com o câmbio Dualogic revisado e embreagem trocada aos 92 mil km. Segundo dono, sempre em garagem, com manual e chave reserva. Bom para quem quer um hatch grande pagando pouco.", opcionais: ["Ar-condicionado digital", "Rodas de liga", "Central multimídia", "Sensor de estacionamento"] }),
    v({ id: 2, codigo: "AM-1042", nome: "Fiat Stilo Attractive", versao: "1.8 Flex · único dono", ano_modelo: 2010, km: 118300, preco: 34500, categoria: "Consignação", cambio: "Manual", combustivel: "Flex", cor: "Prata Bari", final_placa: "1", unidade_id: 2, anunciante_id: 3, garantia: "Sem garantia de loja (consignação)", fotos: foto("img/stilo-prata.jpg"), descricao: "Único dono desde zero km, com todas as revisões feitas em concessionária até 100 mil km. Carro de rodagem urbana leve, pneus com meia-vida e ar-condicionado gelando. Vendido em consignação: o proprietário aceita proposta à vista.", opcionais: ["Ar-condicionado digital", "Rodas de liga"] }),
    v({ id: 3, codigo: "AM-0987", nome: "Toyota Corolla XEi", versao: "2.0 Flex CVT · 1 dono", ano_modelo: 2021, km: 48200, preco: 139900, categoria: "Seminovos", cambio: "Automático", combustivel: "Flex", cor: "Prata Supernova", final_placa: "9", unidade_id: 2, anunciante_id: 1, descricao: "Corolla XEi de um único dono, com histórico completo na Toyota e revisão dos 40 mil km feita na entrada. Nunca bateu, laudo cautelar aprovado e IPVA quitado. O seminovo mais procurado da loja — sai rápido.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Piloto automático", "Rodas de liga", "Faróis full LED", "Chave presencial"] }),
    v({ id: 4, codigo: "AM-1103", nome: "Jeep Compass Longitude", versao: "1.3 T270 · teto solar", ano_modelo: 2022, km: 31500, preco: 158000, categoria: "Seminovos", cambio: "Automático", combustivel: "Flex", cor: "Cinza Granite", final_placa: "4", unidade_id: 1, anunciante_id: 2, descricao: "Compass Longitude com teto solar e pacote de assistências de série. Um dono, garantia de fábrica válida até 2027 e pneus originais com 80% de borracha. Interior sem marcas de uso.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Piloto automático", "Rodas de liga", "Faróis full LED", "Start-stop", "Chave presencial"] }),
    v({ id: 5, codigo: "AM-1188", nome: "BMW X2 sDrive20i", versao: "M Sport · teto panorâmico", ano_modelo: 2024, km: 19800, preco: 319900, categoria: "Premium", cambio: "Automático", combustivel: "Gasolina", cor: "Azul Misano", final_placa: "0", unidade_id: 2, anunciante_id: 1, garantia: "Garantia de fábrica até 2027", fotos: foto("img/bmw-x2.jpg"), descricao: "X2 M Sport praticamente novo, com teto panorâmico, som Harman Kardon e bancos em couro Vernasca. Revisões na rede BMW e garantia de fábrica transferível. Único dono, não fumante.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Piloto automático", "Rodas de liga", "Bancos em couro", "Faróis full LED", "Start-stop", "Chave presencial"] }),
    v({ id: 6, codigo: "AM-1190", nome: "Porsche Macan S", versao: "3.0 V6 · PDK", ano_modelo: 2021, km: 38600, preco: 589000, categoria: "Premium", cambio: "Automático", combustivel: "Gasolina", cor: "Branco Carrara", final_placa: "6", unidade_id: 2, anunciante_id: 1, descricao: "Macan S com pacote esportivo Chrono, escapamento esportivo e suspensão adaptativa. Manutenção toda na Porsche Center, sem retoque de pintura em nenhuma peça. Documentação em ordem para transferência imediata.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Piloto automático", "Rodas de liga", "Bancos em couro", "Faróis full LED", "Chave presencial"] }),
    v({ id: 7, codigo: "AM-1201", nome: "Toyota Hilux SRX Blindada", versao: "2.8 4x4 · nível III-A", ano_modelo: 2022, km: 56400, preco: 379000, categoria: "Blindados", cambio: "Automático", combustivel: "Diesel", cor: "Preto Attitude", final_placa: "2", unidade_id: 1, anunciante_id: 2, documentacao: "IPVA 2026 pago · certificado de blindagem válido até 2028", descricao: "Hilux SRX com blindagem nível III-A executada por empresa homologada, certificado em dia até 2028. Motor 2.8 com revisões de 10 mil em 10 mil km e nunca usada em obra. Pneus e amortecedores novos.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Piloto automático", "Rodas de liga", "Bancos em couro", "Chave presencial"] }),
    v({ id: 8, codigo: "AM-1150", nome: "Honda Civic Touring Blindado", versao: "1.5 Turbo · nível III-A", ano_modelo: 2020, km: 71200, preco: 169900, categoria: "Blindados", cambio: "Automático", combustivel: "Gasolina", cor: "Cinza Barium", final_placa: "5", unidade_id: 2, anunciante_id: 1, documentacao: "IPVA 2026 pago · certificado de blindagem válido até 2027", descricao: "Civic Touring blindado, com vidros e borrachas revisados no ano passado. Turbo 1.5 com 173 cv, câmbio CVT sem trancos e histórico de revisões na Honda. Dois donos.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Piloto automático", "Rodas de liga", "Bancos em couro", "Faróis full LED", "Chave presencial"] }),
    v({ id: 9, codigo: "AM-1215", nome: "Honda CB 500F", versao: "ABS · revisões em dia", ano_modelo: 2023, km: 8900, preco: 39900, categoria: "Motos", cambio: "Manual", combustivel: "Gasolina", cor: "Vermelho", final_placa: "8", unidade_id: 1, anunciante_id: 2, garantia: "Garantia de fábrica até 2026", descricao: "CB 500F com pouco mais de 8 mil km, ABS nas duas rodas e todas as revisões na concessionária. Nunca caiu, pneus originais e apenas um dono. Ideal para primeira moto de média cilindrada.", opcionais: ["Faróis full LED", "Central multimídia"] }),
    v({ id: 10, codigo: "AM-1219", nome: "Yamaha MT-07", versao: "689cc · escapamento original", ano_modelo: 2022, km: 14600, preco: 48900, categoria: "Motos", cambio: "Manual", combustivel: "Gasolina", cor: "Azul Ice Fluo", final_placa: "1", unidade_id: 2, anunciante_id: 1, descricao: "MT-07 original de ponta a ponta, com escapamento de fábrica e slider instalado de origem. Motor CP2 sem qualquer vazamento, corrente e relação novas. Documentação quitada.", opcionais: ["Faróis full LED"] }),
    v({ id: 11, codigo: "AM-1099", nome: "Volkswagen T-Cross Highline", versao: "1.4 TSI · pack tech", ano_modelo: 2021, km: 52400, preco: 124900, categoria: "Seminovos", cambio: "Automático", combustivel: "Flex", cor: "Branco Cristal", final_placa: "0", unidade_id: 1, anunciante_id: 2, fotos: foto("img/tcross.jpg"), descricao: "T-Cross Highline com pack tech: painel digital, piloto adaptativo e câmera de ré. Revisões na rede VW e pneus trocados aos 50 mil km. Um dono, uso familiar.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Piloto automático", "Rodas de liga", "Faróis full LED", "Start-stop", "Chave presencial"] }),
    v({ id: 12, codigo: "AM-1120", nome: "Chevrolet Onix Plus LTZ", versao: "1.0 Turbo · IPVA pago", ano_modelo: 2022, km: 44100, preco: 89900, categoria: "Consignação", cambio: "Automático", combustivel: "Flex", cor: "Cinza Satin", final_placa: "6", unidade_id: 1, anunciante_id: 3, garantia: "Sem garantia de loja (consignação)", descricao: "Onix Plus LTZ turbo automático, com central MyLink e sensor de estacionamento. Carro de uma família só, rodagem de cidade e revisões na Chevrolet. Proprietário aceita troca por hatch menor.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Rodas de liga"] }),
    v({ id: 13, codigo: "AM-1230", nome: "Maserati GranTurismo Sport", versao: "4.7 V8 460 cv · escape esportivo", ano_modelo: 2014, km: 41800, preco: 689000, categoria: "Premium", cambio: "Automático", combustivel: "Gasolina", cor: "Vermelho Ponente", final_placa: "4", unidade_id: 2, anunciante_id: 1, destaque: true, fotos: foto("img/maserati-granturismo.jpg"), descricao: "GranTurismo Sport com o V8 4.7 aspirado de 460 cv e escapamento esportivo de fábrica. Interior em couro Poltrona Frau impecável, manutenção feita em especialista com nota fiscal. Peça de colecionador que ainda pode ser usada no dia a dia.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Rodas de liga", "Bancos em couro", "Faróis full LED", "Chave presencial"] }),
    v({ id: 14, codigo: "AM-1231", nome: "Chevrolet Camaro SS", versao: "6.2 V8 461 cv · pacote 1LE", ano_modelo: 2020, km: 27300, preco: 389900, categoria: "Premium", cambio: "Automático", combustivel: "Gasolina", cor: "Amarelo Shock", final_placa: "2", unidade_id: 2, anunciante_id: 1, destaque: true, fotos: foto("img/camaro-ss.jpg"), descricao: "Camaro SS com pacote 1LE, freios Brembo e diferencial de deslizamento limitado. Nunca rodou em pista, pintura original com laudo aprovado. Som Bose e teto solar.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Rodas de liga", "Bancos em couro", "Faróis full LED", "Chave presencial"] }),
    v({ id: 15, codigo: "AM-1232", nome: "Koenigsegg Jesko Absolut", versao: "5.0 V8 biturbo 1.600 cv · 1 de 125", ano_modelo: 2023, km: 900, preco: 39900000, categoria: "Premium", cambio: "Automático", combustivel: "Gasolina", cor: "Cinza carbono aparente", final_placa: "1", unidade_id: 2, anunciante_id: 1, destaque: true, documentacao: "Importação regularizada · IPVA 2026 pago", garantia: "Garantia de fábrica até 2028", fotos: foto("img/jesko.jpg"), descricao: "Uma das 125 unidades do Jesko Absolut, com 900 km rodados e ficha de entrega original. Motor V8 biturbo de 1.600 cv com etanol, câmbio LST de nove marchas. Venda com assessoria completa de documentação e transporte fechado.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Bancos em couro", "Faróis full LED", "Chave presencial"] }),
    v({ id: 16, codigo: "AM-1233", nome: "Fiat Pulse Audace", versao: "1.0 Turbo 200 CVT · teto bicolor", ano_modelo: 2024, km: 18400, preco: 129900, categoria: "Seminovos", cambio: "Automático", combustivel: "Flex", cor: "Azul Fun com teto preto", final_placa: "9", unidade_id: 1, anunciante_id: 2, destaque: true, garantia: "Garantia de fábrica até 2027", fotos: foto("img/pulse.webp"), descricao: "Pulse Audace turbo 200 com teto bicolor, central de 10 polegadas e câmera de ré. Primeiro dono, garantia de fábrica válida e apenas 18 mil km. Consumo de 12 km/l na cidade com etanol.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Sensor de estacionamento", "Piloto automático", "Rodas de liga", "Faróis full LED", "Start-stop", "Chave presencial"] }),
    v({ id: 17, codigo: "AM-1234", nome: "Fiat Argo Drive", versao: "1.0 Firefly · completo", ano_modelo: 2022, km: 39700, preco: 74900, categoria: "Seminovos", cambio: "Manual", combustivel: "Flex", cor: "Branco Banchisa", final_placa: "5", unidade_id: 1, anunciante_id: 2, fotos: foto("img/argo.jpg"), descricao: "Argo Drive completo, com central multimídia e ar digital. Rodagem de cidade, revisões em dia e pneus com 70% de borracha. Excelente primeiro carro automático de manutenção barata.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Câmera de ré", "Rodas de liga"] }),
    v({ id: 18, codigo: "AM-1235", nome: "Fiat Mobi Like", versao: "1.0 Firefly · IPVA pago", ano_modelo: 2021, km: 46200, preco: 54900, categoria: "Seminovos", cambio: "Manual", combustivel: "Flex", cor: "Cinza Scandium", final_placa: "3", unidade_id: 1, anunciante_id: 2, fotos: foto("img/mobi.jpg"), descricao: "Mobi Like com ar-condicionado, direção elétrica e vidros dianteiros. Carro de uma dona só, usado para trajeto curto de trabalho. Faz mais de 13 km/l na cidade.", opcionais: ["Ar-condicionado digital", "Central multimídia"] }),
    v({ id: 19, codigo: "AM-1236", nome: "Fiat 500 Cult", versao: "1.4 Evo · teto solar elétrico", ano_modelo: 2013, km: 88600, preco: 44900, categoria: "Consignação", cambio: "Manual", combustivel: "Flex", cor: "Branco Perolizado", final_placa: "8", unidade_id: 2, anunciante_id: 3, garantia: "Sem garantia de loja (consignação)", fotos: foto("img/fiat-500.jpg"), descricao: "Fiat 500 Cult com teto solar elétrico funcionando e interior em couro claro conservado. Correia dentada trocada aos 85 mil km, com nota. Consignação: proposta vai direto ao proprietário.", opcionais: ["Ar-condicionado digital", "Central multimídia", "Rodas de liga", "Bancos em couro"] }),
    v({ id: 20, codigo: "AM-1237", nome: "Volkswagen Brasília 1600", versao: "Original de fábrica · placa preta", ano_modelo: 1978, km: 71400, preco: 59900, categoria: "Consignação", cambio: "Manual", combustivel: "Gasolina", cor: "Bege Cristal", final_placa: "0", unidade_id: 2, anunciante_id: 3, destaque: true, laudo_cautelar: "Aprovado — numeração original", documentacao: "Placa preta de coleção · isenta de IPVA", garantia: "Sem garantia de loja (consignação)", fotos: foto("img/brasilia.jpg"), descricao: "Brasília 1600 com numeração de motor e chassi originais, placa preta de coleção já emitida. Pintura e bancos restaurados respeitando as cores de fábrica. Roda com tranquilidade e é isenta de IPVA.", opcionais: [] })
  ];

  const conversas = [
    { id: 1, anunciante_id: 1, veiculo_id: 3, inicial: "D", hora: "14:32", status: "online agora", papel: "Diego Manfrin · Auto Minas Batel", mensagens: [
      { de: "vendedor", texto: "Oi! Aqui é o Diego, da Auto Minas Batel. Vi que você abriu o Corolla XEi — ele está disponível e é de único dono.", hora: "14:21" },
      { de: "comprador", texto: "Boa tarde. Esse valor de R$ 139.900 tem alguma margem para pagamento à vista?", hora: "14:26" },
      { de: "vendedor", texto: "Tem sim. À vista consigo fechar em R$ 134.500 com a transferência já inclusa.", hora: "14:29" },
      { de: "comprador", texto: "E o carro tem multa ou algum sinistro no histórico?", hora: "14:31" },
      { de: "vendedor", texto: "Nada. Laudo cautelar aprovado, IPVA 2026 pago e sem multas. Posso te mandar o laudo em PDF.", hora: "14:32" }
    ] },
    { id: 2, anunciante_id: null, nome: "Camila Ferrari", papel: "Interessada no seu anúncio", veiculo_id: 1, inicial: "C", hora: "11:04", status: "visto há 20 min", mensagens: [
      { de: "vendedor", texto: "Oi, tudo bem? Vi seu anúncio do Stilo. Ele aceita troca por uma moto?", hora: "10:58" },
      { de: "comprador", texto: "Oi Camila! Aceito avaliar sim, depende do modelo e do ano.", hora: "11:02" },
      { de: "vendedor", texto: "É uma CB 500F 2023 com 8 mil km. Posso levar na loja no sábado?", hora: "11:04" }
    ] },
    { id: 3, anunciante_id: 2, veiculo_id: 7, inicial: "M", hora: "ontem", status: "responde em ~10 min", papel: "Marcelo Aoki · Auto Minas CIC", mensagens: [
      { de: "vendedor", texto: "A Hilux blindada segue disponível. O certificado de blindagem está em dia até 2028.", hora: "17:40" },
      { de: "comprador", texto: "Consigo agendar um test-drive na quinta à tarde?", hora: "18:02" },
      { de: "vendedor", texto: "Consigo sim. Quinta às 15h está livre — deixo o carro separado.", hora: "18:05" }
    ] }
  ];

  const respostas_automaticas = [
    "Perfeito. Esse está com laudo cautelar aprovado e revisão feita na entrada — posso te mandar o PDF agora.",
    "Consigo segurar para você até amanhã às 18h sem custo. Quer que eu reserve?",
    "Aceitamos seu usado na troca, sim. Me diz modelo, ano e km que já faço a conta da diferença.",
    "Test-drive pode ser hoje até 19h ou sábado de manhã na unidade Batel. Qual fica melhor?"
  ];

  const respostas_rapidas = ["Ainda está disponível?", "Aceita troca?", "Qual a melhor parcela?", "Posso agendar test-drive?"];

  const raio_x_pontos = [
    { n: 1, x: 22, y: 34, estado: "Original", titulo: "Pintura dianteira", texto: "Sem repintura. Medição de espessura entre 108 e 121 micra nas oito peças frontais — dentro do padrão de fábrica." },
    { n: 2, x: 47, y: 62, estado: "Trocado", titulo: "Pneus e alinhamento", texto: "Quatro pneus novos instalados em março, com geometria e balanceamento refeitos na entrada do veículo." },
    { n: 3, x: 70, y: 40, estado: "Retoque", titulo: "Porta traseira direita", texto: "Retoque de pintura de aproximadamente 6 cm, feito por funilaria parceira. Sem massa e sem troca de peça." },
    { n: 4, x: 86, y: 56, estado: "Revisado", titulo: "Motor e câmbio", texto: "Revisão completa aos 40 mil km: óleo, filtros, correia e fluido de câmbio. Nota fiscal disponível na loja." }
  ];

  const preferencias_notificacao = [
    { k: "estoque", titulo: "Alerta de estoque", desc: "Aviso quando entra um veículo dentro do seu perfil de busca.", padrao: true },
    { k: "preco", titulo: "Queda de preço", desc: "Aviso quando um carro que você salvou baixa de valor.", padrao: true },
    { k: "chat", titulo: "Mensagens", desc: "Notificação de resposta do vendedor ou de interessados no seu anúncio.", padrao: true },
    { k: "novidades", titulo: "Novidades da loja", desc: "Feirões, condições especiais e lançamentos. No máximo um e-mail por mês.", padrao: false }
  ];

  const beneficios_conta = [
    { t: "Favoritos salvos", d: "Guarde carros e compare preço e km lado a lado." },
    { t: "Crédito em andamento", d: "Acompanhe a análise dos bancos sem ligar para a loja." },
    { t: "Alerta de estoque", d: "Avisamos quando entra um veículo no seu perfil de busca." }
  ];

  const passos_venda = [
    { n: "01", t: "Envie os dados do carro", d: "Marca, modelo, ano e km. Uma foto ajuda, mas não é obrigatória." },
    { n: "02", t: "Recebe faixa de preço", d: "Em até 2 horas úteis um consultor manda a faixa de mercado no WhatsApp." },
    { n: "03", t: "Avaliação presencial", d: "40 minutos na unidade, com vistoria de lataria, motor e histórico." },
    { n: "04", t: "Pagamento ou consignação", d: "Transferência no mesmo dia, ou anúncio no nosso pátio com preço mínimo seu." }
  ];

  const modalidades_anuncio = [
    { k: "Anúncio próprio", nota: "Você negocia direto pelo chat. Sem comissão, sem taxa de anúncio." },
    { k: "Consignação", nota: "O carro fica no pátio da Auto Minas e a loja negocia por você. Comissão de 6% na venda." },
    { k: "Venda para a loja", nota: "Proposta de compra à vista após vistoria presencial, pagamento no mesmo dia." }
  ];

  const formas_pagamento = [
    { k: "avista", titulo: "À vista", desc: "Transferência ou PIX em até 2 dias úteis." },
    { k: "financiado", titulo: "Financiado", desc: "Entrada de 20% e o saldo em 48 parcelas." },
    { k: "troca", titulo: "Com troca no negócio", desc: "Avaliamos seu usado e abatemos do valor." }
  ];

  const etapas_pedido = ["Pedido recebido", "Vistoria agendada", "Documentação", "Entrega"];

  /* usuário devolvido pelo callback do OAuth Google (o site nunca inventa) */
  const usuario_google = { id: "u-8841", nome: "Rafael Prado", email: "rafael.prado@gmail.com", telefone: "", cidade: "Curitiba/PR" };

  const DB = {
    config_financiamento, unidades, anunciantes, opcionais_catalogo, veiculos, conversas,
    respostas_automaticas, respostas_rapidas, raio_x_pontos, preferencias_notificacao,
    beneficios_conta, passos_venda, modalidades_anuncio, formas_pagamento, etapas_pedido, usuario_google
  };

  window.AUTO_MINAS_DB = DB;
  window.AutoMinasAPI = {
    /* GET /bootstrap — troque por fetch quando o back-end existir */
    carregar: () => Promise.resolve(DB),
    /* GET /veiculos?... — filtro/ordenação server-side quando passar de ~60 carros */
    veiculos: () => Promise.resolve(DB.veiculos.filter(x => x.status === "disponivel")),
    veiculo: (codigo) => Promise.resolve(DB.veiculos.find(x => x.codigo === codigo) || null),
    configFinanciamento: () => Promise.resolve(DB.config_financiamento),
    /* GET /auth/google/callback */
    autenticarGoogle: () => Promise.resolve(DB.usuario_google)
  };
})();
