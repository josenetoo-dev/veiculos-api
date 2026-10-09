# Auto Minas — Fase 2B: separação incremental de veículos e anúncios

## Objetivo
Criar cadastro persistente de `Veiculo` independente do anúncio e manter APIs e registros legados intactos, sem alterações em produção.

**Importante:** esta entrega é a **etapa de transição do modelo**, não a conclusão total da normalização. As colunas anteriores de veículo em `anuncio` permanecem como espelho compatível com clientes e código anteriores. O corte definitivo para uma única fonte de verdade exige outra etapa, após teste de adoção.

## Modelagem
- `veiculo`: `id`, `cadastrado_por_id`, marca, modelo, versão, ano, quilometragem, cor, combustível, câmbio e condição de segunda mão.
- `anuncio.veiculo_id`: vínculo para `veiculo.id`, obrigatório.
- `anuncio`: continua guardando preço, título, descrição, estado, moderação, anunciante, fotos, propostas e, temporariamente, as colunas antigas de veículo.
- `cadastrado_por_id` identifica quem cadastrou o veículo **no sistema**, não proprietário verificado. A verificação documental será implementada à parte.

## Compatibilidade HTTP
- Endpoints existentes (`/v1/anuncio/**`) e campos anteriores continuam funcionando.
- `AnuncioResponse` recebe um novo campo aditivo `veiculoId`.
- Todo anúncio novo cria seu veículo na mesma transação; qualquer edição de características do anúncio também sincroniza o registro correspondente.
- Anúncios legados são vinculados a veículos no backfill da migration V5.

## Migração V5
1. Cria a tabela `veiculo`.
2. Adiciona a coluna `anuncio.veiculo_id`.
3. Replica as características de cada anúncio antigo para um veículo independente. Para facilitar auditoria, os IDs dos veículos migrados coincidem com os IDs dos anúncios de origem.
4. Preenche `anuncio.veiculo_id`, torna o relacionamento obrigatório e adiciona FK.
5. Mantém as colunas e valores legados. Nenhuma linha é excluída.

**Atenção:** ainda que aditiva, a V5 faz DDL e altera o esquema. Deve ser ensaiada em uma restauração de backup do MySQL real antes de implantação. Todos os anúncios legados precisam ter campos obrigatórios consistentes.

## Testes
- Migração MySQL: backfill de anúncio legado, conservação de ID, detalhes do veículo, anúncio, foto, proposta e status.
- API: cadastro com `veiculoId`, edição com sincronização dos campos duplicados e integridade dos vínculos de negociações.
- CI: `./mvnw -B clean verify` usando Java 21 e MySQL descartável.

## Próxima etapa para normalização completa
- Eliminar a duplicação das características de veículo na tabela `anuncio` somente depois de confirmar integridade e compatibilidade em ambiente restaurado.
- Tornar `Veiculo` a fonte primária única e tratar veículo histórico/transferência, modificação de quilometragem e reanúncio sem reescrever dados negociados no passado.
- Substituir exclusões destrutivas por desativação/arquivamento controlado, preservando propostas e auditoria.

## Fora de escopo
- Consulta oficial de placa/RENAVAM/CPF, laudos, biometria ou documentos.
- Migração de mídia ou verificação documental.
- Deploy, migração na produção e merge na branch `main`.
