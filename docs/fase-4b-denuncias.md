# Auto Minas — Fase 4B: denúncias de anúncios suspeitos

## Objetivo
Permitir que usuários com contato confirmado sinalizem anúncios publicados com potencial fraude ou conteúdo indevido. **Denúncias não suspendem automaticamente anúncios** — exigem uma decisão humana.

## Fluxos
- `POST /v1/denuncias`: body `{"anuncioId":123,"categoria":"POSSIVEL_FRAUDE","relato":"Descrição de 10 a 1000 caracteres"}`.
- Um usuário só registra uma denúncia por anúncio; não pode denunciar anúncio próprio, anúncios não publicados ou cadastrar denúncia sem conta ACTIVE.
- `GET /v1/denuncias/minhas` e `GET /v1/denuncias/{id}`: apenas o denunciante ou operador ADMIN/REVIEWER ACTIVE.
- `GET /v1/denuncias/revisao/pendentes`: fila staff.
- `POST /v1/denuncias/revisao/{id}/confirmar` com justificativa: registra decisão e muda anúncio para SUSPENSO, impedindo reabertura pelo vendedor, sem excluir ofertas/histórico.
- `POST /v1/denuncias/revisao/{id}/descartar` com justificativa: registra decisão sem ocultar anúncio.
- Um operador não pode decidir denúncia do próprio anúncio nem a que ele mesmo registrou.
- `POST /v1/denuncias/revisao/{id}/reverter`: somente ADMIN pode reverter denúncia CONFIRMADA, registrando motivo e devolvendo anúncio SUSPENSO a PENDENTE; **nunca republica diretamente**.
- Anúncios SUSPENSO não podem ser editados, ter fotos modificadas ou ser reaprovados pelo próprio titular.
- Decisões são de uso único; repetição gera 409. A suspensão por denúncia é registrada também como `ModeracaoEvento`.

## Persistência
`V9__listing_reports.sql` cria tabela `denuncia` com FK para anúncio e denunciante, unicidade por usuário+anúncio, categoria, status, relato, revisor, decisão e horários. Migração aditiva, sem `DROP`/`DELETE`.

## Segurança/privacidade
- Relatos são privados, potencialmente contêm dados de terceiros e devem seguir LGPD.
- O frontend deve renderizar relatos como texto escapado, nunca HTML confiável.
- A confirmação de denúncia é baseada em análise humana; não declara crime ou fraude como fato.
- Falta um mecanismo de recurso/contestação, política de SLA de revisão e controle antiabuso por IP/dispositivo, antes de ampliação pública.
- URLs legadas de fotos ainda podem estar acessíveis a quem as recebeu antes da suspensão; storage com controle de acesso é evolução necessária.

## Testes
- Isolamento de denúncias, auto-denúncia, duplicidade, conta não verificada, perfil revisor, decisão única, suspensão administrativa resistente a bypass, recurso ADMIN e preservação de propostas.
- MySQL V9 valida chaves, unicidade e não alteração de anúncios existentes.
- `./mvnw -B clean verify` na CI.
