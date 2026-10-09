# Auto Minas — Fase 4A: histórico preservado e exclusão lógica

## Problema corrigido
O endpoint DELETE de anúncios apagava anúncios, propostas e mensagens de negociação. Essa perda é inadequada para um marketplace, especialmente diante de denúncias e disputas.

## Implementação
- `DELETE /v1/anuncio/{id}` agora marca `StatusAnuncio.ARQUIVADO`, `arquivado_em` e `arquivado_por_id` após confirmar que o autor é o titular.
- Ação idempotente: repetir a exclusão de anúncio já arquivado não reabre sua publicação.
- Fotos reconhecidas pelo storage local são removidas **depois do commit**, bem como seus metadados. URLs de mídia legada exigem tratamento separado.
- Propostas, mensagens, veículo e histórico de decisões permanecem no banco, preservando IDs e referências.
- Anúncios arquivados não aparecem em listagens públicas e não aceitam novas propostas.
- Anúncio arquivado não pode ser editado ou receber alterações de fotos.
- Ao excluir a própria conta, anúncios desse usuário também são arquivados antes de invalidar sua sessão.

## Flyway
`V8__listing_archive_history.sql`: duas colunas opcionais na tabela `anuncio`. Não apaga registros e não transforma dados existentes automaticamente.

## Compatibilidade
- Resposta do DELETE continua **204 No Content**, mas a linha passa a ser preservada.
- GET por ID fica indisponível para público/terceiros; o próprio vendedor pode ver o anúncio arquivado em `GET /v1/anuncio/meus` antes de encerrar a conta.
- Negociações históricas podem continuar sendo consultadas pelos participantes conforme as regras atuais. Não há garantias transacionais de pagamento/transferência.

## Segurança e privacidade
- Reter propostas e mensagens exige **política explícita de retenção e exclusão** conforme LGPD. Não significa guardar dados indefinidamente.
- Imagens históricas em servidores externos ou URLs legadas precisam de inventário e saneamento independente.
- Controle de concorrência utiliza lock no anúncio antes de arquivar e nas alterações de foto.
- Ensaiar Flyway V1–V8 em banco MySQL restaurado antes do merge/deploy em produção.

## Testes
- Exclusão de anúncio: permanece arquivado, propostas e mensagens preservadas, foto local removida.
- Rollback: mantém status e mídia se a transação falha.
- Exclusão de conta: arquiva todos os anúncios e revoga sessão.
- Arquivados não aceitam propostas/edições/fotos.
