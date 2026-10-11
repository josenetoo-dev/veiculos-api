# Auto Minas — Fase 6: integração de verificação facial (AWS Rekognition)

> Implementação inicial **sem AWS habilitada por padrão**. Não houve upload de selfies reais, contratação de serviços, uso de chaves AWS, cobrança nem implantação. A versão pública ainda depende do frontend e do procedimento LGPD.

## Objetivo e limites de confiança

A biometria complementa a **revisão humana de documentos** implementada no PR #4. Ela consiste em:
1. Prova de vida (Face Liveness), para verificar se há uma pessoa real diante da câmera.
2. Comparação `CompareFaces` entre o frame selecionado pela AWS e a foto da frente do documento armazenada de forma criptografada.
3. **Decisão humana separada** de aprovação da identidade.

Um resultado `APROVADA_TECNICAMENTE` é **um sinal técnico**, **não** prova legal de identidade, autenticidade do documento nem posse legal do veículo. Não exibir um selo de "validado pelo Governo" ou "sem possibilidade de fraude". Para validação em base oficial, são necessárias integrações autorizadas e adequadas.

## Mudanças no backend

- AWS SDK for Java v2 `software.amazon.awssdk:rekognition` e adapter isolado `AwsRekognitionBiometricProvider` só ativado por configuração. Usa credenciais IAM do ambiente (default provider chain), nunca segredos no repositório.
- `DisabledBiometricProvider` retorna 503, sem chamar a AWS, se `BIOMETRIC_ENABLED=false`.
- `V11__face_liveness_sessions.sql` adiciona as tabelas `sessao_biometria` e `dispensa_biometria` (justificativas do fallback humano) com UUID da sessão, vínculo à verificação, ID da evidência original, status técnico, datas, horário do último polling, aceite registrado e versão de política. **Não armazena selfies, vídeos, embeddings faciais ou scores** no MySQL.
- A frente do documento já é armazenada em `PrivateEvidenceStorage` criptografado; o backend a recupera após autenticar e verificar a titularidade da solicitação.
- `CreateFaceLivenessSession` cria o identificador; o frontend o usa com o componente `FaceLivenessDetector` da AWS Amplify; quando o callback terminar, o backend chama `GetFaceLivenessSessionResults` e, somente com prova de vida satisfatória, `CompareFaces`.
- Limiares configuráveis: 90/100 como **valores técnicos iniciais sujeitos a calibração**. Scores e imagens de referência não são retornados ao navegador, persistidos no banco ou incluídos em logs pela implementação.
- Um token de sessão externo não é suficiente para recuperar resultados: a API exige JWT do **solicitante original** e vínculo com a verificação.
- O resultado só se aplica ao **mesmo ID da evidência documental** usado quando a sessão foi iniciada. Se o documento for excluído e reenviado, mesmo com foto idêntica, o match anterior deixa de autorizar a submissão.
- Exceção de acessibilidade: somente ADMIN ativo, distinto do solicitante, pode registrar **dispensa auditável** vinculada ao documento atual, com justificativa de 10 a 500 caracteres. A dispensa só permite enviar para análise humana — não produz um status de aprovação técnica nem confirma identidade.
- No máximo **3 sessões por verificação/24 h**, com intervalo mínimo de **1 minuto**; o usuário não escolhe sessionId. Sessões não finalizadas expiram após **3 minutos**, seguindo o limite da AWS.
- Polling de resultados protegido: no máximo **1 consulta AWS a cada 3 segundos por sessão**, registrando `consultado_em` no banco; respostas intermediárias continuam CRIADA.
- `VerificacaoService.enviar` impede envio de identidade sem resultado técnico aprovado nas últimas 24 h **somente quando a funcionalidade está habilitada**. A decisão administrativa posterior exige histórico compatível com a submissão e continua sendo humana.

## Endpoints (JWT obrigatório)

**POST `/v1/verificacoes/{verificacaoId}/biometria/sessoes`**

Body: `{"aceiteBiometria":true}`.

Retorna HTTP 201 e `{"sessionId":"uuid","status":"CRIADA","criadoEm":"...","finalizadoEm":null}`.

Requisitos: solicitação do tipo IDENTIDADE em RASCUNHO/REJEITADA, do titular autenticado com conta ACTIVE, documento de identidade (frente) previamente enviado ao backend e ciência declarada sobre a coleta.

**GET `/v1/verificacoes/{verificacaoId}/biometria/sessoes/{sessionId}/resultado`**

Somente o titular autenticado consulta. Resposta contém status `CRIADA`, `APROVADA_TECNICAMENTE` ou `INCONCLUSIVA`. Resultados ainda em progresso continuam `CRIADA` (o navegador poderá tentar novamente por um período breve). Não aceita `confidence` ou `similarity` enviados pelo navegador como fonte de verdade.

**POST `/v1/verificacoes/{verificacaoId}/biometria/dispensar` — somente ADMIN**

