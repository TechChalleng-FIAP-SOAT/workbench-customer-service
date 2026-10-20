package com.workbench.workshop.infrastructure.config;

import java.lang.annotation.*;

/** Marca um campo String para ser normalizado em UPPERCASE na desserialização. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UpperCase {}
