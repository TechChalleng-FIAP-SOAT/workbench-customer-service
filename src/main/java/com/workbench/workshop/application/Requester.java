package com.workbench.workshop.application;

import com.workbench.workshop.domain.client.Client;

/**
 * Quem fez a requisição: o {@code subject} do JWT (CPF/CNPJ para cliente, e-mail para
 * staff) e se é um token de cliente ({@code ROLE_CLIENT}). Cliente só acessa o próprio
 * cadastro e os próprios veículos; staff acessa todos.
 */
public record Requester(String subject, boolean client) {

    public static Requester staff(String subject) {
        return new Requester(subject, false);
    }

    /** True quando pode ver os dados deste cliente. */
    public boolean canAccess(Client owner) {
        return !client || (subject != null && owner != null
                && subject.replaceAll("[^0-9]", "").equals(owner.getDocument().getValue()));
    }

    public void requireAccessTo(Client owner) {
        if (!canAccess(owner)) {
            throw new SecurityException("Cliente não autorizado a acessar dados de outro cliente.");
        }
    }
}
