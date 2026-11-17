# Recomendações da Home

O módulo usa o JWT existente. O controlador cuida de consentimento, cookie e limite de requisições. Signals grava um sinal imutável já ponderado na mesma operação que atualiza a evidência disponível para o perfil. HomeService consulta ListingCatalog, monta os perfis e entrega as seções. Ranking é Java puro. Não há chamada externa, cache ou recálculo da Home na escrita.

## Contrato HTTP

| Rota | Entrada e resultado |
| --- | --- |
| POST /v1/recommendations/events | JSON com eventId (UUID), kind, consent=true. Para SEARCH, enviar brand de uma marca em estoque. Para os demais, enviar listingId positivo. VIEW exige dwellMillis ≥ 5000. FAVORITE exige login, CONTACT exige proposta do comprador naquele anúncio. 204 para inclusão ou duplicata, 400 para sinal inválido, 429 por excesso. |
| GET /v1/recommendations/home | Parâmetros opcionais minPrice, maxPrice (reais, inclusivos) e uf. Devolve sponsored, brands, recommendations, carousels e categories. A consulta pública sem sinais não cria cookie. |
| DELETE /v1/recommendations/profile | JWT obrigatório. Apaga histórico e sinais das sessões associados à conta. Idempotente, 204. |

Após consentimento, o primeiro evento emite o cookie HttpOnly am_rec, válido por sete dias. O cliente deve conservar esse cookie e interromper o envio ao retirar consentimento. Eventos anônimos anteriores ao login permanecem anônimos. Sessões autenticadas usam uma chave derivada do cookie e do usuário para isolar contas no mesmo navegador. A limpeza ocorre a cada seis horas, com cortes de leitura de sete e 180 dias. A base armazena UF e atributos de catálogo, sem texto livre de pesquisa, e-mail, coordenadas ou IP. O limite por minuto é de 60 tentativas por conta ou endereço de conexão anônima em memória, por instância. Proxies compartilhados podem compartilhar essa cota.

## Cálculo

Cada dimensão acumula peso × 2 elevado a (−idade em dias / meia-vida). A pontuação de cada valor é dividida pelo maior valor da dimensão. Faixas vizinhas de R$ 10 mil recebem metade da afinidade. Marca, modelo, categoria, preço e UF contribuem com 20%, 20%, 15%, 30% e 15%. A confiança cresce até quatro eventos equivalentes recentes. Com evidência suficiente nos dois perfis, a sessão recebe 75% e o histórico 25%. A parcela restante usa posição cronológica e popularidade por propostas. Empates são resolvidos por id.

Os parâmetros recommendation.weights.view/details/search/favorite/contact têm valores iniciais 1/3/4/7/10. Alterações afetam novas gravações. recommendation.session-half-life e recommendation.history-half-life controlam meias-vidas em dias (3 e 30). O ID do evento e a combinação tipo/alvo/dia têm restrições únicas por usuário e por sessão. Favoritos são sinais declarados e influenciam somente o próprio perfil. A duração da visualização é informada pelo cliente.

## Inventário e operação

A V27 cria recommendation_signal, com índices de sessão, conta e retenção, e recommendation_home_benefit, uma concessão explícita por assinatura. Não existe concessão implícita para os planos atuais. A exibição exige também anúncio ativo e marcado, mesmo vendedor, assinatura associada à publicação, pagamento confirmado e vigência. Uma contratação futura desse benefício precisa registrar a concessão.

Duas consultas limitam o inventário aos oito patrocinados e aos 240 orgânicos mais recentes dentro dos filtros. O perfil lê no máximo 500 sinais por origem. A Home deduplica veículos, prioriza até três itens por marca na primeira passada e usa até três carrosséis com estoque mínimo de dois veículos. Um anúncio orgânico pode reaparecer no carrossel da própria marca. Se a leitura de perfis falhar, o serviço usa o inventário filtrado com popularidade e recência. A disponibilidade do catálogo e do banco continua necessária.

O limite de candidatos favorece anúncios recentes em estoques grandes. O teste de integração mede o número de consultas de inventário em H2. RecommendationMySqlTest gera 10 mil anúncios e 10 mil propostas em banco isolado, aquece o catálogo e mede 30 consultas orgânicas com filtros de preço e UF. Também verifica o índice da seleção cronológica, deduplicação concorrente com oito threads, isolamento entre contas, repetição semântica e retenção.

Na execução 44135347651, MySQL 8.4 apresentou mediana de 11,394 ms e p95 de 20,100 ms para a consulta do catálogo filtrado. O EXPLAIN da seleção cronológica utilizou ix_rec_inventory com leitura reversa do índice. Essa medição usa conexões JDBC novas e dados sintéticos no runner do GitHub. Ela não mede a latência HTTP da Home completa nem reproduz a distribuição de produção. O teste imprime as métricas em cada execução, sem impor um limite de latência dependente do runner.

Evidência disponível em https://github.com/josenetoo-dev/veiculos-api/actions/runs/44135347651.
