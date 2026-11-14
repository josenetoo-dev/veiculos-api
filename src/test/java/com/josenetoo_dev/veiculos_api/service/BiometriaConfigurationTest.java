package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.BiometriaIndisponivelException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BiometriaConfigurationTest {
    @ParameterizedTest
    @ValueSource(floats = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0, 101})
    void rejectsInvalidThresholdBeforeCallingProvider(float value) {
        PrivateEvidenceStorage storage = mock(PrivateEvidenceStorage.class);
        BiometricProvider provider = mock(BiometricProvider.class);
        when(storage.available()).thenReturn(true);
        BiometriaService service = new BiometriaService(null, null, null, null, null, storage, provider);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "policyVersion", "biometria-v1");
        ReflectionTestUtils.setField(service, "faceThreshold", 90f);
        ReflectionTestUtils.setField(service, "livenessThreshold", value);
        assertThrows(BiometriaIndisponivelException.class, () -> service.iniciar(1L));
        ReflectionTestUtils.setField(service, "livenessThreshold", 90f);
        ReflectionTestUtils.setField(service, "faceThreshold", value);
        assertThrows(BiometriaIndisponivelException.class, () -> service.iniciar(1L));
        verifyNoInteractions(provider);
    }
}
