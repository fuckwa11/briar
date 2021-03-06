package org.briarproject.api.crypto;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/** Annotation for injecting the executor for long-running crypto tasks. */
@BindingAnnotation
@Target({ FIELD, METHOD, PARAMETER })
@Retention(RUNTIME)
public @interface CryptoExecutor {}