Body: `{"motivo":"Justificativa registrada pelo revisor"}`. Retorna 204 sem corpo. Verifica conta administrativa ativa, titular diferente, verificação ainda em edição e ID do documento anexado. Escreve evento imutável em `dispensa_biometria`, com operador, motivo e data. Uma dispensa antiga deixa de ser válida quando a frente do documento é substituída.

Esse endpoint oferece o caminho técnico para pessoas impedidas de concluir a biometria, mas não substitui suporte ao usuário, definição dos critérios administrativos nem revisão documental.

## Configuração (opt-in)

```properties
BIOMETRIC_ENABLED=false
BIOMETRIC_AWS_REGION=sa-east-1
BIOMETRIC_LIVENESS_THRESHOLD=90
BIOMETRIC_FACE_SIMILARITY_THRESHOLD=90
BIOMETRIC_POLICY_VERSION=biometria-v1
```

- A região precisa suportar Face Liveness e permitir o uso de `CompareFaces`; confirme disponibilidade, preço e quotas na conta AWS escolhida.
- Configure IAM backend com **somente** `rekognition:CreateFaceLivenessSession`, `rekognition:GetFaceLivenessSessionResults`, `rekognition:CompareFaces`. Ajustar escopo de recurso, políticas de rede e limites conforme suporte AWS (ações podem exigir `Resource: "*"`).
- Para o frontend React, a AWS Amplify UI fornece `FaceLivenessDetector`, que chama `StartFaceLivenessSession`. Isso exige credenciais **AWS temporárias e limitadas**, preferencialmente por identidade autenticada; conceder no frontend somente `rekognition:StartFaceLivenessSession`. **NUNCA** entregar chave IAM permanente, acesso ao resultado ou ações de comparação ao browser.
- Para não ampliar acesso/consumo indevido, mapear JWT do Auto Minas às credenciais AWS temporárias autenticadas (Cognito/federação apropriada), restringir papéis/quotas e testar a política IAM antes do lançamento.
- `VERIFICATION_STORAGE_KEY` e `VERIFICATION_STORAGE_DIR` privados precisam estar ativos para ler a foto do documento.
- **Não habilitar a flag apenas para testar**: a primeira sessão real poderá gerar consumo cobrado. Configure orçamento AWS (Budget), alertas, limites e mecanismos antiabuso antes de realizar testes com pessoas.

## Fluxo a integrar no frontend

1. Informar coleta e finalidade de dados biométricos, prazo de retenção, possibilidade de revisão/contestação e alternativa acessível. Registrar a base legal apropriada para dado sensível (o `aceiteBiometria` não resolve sozinho o enquadramento jurídico).
2. Usuário envia frente e verso do documento à rota protegida.
3. Após login e autorização, frontend pede a sessão com `aceiteBiometria: true`.
4. Amplify `FaceLivenessDetector` executa câmera e prova de vida com o **sessionId** retornado.
5. Após concluir captura, frontend consulta o resultado **no backend** (com Bearer JWT); nunca aprova identidade com score vindo do cliente.
6. Somente `APROVADA_TECNICAMENTE` libera o envio da solicitação para **análise documental humana**.
7. Em caso INCONCLUSIVA, permitir nova tentativa dentro do limite ou seguir fluxo alternativo de revisão humana previamente autorizado, sem declarar fraude automaticamente.

## Dependências e bloqueadores para usuários reais

- Frontend React integrado, HTTPS (câmera requer origem segura) e credenciais temporárias via IAM/Cognito configuradas.
- Estabelecer base legal, aviso de privacidade, retenção e descarte, DPIA/RIPD quando necessário, direitos do titular e processo de contestação sob LGPD. Biometria é **dado pessoal sensível**.
- Garantir **alternativa acessível**: o endpoint ADMIN de dispensa foi implementado, mas ainda é necessário definir quem o opera, quais razões justificam a exceção, como contestar uma negativa e como registrar a revisão humana. A flag deve permanecer desligada até existir esse procedimento e a política de privacidade apropriada.
- Testes reais controlados com documentos de teste autorizados e AWS própria, avaliação de falso positivo/falso negativo (incluindo variabilidade demográfica e iluminação), orçamento, quotas e detecção de abuso por IP.
- Testar migrações V1–V11 e Hibernate validate em **backup restaurado do MySQL verdadeiro**; nenhum teste da CI equivale a isso.
- Revisão externa de IAM, threat modeling e análise de impactos antes do lançamento.
- O provider de testes é mockado; **CI não comprova funcionamento do FaceLivenessDetector nem confirma presença de credenciais AWS**.

## Fontes técnicas
- AWS: https://docs.aws.amazon.com/rekognition/latest/dg/face-liveness-programming-api.html
- AWS SDK Java V2: https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/services/rekognition/RekognitionClient.html
- Amplify UI React: https://ui.docs.amplify.aws/react/connected-components/liveness
- CompareFaces: https://docs.aws.amazon.com/rekognition/latest/dg/faces-comparefaces.html
