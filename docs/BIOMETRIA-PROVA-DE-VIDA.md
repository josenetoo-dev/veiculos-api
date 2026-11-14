# Biometria e prova de vida — Auto Minas

Revisado em 14/11/2026. O ambiente demo mantém `BIOMETRIC_ENABLED=false` e a câmera indisponível. A revisão documental humana permanece disponível para contas com e-mail confirmado. Este documento descreve o código inspecionado e o que falta para ativar o fluxo real.

## Implementado e coberto pela suíte

- Sessão criada no backend após autenticação, e-mail confirmado, vínculo com o titular e existência da frente do documento.
- Aceite explícito da coleta no pedido de sessão. O aviso de privacidade e a base legal precisam ser definidos antes do uso com pessoas reais.
- Sessão vinculada à verificação e à evidência do documento. Documento substituído invalida o resultado anterior.
- Máximo de três sessões por verificação em 24 horas, intervalo de um minuto entre criações e três segundos entre consultas ao provedor.
- Sessões locais inconclusivas após três minutos. UUID externo gerado pelo provedor.
- Backend consulta `GetFaceLivenessSessionResults`, valida confiança e só então executa `CompareFaces` com o documento privado.
- Resultado recebido do navegador nunca é tratado como aprovação. Pontuação e imagem facial não são retornadas pelo backend.
- Aprovação técnica permite encaminhar à revisão humana. Administrador pode registrar dispensa fundamentada para análise humana, sem aprovar seus próprios documentos.
- Dados faciais e scores não são persistidos pela aplicação. `auditImagesLimit=0` no provedor. Isso não substitui avaliação da retenção e dos termos da AWS.
- Limiares inválidos, inclusive NaN e infinitos, são rejeitados antes de chamar o provedor.

## Fluxo para terminar a integração

1. Definir a conta AWS, região, política de tratamento e responsável pela revisão. A documentação oficial consultada lista São Paulo entre as regiões disponíveis. Manter a mesma região no backend e na captura.
2. Configurar uma identidade de serviço no backend com permissões mínimas para criar/consultar sessões e comparar faces. Usar variáveis protegidas ou identidade temporária, nunca segredos no Git, navegador ou logs.
3. Adicionar `FaceLivenessDetectorCore` ao frontend com traduções PT-BR e um provedor de credenciais temporárias autorizado. A captura usa `rekognition:StartFaceLivenessSession`. Escolher Cognito Identity Pool autenticado ou broker de credenciais temporárias vinculado à sessão da aplicação. Não liberar credenciais permanentes nem criar pool público sem controle de abuso.
4. O navegador solicita `POST /v1/verificacoes/{id}/biometria/sessoes`, recebe o identificador e abre a captura oficial. O ID vem do backend.
5. No callback da captura, consultar `GET /v1/verificacoes/{id}/biometria/sessoes/{sessionId}/resultado`. O backend decide o estado a partir da AWS. Tratar espera, inconclusão, expiração e tentativas excedidas.
6. Homologar câmera negada/ausente, iluminação insuficiente, replay, foto/vídeo de tela, documento trocado, sessão de outra conta, abandono e retorno, acesso por celular e acessibilidade. Preservar alternativa de revisão humana.
7. Validar exclusão/retenção, limites de custo e monitoramento antes de habilitar as flags do frontend e backend conjuntamente. Testes com mocks confirmam regras da aplicação, mas não validam a captura ou a resistência real do provedor a fraude.

## Dependências externas pendentes

- Conta e permissões AWS, orçamento e mecanismo de credenciais temporárias.
- Política de privacidade, retenção, consentimento/aviso e procedimento de contestação.
- Captura Amplify integrada e homologada com o provedor real. O botão atual é indisponível e não simula uma verificação.
- SMTP para confirmar o e-mail antes de iniciar a verificação.

## Fontes oficiais consultadas

- https://docs.aws.amazon.com/rekognition/latest/dg/face-liveness-faq.html
- https://ui.docs.amplify.aws/react/connected-components/liveness?platform=nextjs
- https://ui.docs.amplify.aws/react/connected-components/liveness/troubleshooting
