package com.josenetoo_dev.veiculos_api.enums;
public enum StatusUsuario {
    PENDING_CONTACT_VERIFICATION, ACTIVE, SUSPENDED, BLOCKED, DELETED;
    public boolean podeAutenticar() { return this == ACTIVE || this == PENDING_CONTACT_VERIFICATION; }
}
