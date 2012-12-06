package net.sf.briar.api.db;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/** Annotation for injecting the executor for database tasks. */
@BindingAnnotation
@Target({ PARAMETER })
@Retention(RUNTIME)
public @interface DatabaseExecutor {}