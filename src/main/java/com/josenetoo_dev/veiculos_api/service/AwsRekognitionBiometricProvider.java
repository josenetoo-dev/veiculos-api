package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.BiometriaIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

/**
 * AWS SDK v2. Backend uses IAM role (DefaultCredentialsProvider), never commits
 * AWS keys and never returns results supplied by the user as trusted input.
 * Reference selfie exists only during the invocation; auditImagesLimit=0.
 */
@Component
@ConditionalOnProperty(prefix="verification.biometric",name="enabled",havingValue="true")
public class AwsRekognitionBiometricProvider implements BiometricProvider {
    private final RekognitionClient client;

    @org.springframework.beans.factory.annotation.Autowired
    public AwsRekognitionBiometricProvider(@Value("${verification.biometric.region}") String region) {
        this(RekognitionClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(c->c.apiCallTimeout(java.time.Duration.ofSeconds(12))
                    .apiCallAttemptTimeout(java.time.Duration.ofSeconds(9)))
                .build());
    }

    // Package-private constructor allows contract tests without credentials or AWS calls.
    AwsRekognitionBiometricProvider(RekognitionClient client) {
        this.client = client;
    }

    @Override
    public String criarSessao() {
        try {
            var response = client.createFaceLivenessSession(CreateFaceLivenessSessionRequest.builder()
                .settings(CreateFaceLivenessSessionRequestSettings.builder().auditImagesLimit(0).build())
                .build());
            if(response.sessionId()==null || response.sessionId().isBlank()) {
                throw new BiometriaIndisponivelException();
            }
            return response.sessionId();
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            throw new BiometriaIndisponivelException();
        }
    }

    @Override
    public Outcome consultar(String sessionId, byte[] documentImage, float liveness, float similarity) {
        try {
            var result=client.getFaceLivenessSessionResults(
                    GetFaceLivenessSessionResultsRequest.builder().sessionId(sessionId).build());
            if (result.status()==LivenessSessionStatus.CREATED
                    || result.status()==LivenessSessionStatus.IN_PROGRESS) return Outcome.AGUARDANDO;
            if (result.status()!=LivenessSessionStatus.SUCCEEDED
                    || result.confidence()==null || result.confidence()<liveness
                    || result.referenceImage()==null || result.referenceImage().bytes()==null) {
                return Outcome.INCONCLUSIVA;
            }

            // Only compare when liveness succeeded and confidence passed.
            // AWS SDK encodes image data; never serialize returned face bytes.
            var faceResponse = client.compareFaces(CompareFacesRequest.builder()
                    .sourceImage(Image.builder().bytes(SdkBytes.fromByteArray(documentImage)).build())
                    .targetImage(Image.builder().bytes(result.referenceImage().bytes()).build())
                    .qualityFilter(QualityFilter.AUTO)
                    .similarityThreshold(similarity)
                    .build());
            if (faceResponse.faceMatches().size()!=1) return Outcome.INCONCLUSIVA;
            var match = faceResponse.faceMatches().get(0);
            return match.similarity()!=null && match.similarity()>=similarity
                ? Outcome.APROVADA_TECNICAMENTE : Outcome.INCONCLUSIVA;
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            // Do not expose AWS error text or document/face data in logs or HTTP.
            throw new BiometriaIndisponivelException();
        }
    }
}
