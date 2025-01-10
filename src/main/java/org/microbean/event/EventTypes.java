/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2025 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.event;

import java.lang.System.Logger;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.microbean.assign.Types;

import org.microbean.construct.Domain;

import static java.lang.System.Logger.Level.WARNING;

/**
 * A utility for working with <dfn>event types</dfn>.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #eventTypes(TypeMirror)
 *
 * @see #legalEventType(TypeMirror)
 */
public final class EventTypes extends Types {


  /*
   * Static fields.
   */


  private static final Logger LOGGER = System.getLogger(EventTypes.class.getName());


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link EventTypes}.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   */
  public EventTypes(final Domain domain) {
    super(domain);
  }


  /*
   * Instance methods.
   */


  /**
   * Returns an immutable {@link List} of {@linkplain #legalEventType(TypeMirror) legal event types} that the supplied
   * {@link TypeMirror} bears.
   *
   * <p>The returned {@link List} may be empty.</p>
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return an immutable {@link List} of {@linkplain #legalEventType(TypeMirror) legal event types} that the supplied
   * {@link TypeMirror} bears; never {@code null}
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @microbean.nullability This method never returns {@code null}.
   *
   * @microbean.idempotency This method is idempotent and returns determinate values.
   *
   * @microbean.threadsafety This method is safe for concurrent use by multiple threads.
   */
  public final List<? extends TypeMirror> eventTypes(final TypeMirror t) {
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#event_types_and_qualifier_types
    if (t.getKind() == TypeKind.DECLARED) {
      final Element e = ((DeclaredType)t).asElement();
      if (e.getKind().isInterface() || !e.getKind().isClass() || e.getModifiers().contains(Modifier.ABSTRACT)) {
        // "An event object is an instance of a concrete Java class...."
        if (LOGGER.isLoggable(WARNING)) {
          LOGGER.log(WARNING, t + " is an illegal event type");
        }
        return List.of();
      }
    }
    // "The event types of the event include all superclasses and interfaces of the [concrete] runtime class of the
    // event object."
    return this.supertypes(t, EventTypes::legalEventType);
  }


  /*
   * Static methods.
   */


  /**
   * Returns {@code true} if and only if the supplied {@link TypeMirror} is a <dfn>legal event type</dfn>.
   *
   * <p>Legal event types are, exactly:</p>
   *
   * <ol>
   *
   * <li>{@linkplain TypeKind#ARRAY Array} types whose {@linkplain ArrayType#getComponentType() component type}s are
   * legal event types</li>
   *
   * <li>{@linkplain TypeKind#DECLARED Declared} types that contain no {@linkplain TypeKind#WILDCARD wildcard type}s for
   * every level of containment</li>
   *
   * </ol>
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if {@code t} is a legal bean type; {@code false} otherwise
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @microbean.idempotency This method is idempotent and deterministic.
   *
   * @microbean.threadsafety This method itself is safe for concurrent use by multiple threads, but {@link TypeMirror}
   * implementations and {@link Domain} implementations may not be safe for such use.
   */
  public static final boolean legalEventType(final TypeMirror t) {
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#event_types_and_qualifier_types
    return switch (t.getKind()) {
    case ARRAY -> {
      // Recurse into the component type.
      if (!legalEventType(((ArrayType)t).getComponentType())) { // note recursion
        if (LOGGER.isLoggable(WARNING)) {
          LOGGER.log(WARNING, t + " has a component type that is an illegal event type (" + ((ArrayType)t).getComponentType());
        }
        yield false;
      }
      yield true;
    }

    // You can't fire a primitive event as of this writing, but there's nothing stopping a primitive event type from
    // being legal otherwise.
    // case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> true;

    case DECLARED -> {
      // "An event type may not contain an unresolvable type variable. A wildcard type is not considered an unresolvable
      // type variable."
      //
      // We interpret "contain" to mean "have as a type argument, recursively, anywhere".
      for (final TypeMirror ta : ((DeclaredType)t).getTypeArguments()) {
        if (ta.getKind() != TypeKind.WILDCARD && !legalEventType(ta)) { // note recursion
          if (LOGGER.isLoggable(WARNING)) {
            LOGGER.log(WARNING, t + " has a type argument that is an illegal event type (" + ta + ")");
          }
          yield false;
        }
      }
      yield true;
    }

    default -> {
      if (LOGGER.isLoggable(WARNING)) {
        LOGGER.log(WARNING, t + " is an illegal event type");
      }
      yield false;
    }
    };

  }

  /**
   * Returns {@code true} if and only if the supplied {@link TypeMirror} is a <dfn>legal observed event type</dfn>.
   *
   * <p>A legal observed event type is <a
   * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#event_types_and_qualifier_types">any Java type
   * that a method parameter may bear</a>.
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if {@code t} is a legal observed event type; {@code false} otherwise
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @microbean.idempotency This method is idempotent and deterministic.
   *
   * @microbean.threadsafety This method itself is safe for concurrent use by multiple threads, but {@link TypeMirror}
   * implementations and {@link Domain} implementations may not be safe for such use.
   */
  public static final boolean legalObservedEventType(final TypeMirror t) {
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#event_types_and_qualifier_types
    // "Any Java type [that a method parameter element may bear] may be an observed event type."
    return switch (t.getKind()) {
    case ARRAY, BOOLEAN, BYTE, CHAR, DECLARED, DOUBLE, FLOAT, INT, LONG, SHORT, TYPEVAR -> true;
    default -> false;
    };
  }

}
