# Auto Minas — Fase 1.1 (revisão complementar do PR #1)

## Escopo
Correções adicionais de visibilidade de anúncios, limpeza de fotos na exclusão e proteção do fluxo de alteração de e-mail. **Sem merge, deploy ou execução em banco de produção.**

### 1. Visibilidade de anúncios
- `GET /v1/anuncio`, `/destaques` e `/categoria/{categoria}`: apenas anúncios `ATIVO`.
- `GET /v1/anuncio/{id}` e `/codigo/{codigo}`: anúncios `PAUSADO`/`VENDIDO` respondem 404 para público e terceiros; proprietário autenticado e equipe administrativa podem consultar.
- `GET /v1/anuncio/status/{status}`: status `ATIVO` público; outros status retornam apenas os próprios anúncios para usuários comuns; equipe administrativa vê todos.
- `GET /v1/anuncio/meus`: novo endpoint autenticado para listar os próprios anúncios, inclusive pausados e vendidos.
- `GET /v1/anuncio/{id}/fotos`: não lista fotos de anúncio indisponível para usuário não autorizado.
- **Limitação importante:** imagens já publicadas com URL direta em `/uploads/fotos/**` permanecem acessíveis para quem conhece a URL. Bloqueio/expiração de URLs exige evolução do storage e da política de mídia, prevista para a Fase 2.

### 2. Exclusão de imagens
- Ao excluir anúncio, a remoção das fotos físicas criadas pelo storage atual é agendada após commit da transação, junto à remoção dos registros vinculados.
- Falha de transação mantém os arquivos, impedindo inconsistência por exclusão antecipada.
- Fotos com URLs legadas de terceiros ou padrões não reconhecidos **não são removidas automaticamente** para evitar apagar arquivos indevidos. O saneamento legado exige inventário e política própria.
- A exclusão destrutiva de anúncio/negociações continua sendo legado e deve ser substituída por soft delete com retenção de histórico na fase apropriada.

### 3. Troca de e-mail segura (habilitação opt-in)
O `PUT /v1/usuario/{id}` passa a permitir editar nome/telefone, mas **rejeita troca direta de e-mail**. A alteração exige confirmar posse do novo endereço:

1. `POST /v1/usuario/me/email-change` com `{"newEmail":"novo@example.com","currentPassword":"senha-atual"}`.
2. Backend verifica autenticação, senha atual, disponibilidade do novo endereço e cooldown de 1 minuto por usuário.
3. Gera código aleatório de 256 bits com validade de 15 minutos; armazena somente hash SHA-256 no banco e envia código exclusivamente ao novo e-mail por SMTP.
4. `POST /v1/usuario/me/email-change/confirm` com `{"token":"codigo-recebido"}`, autenticado.
5. Um código correto dentro do prazo altera o e-mail, elimina o desafio e invalida access tokens antigos via `tokenVersion`.

Nenhuma API devolve o código no JSON ou registra esse valor em logs. Envios falhos revertem o registro do desafio. A aplicação retorna **503** ao solicitar código se SMTP não estiver habilitado/configurado e **429** para pedidos excessivamente próximos. Ainda é recomendado adicionar mecanismos robustos de rate limit, prevenção de abuso SMTP e observabilidade antes de um lançamento público.

### Configuração opcional para ambiente de teste/homologação
- `EMAIL_CHANGE_ENABLED=true` — padrão `false`.
- `EMAIL_CHANGE_FROM` — endereço remetente, sem padrão.
- `SMTP_HOST` — servidor SMTP, sem padrão.
- `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`.
- `SMTP_AUTH` e `SMTP_STARTTLS` — ajustar ao provedor; senhas apenas no secret manager, nunca no Git.
- Tempos de conexão, leitura e escrita SMTP limitados a 5s.

**Não ativar no deploy até confirmar SMTP, DNS do remetente e banco migrado.** Nenhuma conta administrativa ou credencial de SMTP foi criada no repositório.

### 4. Banco / compatibilidade
- Nova migration aditiva `V3__pending_email_verification.sql`, adicionando `pending_email`, `pending_email_token_hash`, `pending_email_expires_at`, `pending_email_requested_at`.
- Sem `DROP`/`DELETE` e sem alteração de registros existentes.
- Para o banco legado, **ainda é obrigatório** backup restaurado, comparação de schema, baseline manual versão 1, V2 e V3 testadas na cópia antes do deploy.
- `FLYWAY_ENABLED=false` por padrão. A aplicação com `ddl-auto=validate` só inicia com as colunas V2 e V3 presentes, independentemente de o envio SMTP estar desabilitado.

### Validação
- Testes de regressão novos cobrem visibilidade pública/privada, fotos na exclusão e rollback, troca de e-mail sem confirmação, senha incorreta, entrega simulada (sem envio SMTP real), código inválido/expirado/de uso único, revogação de tokens e cooldown.
- Teste MySQL dedicado cobre V3 aditiva preservando conta antiga.
- O resultado final do `./mvnw -B clean verify` deve ser confirmado na CI do último commit antes de qualquer merge.

### Pendências conhecidas
- **NO-GO deploy de produção** até comprovar compatibilidade do banco real, variáveis de ambiente, integração com frontend e plano de retorno.
- Rate limiting robusto, refresh/logout, verificação obrigatória de vendedor/veículo, publicação sob revisão, mídia privada/storage externo e soft delete de anúncios continuam fora da Fase 1.1.
