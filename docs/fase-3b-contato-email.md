# Auto Minas — Fase 3B: confirmação de contato por e-mail

## Motivo
Cadastros novos recebem status `PENDING_CONTACT_VERIFICATION` e, corretamente, não podem publicar anúncio. Sem um mecanismo para confirmar o e-mail, contas legítimas não conseguem evoluir para `ACTIVE`.

## Fluxo implementado
1. Usuário cadastra conta e realiza login normal. O backend permite JWT apenas para ações de onboarding.
2. `POST /v1/usuario/me/contato/email/solicitar` (JWT obrigatório) envia um código criptograficamente aleatório ao endereço salvo na conta.
3. O código tem 256 bits de entropia, validade de 15 minutos, é armazenado somente como SHA-256 e nunca aparece no JSON da API.
4. `POST /v1/usuario/me/contato/email/confirmar` (body `{"token":"codigo-recebido"}`) valida o código e ativa a conta.
5. Ao confirmar, o backend marca `contact_email_verified_at`, remove o desafio e incrementa `tokenVersion`, invalidando todos os access tokens anteriores. Usuário precisa fazer login novamente.
6. Há um intervalo mínimo de um minuto entre solicitações, controlado transacionalmente no banco para a mesma conta.
7. Troca de e-mail invalida eventuais códigos de contato emitidos anteriormente.

## Configuração (opt-in)
- `CONTACT_EMAIL_ENABLED=true` — padrão `false`.
- `CONTACT_EMAIL_FROM` — remetente, sem padrão.
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS` — conforme o provedor.
- Sem SMTP habilitado, solicitação retorna 503 e **não ativa usuários automaticamente**.

## Banco / compatibilidade
`V7__contact_email_verification.sql` adiciona quatro colunas opcionais em `usuario`. Não altera contas legadas, senhas, roles ou tokens já emitidos. Testar V1–V7 numa restauração do MySQL original antes de deploy.

## Limitações e riscos
- Confirmar endereço de e-mail não comprova identidade legal nem propriedade de veículo; a revisão documental da Fase 3 é separada.
- Falha SMTP reverte o desafio; envio em transação prolonga o lock do usuário — avaliar outbox/filas em escala.
- Proteção de abuso por IP/ASN, CAPTCHA, detecção de cadastros automatizados e limites globais de envio SMTP continuam recomendadas para abertura pública.
- Dados de e-mail seguem sujeitos a política de privacidade, consentimentos/avisos aplicáveis e LGPD.
- Conta `ACTIVE` permite solicitar anúncios, mas a publicação requer identidade e veículo aprovados por operadores, conforme PR #4.
- Nenhum SMTP pago foi ativado ou contratado nesta mudança.

## Testes
- Fluxo de emissão, hash, expiração, uso único, revogação de token e cooldown.
- MySQL V7 aditiva preservando contas existentes.
- Verificar a última CI do PR antes de qualquer merge.
