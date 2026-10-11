package com.josenetoo_dev.veiculos_api.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * AWS contract tests use a mocked SDK client; no real AWS requests or credentials.
 */
class AwsRekognitionBiometricProviderTest {
    private final RekognitionClient sdk=mock(RekognitionClient.class);
    private final AwsRekognitionBiometricProvider provider=new AwsRekognitionBiometricProvider(sdk);
    private final String sessionId=UUID.randomUUID().toString();
    private final byte[] document={1,2,3,4};
    private final SdkBytes liveImage=SdkBytes.fromByteArray(new byte[]{8,7,6,5});

    private void completed(Float liveness) {
        when(sdk.getFaceLivenessSessionResults(any(GetFaceLivenessSessionResultsRequest.class)))
            .thenReturn(GetFaceLivenessSessionResultsResponse.builder()
                .sessionId(sessionId).status(LivenessSessionStatus.SUCCEEDED)
                .confidence(liveness)
                .referenceImage(AuditImage.builder().bytes(liveImage).build())
                .build());
    }

    @Test void createsSessionWithoutAuditImagesAndReturnsOnlyAwsUuid() {
        when(sdk.createFaceLivenessSession(any(CreateFaceLivenessSessionRequest.class)))
            .thenReturn(CreateFaceLivenessSessionResponse.builder().sessionId(sessionId).build());
        assertEquals(sessionId,provider.criarSessao());
        var captor=ArgumentCaptor.forClass(CreateFaceLivenessSessionRequest.class);
        verify(sdk).createFaceLivenessSession(captor.capture());
        assertEquals(0,captor.getValue().settings().auditImagesLimit());
        verifyNoMoreInteractions(sdk);
    }

    @Test void successfulLivenessMustCompareDocumentAndLivePhotoWithThresholds() {
        completed(97.5f);
        when(sdk.compareFaces(any(CompareFacesRequest.class))).thenReturn(
            CompareFacesResponse.builder()
                .faceMatches(CompareFacesMatch.builder().similarity(95.0f).build())
                .build());
        assertEquals(BiometricProvider.Outcome.APROVADA_TECNICAMENTE,
            provider.consultar(sessionId,document,90f,90f));
        var captor=ArgumentCaptor.forClass(CompareFacesRequest.class);
        verify(sdk).compareFaces(captor.capture());
        CompareFacesRequest compare=captor.getValue();
        assertArrayEquals(document,compare.sourceImage().bytes().asByteArray());
        assertArrayEquals(liveImage.asByteArray(),compare.targetImage().bytes().asByteArray());
        assertEquals(90f,compare.similarityThreshold());
        assertEquals(QualityFilter.AUTO,compare.qualityFilter());
    }

    @Test void livenessBelowThresholdNeverComparesFaces() {
        completed(89.9f);
        assertEquals(BiometricProvider.Outcome.INCONCLUSIVA,
            provider.consultar(sessionId,document,90f,90f));
        verify(sdk,never()).compareFaces(any(CompareFacesRequest.class));
    }

    @Test void failedAndExpiredSessionsCannotBeApproved() {
        when(sdk.getFaceLivenessSessionResults(any(GetFaceLivenessSessionResultsRequest.class)))
            .thenReturn(GetFaceLivenessSessionResultsResponse.builder()
                .sessionId(sessionId).status(LivenessSessionStatus.FAILED).build())
            .thenReturn(GetFaceLivenessSessionResultsResponse.builder()
                .sessionId(sessionId).status(LivenessSessionStatus.EXPIRED).build());
        assertEquals(BiometricProvider.Outcome.INCONCLUSIVA,
            provider.consultar(sessionId,document,90f,90f));
        assertEquals(BiometricProvider.Outcome.INCONCLUSIVA,
            provider.consultar(sessionId,document,90f,90f));
        verify(sdk,never()).compareFaces(any(CompareFacesRequest.class));
    }

    @Test void pendingSessionMayBeRetriedAndDoesNotRunFaceComparison() {
        when(sdk.getFaceLivenessSessionResults(any(GetFaceLivenessSessionResultsRequest.class)))
            .thenReturn(GetFaceLivenessSessionResultsResponse.builder()
                .sessionId(sessionId).status(LivenessSessionStatus.IN_PROGRESS).build());
        assertEquals(BiometricProvider.Outcome.AGUARDANDO,
            provider.consultar(sessionId,document,90f,90f));
        verify(sdk,never()).compareFaces(any(CompareFacesRequest.class));
    }

    @Test void noFaceMatchIsInconclusive() {
        completed(98f);
        when(sdk.compareFaces(any(CompareFacesRequest.class)))
            .thenReturn(CompareFacesResponse.builder().build());
        assertEquals(BiometricProvider.Outcome.INCONCLUSIVA,
            provider.consultar(sessionId,document,90f,90f));
    }
}
