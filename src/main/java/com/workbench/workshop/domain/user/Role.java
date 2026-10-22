package com.workbench.workshop.domain.user;

/**
 * Papéis de usuário staff (paridade com o monólito). A autorização por role é
 * aplicada no OS Service e pelo authorizer do API Gateway a partir das claims do JWT.
 */
public enum Role {
    ADMIN,
    TECHNICIAN
}
