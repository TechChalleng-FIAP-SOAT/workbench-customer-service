package com.workbench.workshop.application.user;

/** Credenciais inválidas (mapeada para 401). Não diz se o e-mail existe ou se a senha errou. */
public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException() {
        super("Credenciais inválidas.");
    }
}
