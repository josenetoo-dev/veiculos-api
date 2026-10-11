# Auto Minas — Fase 5: recuperação de senha

## Entrega
- `POST /auth/password-reset/request` recebe `{"email":"usuario@example.com"}` e responde HTTP **202** com corpo vazio tanto para conta existente quanto desconhecida, reduzindo enumeração de usuários.
- Se existir conta habilitada, SMTP configurado e cooldown vencido, gera código de 256 bits com validade de 15 minutos. Armazena somente **SHA-256** do código na tabela `usuario` e envia exclusivamente ao e-mail cadastrado. Nenhum JSON retorna o segredo.
- `POST /auth/password-reset/confirm` recebe `{"email":"usuario@example.com","token":"codigo-recebido","newPassword":"senha-forte"}`; valida código em tempo constante, expiração e política de senha, grava hash BCrypt e revoga access tokens antigos via `tokenVersion`.
- Código de uso único. Cinco tentativas incorretas invalidam o desafio; o contador é preservado mesmo quando a requisição responde 400.
- Requisição repetida dentro de 1 minuto recebe 202, sem novo envio, para evitar enumeração por 429.
- Alterar o e-mail cadastrado invalida código de recuperação do endereço anterior.
- `PASSWORD_RESET_ENABLED=false` por padrão: sem SMTP habilitado, solicitação responde 503. Não há fallback que publique token em logs.

## Configuração
- `PASSWORD_RESET_ENABLED=true`
- `PASSWORD_RESET_FROM` — remetente controlado.
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`.
- Credenciais somente no secret manager do ambiente, nunca no repositório.
- Todos os envios reais devem passar por domínio configurado, reputação de envio, testes e política antiabuso.

## Banco
`V10__password_recovery.sql` adiciona colunas opcionais de token/expiração/solicitação e contador de falhas. Não altera senhas antigas. A CI usa MySQL descartável; testar V1–V10 numa cópia restaurada do banco antes de deploy.

## Testes automatizados
- Solicitar reset de conta desconhecida sem revelar existência.
- Entrega simulada de SMTP para conta existente.
- Expiração, token errado, contagem de cinco falhas, replay, cooldown e revogação de JWT.
- Política de senha; migração V10 preservando hashes e dados antigos.

## Atenção antes de liberar publicamente
- **Risco residual:** envio SMTP síncrono pode produzir diferenças de latência entre contas existentes e inexistentes. Ideal evoluir para outbox/filas e tempo de resposta uniforme antes de lançamento em escala.
- **Rate limit global:** cooldown por usuário não substitui limites por IP, dispositivo, ASN e proteção de SMTP em camadas (gateway/WAF + serviço), especialmente em cenários distribuídos.
- **Auditoria e recuperação:** enviar notificação de troca de senha, garantir suporte ao titular, planejar reversão de comprometimento de contas e analisar indicadores de abuso.
- **Não concluído:** integração frontend, testes com SMTP real, homologação de segurança, revisão legal e migrations no banco real.
