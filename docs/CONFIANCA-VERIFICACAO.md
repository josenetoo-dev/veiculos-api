# Confiança e verificação — diagnóstico e implantação incremental

Data: 16/11/2026. Base auditada: API `2c5ec41`, frontend `a20126f`.
Este documento separa a fundação documental existente dos quatro níveis completos solicitados.

## O que existe

- Cadastro e login com sessão HttpOnly no BFF; cadastro pendente pode preparar anúncio.
- Confirmação de e-mail por código com hash, expiração, tentativas e reenvio limitado; envio por Brevo HTTPS no demo.
- Todo anúncio novo nasce PENDENTE; visibilidade, fotos privadas e permissões são verificadas no backend.
- Uma verificação pessoal reutilizável e uma por veículo; upload no cadastro, sem exigir IDs ao usuário.
- Documentos JPG/PNG privados, criptografados, com validação de conteúdo e limites de tamanho.
- Revisão humana, proibição de autoaprovação e histórico de decisões persistido.
- Publicação básica gratuita; assinatura e contrato comercial não liberam nem impedem a revisão.
- Validação de formato/dígitos do CPF e armazenamento protegido já existem. Isso não comprova titularidade ou regularidade em fonte oficial.
- Existe adaptador de prova de vida/comparação facial AWS com feature flag. A existência desse código não comprova integração operacional; biometria permanece desativada no demo.

## Correções deste incremento

- Substituir arquivo cria uma nova evidência. Sessões biométricas e dispensas do arquivo anterior não valem para o novo documento. A troca continua transacional, com remoção do arquivo anterior somente após commit.
- CORRECAO_NECESSARIA preserva os outros arquivos e permite retomar o mesmo caso.
- INCONCLUSIVA mantém a solicitação na fila humana; falha técnica não é classificada automaticamente como fraude.
- O titular pode pedir análise manual após enviar os documentos. Isso não aprova a identidade nem dispensa biometria: a dispensa excepcional existente continua exclusiva de ADMIN, justificada e vinculada ao documento exato.
- A administração pode solicitar correção, manter análise inconclusiva e exigir revalidação de uma aprovação.
- A revalidação administrativa marca EXPIRADA e retorna os anúncios ATIVOS afetados para PENDENTE na mesma transação. A publicação continua bloqueada até novas decisões documentais e de moderação.
- A classificação do resultado distingue falha técnica, ilegibilidade, inconsistência, inconclusão, suspeita e fraude confirmada. Só ADMIN pode registrar fraude confirmada; isso não executa banimento automático.
- Histórico paginado é restrito a operadores autorizados e resistente a atualizações pelo ORM, sem endpoint de edição/exclusão. Isso não é um livro-razão inviolável contra administradores do banco.
- Resumo público separa análise documental de vendedor e veículo, com datas quando realmente registradas. Não inclui CPF, telefone, documentos, motivos de fraude, biometria ou chaves de armazenamento.

## Lacunas e limites obrigatórios

| Requisito | Estado real / próximo passo |
|---|---|
| Nível 1 completo | **Não disponível.** A aprovação documental atual não equivale a todos os critérios do novo nível 1. |
| Telefone confirmado | Falta provedor e fluxo de desafio real; telefone cadastrado não significa confirmado. |
| CPF/titularidade/maioridade | Falta procedimento verificável, atestações específicas e, quando necessário, integração autorizada. Não inferir pelo formato do CPF nem por um checkbox do usuário. |
| Autenticidade e autorização de venda | A revisão manual atual precisa de roteiro, critérios de suficiência e registro específico do vínculo/mandato. Não chamar de consulta oficial. |
| Publicação obrigatoriamente vinculada ao novo nível 1 | A política atual exige e-mail, identidade, veículo e moderação. Falta integrar TODOS os novos critérios ao gate de publicação; este incremento não declara esse aceite concluído. |
| Validade e retenção | A revalidação deste incremento é manual. Falta definir prazos por evidência, expiração automática, retenção/descarte, contestação e exceções de preservação. Não confundir validade da análise com retenção do arquivo. |
| Nível 2 | Indisponível até prova de vida operacional, desafio temporário do veículo e controles contra reuso entre solicitações. Troca de arquivo protegida não resolve sozinha replay de vídeo. |
| Nível 3 | Indisponível sem parceiros, laudos e consultas autorizadas reais. |
| Nível 4 | Indisponível; declaração de venda não é venda comprovada. Requer níveis anteriores válidos, evidência independente e ao menos uma venda nos últimos seis meses corridos, com reavaliação da elegibilidade. |
| Fraude e risco | Classificações são decisões humanas auditadas. Falta política operacional de risco, tratamento de pendências graves e contestação. |
| Abuso | Falta proteção específica contra spam/duplicação de anúncios, sem impor quota paga ou limite fixo de anúncios básicos. |

Não atribuir nenhum dos níveis 1–4 enquanto faltarem seus requisitos. Este incremento exibe somente fatos documentais existentes, sem selos numéricos decorativos.

## Riscos técnicos e operacionais

- Aprovação genérica de documentos não prova titularidade, maioridade, posse atual ou ausência de fraude.
- Não ampliar coleta de CPF, nascimento, vídeos ou biometria antes de definir finalidade, necessidade, base legal, retenção, acesso e resposta a incidentes. O aceite na interface não resolve essas definições.
- Credenciais e contratação de provedores externos dependem do responsável pelo projeto. Não haverá envio SMS, cobrança ou consulta externa simulada.
- Mudança de dados pessoais e de veículo deve provocar a revalidação dos critérios afetados; ainda é necessário auditar todos os caminhos de alteração de nome/telefone/CPF.
- Expiração automática exige consulta/visibilidade consistentes em todas as rotas, fotos e listagens. Não ativá-la apenas com uma tarefa periódica que deixe janelas de acesso indevido.
- Operadores precisam de roteiro de análise e canal de contestação. Não afirmar que a moderação manual é perícia documental ou proteção absoluta.
- Antes de coletar biometria de usuários reais, avaliar impacto, contratos com operadores, processamento internacional, retenção do provedor e recuperação segura das chaves/backups.

Fontes primárias consultadas: [orientação ANPD sobre RIPD](https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/relatorio-de-impacto-a-protecao-de-dados-pessoais-ripd), [Resolução CD/ANPD 2/2022](https://www.gov.br/anpd/pt-br/acesso-a-informacao/institucional/atos-normativos/regulamentacoes_anpd/resolucao-cd-anpd-no-2-de-27-de-janeiro-de-2022) e [LGPD, texto atualizado](https://www2.camara.leg.br/legin/fed/lei/2018/lei-13709-14-agosto-2018-787077-normaatualizada-pl.html). A escolha de base legal e prazos permanece pendente de definição pelo controlador e revisão jurídica.

## Próximos incrementos

1. Validar esta fundação com regressões de autorização, substituição de arquivo, correção parcial, histórico, inconclusão, revalidação e visibilidade pública.
2. Definir e integrar confirmação de telefone, roteiro de identidade/CPF/maioridade/vínculo e política de validade. Só então conceder nível 1 e torná-lo obrigatório na publicação.
3. Implementar nível 2 com provedor real e desafios temporários, sem aprovação automática por vídeo.
4. Habilitar nível 3 somente com parceiros/procedimentos operacionais aprovados.
5. Implementar comprovação de venda independente, política de risco e janela móvel de seis meses para nível 4. Manter venda declarada, venda comprovada e avaliação como conceitos separados.
