package com.josenetoo_dev.veiculos_api;

import org.springframework.context.annotation.Configuration;

/**
 * Nenhuma pasta física de upload é registrada como recurso estático.
 * O download de fotos é mediado por controller com autorização baseada
 * no status do anúncio e no proprietário.
 */
@Configuration
public class UploadConfig { }
