# Auto Minas — Fase 5B: fotos servidas sob autorização

## Vulnerabilidade corrigida
O `ResourceHandler` que disponibilizava `/uploads/fotos/**` entregava arquivos diretamente do diretório, sem verificar se o anúncio ainda estava publicado. Um link conhecido continuava acessível mesmo após PAUSADO, SUSPENSO, REJEITADO ou PENDENTE, enquanto a foto existisse em disco.

## Implementação
- Remove o mapeamento estático de `upload.dir`; uma classe `UploadConfig` intencionalmente não expõe caminhos físicos.
- Novo `FotoPublicaController` responde em `GET /uploads/fotos/{filename}`, preservando o formato das URLs já geradas.
- `FotoPublicaService` exige registro `AnuncioFoto` vinculado à URL, autorização via `AnuncioService.exigirVisibilidadePorId` e somente então lê os bytes do storage local.
- Anúncios `ATIVO`: qualquer visitante pode visualizar as imagens. Demais estados: somente titular autenticado e equipe autorizada.
- Identificadores de arquivo precisam ser UUIDs canônicos em letras minúsculas com extensão JPEG/PNG gerada pelo servidor. Arquivos desconhecidos, órfãos, nomes arbitrários e links simbólicos não são publicados.
- Limite de tamanho na leitura; resposta HTTP com `Cache-Control: no-store, max-age=0`, `X-Content-Type-Options: nosniff` e MIME explícito.
- O acesso ao conteúdo privado usa Bearer JWT, nunca credenciais colocadas nos URLs.

## Impacto no frontend
- A vitrine pública continua exibindo fotos de anúncios ATIVO por URL HTTP normal.
- **Para ver fotos de um anúncio não publicado**, um componente HTML `<img src=...>` não envia header Authorization. O frontend do painel do vendedor/revisor precisa fazer `fetch` autenticado, criar um Blob e gerar URL temporária de objeto, revogando-a ao desmontar a tela. Não colocar JWT em query string.
- Imagens legadas com outro formato de nome ou armazenadas fora do diretório padrão exigem inventário/migração; por segurança o controller não publica caminhos desconhecidos.
- **Cache externo:** CDN, proxies e browsers que guardaram imagens antes da correção não podem ser apagados somente pelo backend. Revisar purga de cache, storage anterior, headers de edge e migração de URLs antes do lançamento.

## Testes
- Foto enviada para anúncio passa a PENDENTE: anônimo e terceiro recebem 404; dono autenticado recebe 200.
- Quando o anúncio se torna ATIVO, o público recebe a imagem JPEG/PNG real.
- SUSPENSO e ARQUIVADO: não servem a anônimos; arquivamento elimina foto local.
- Arquivo órfão que existe fisicamente mas não está vinculado a anúncio não pode ser baixado.
- Regression CI: `./mvnw -B clean verify` com Java/MySQL isolados.

## Fora de escopo
- Não equivale a mídia privada distribuída/S3 com URLs assinadas ou CDN configurada.
- Imagens antigas já obtidas por terceiros não podem ser revogadas retroativamente.
- Mudança não protege URLs de outros servidores; confirmar a infraestrutura de produção.
- Não fazer deploy sem homologar o frontend e verificar fluxo de upload/migração de fotos antigas.
