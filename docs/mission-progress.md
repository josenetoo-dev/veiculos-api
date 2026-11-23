# Missão mestre — estado da main auditada em 2026-11-23

## Base observada

- Backend Java 21/Spring Boot 4.1, MySQL/Flyway V1–V31; frontend Next.js/React/TypeScript.
- Comercial: ofertas antigas, publicação gratuita, intenção sem pagamento, confirmação manual desabilitada por padrão, expiração agendada; ainda não há integração financeira real.
- Recomendação: sinais, ranking e `GET /v1/recommendations/home` já existem. A Home ainda usa `featuredCars`.
- Confiança: verificações manuais e arquivos privados já existem; níveis 1–4 completos não foram comprovados.
- DEMO: catálogo restrito ao staging e proposta para código `DEMO-###` recusada na API.

## Entrega desta branch: proteção inicial contra abuso

- Flyway V32 acrescenta chaves HMAC e eventos de janela deslizante ao MySQL. Bloqueio de linha serializa concorrência entre instâncias.
- Filtro aplica 429/Retry-After a mutações de autenticação, eventos, mensagens, denúncias, uploads, anúncios e propostas.
- IP é o endereço de conexão, sem aceitar cabeçalhos encaminhados arbitrários. Limite por usuário autenticado e teto IP compartilhado. Valores por propriedades `app.abuse.*`.
- Limpeza periódica mantém dados de até 24 horas; migrar V32 exige backup e rollback por restauração, pois a migração é apenas aditiva.
- Teste MySQL para concorrência, isolamento de identidades e hashing. Executá-lo em ambiente com `MYSQL_TEST_*` antes de merge.

## Limites ainda abertos

- O limitador cobre apenas parte dos vetores; faltam chaves por identificador de conta com tratamento de enumeração, desafios progressivos e testes E2E de recuperação legítima.
- Todo o restante das entregas 1–30 permanece parcial ou pendente de implementação e validação. Não publicar esta branch isolada como missão concluída.
- Build local bloqueado por DNS de Maven Central neste ambiente. Validar CI antes de integrar.

## Próxima PR encadeada — oferta PRO

- Flyway V33 desativa novas ofertas legadas, preservando suas linhas e assinaturas; insere `AUTO_MINAS_PRO` sem mensalidade aprovada.
- `PUT /v1/planos/AUTO_MINAS_PRO/preco` permite configurar preço somente por ADMIN. A intenção só é criada após preço positivo; o processamento financeiro continua manual e desabilitado por padrão.
- Teste MySQL verifica preservação das linhas antigas, oferta única e ausência de preço inventado.
