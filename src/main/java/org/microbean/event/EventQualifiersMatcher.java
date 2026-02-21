/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2025–2026 microBean™.
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

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;

import org.microbean.assign.Matcher;
import org.microbean.assign.Qualifiers;

import static java.util.Objects.requireNonNull;

/**
 * A {@link Matcher} encapsulating <a
 * href="https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1#observer_resolution">CDI-compatible event
 * qualifier matching rules</a>.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 *
 * @see #test(Collection, Collection)
 */
public final class EventQualifiersMatcher implements Matcher<Collection<? extends AnnotationMirror>, Collection<? extends AnnotationMirror>> {


  /*
   * Instance fields.
   */


  private final Qualifiers qualifiers;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link EventQualifiersMatcher}.
   *
   * @param qualifiers a {@link Qualifiers}; must not be {@code null}
   *
   * @exception NullPointerException if {@code qualifiers} is {@code null}
   */
  public EventQualifiersMatcher(final Qualifiers qualifiers) {
    super();
    this.qualifiers = requireNonNull(qualifiers, "qualifiers");
  }


  /*
   * Instance methods.
   */


  /**
   * Returns {@code true} if and only if either the {@linkplain org.microbean.assign.Qualifiers#qualifiers(Collection)
   * qualifiers present} in {@code receiverAnnotations} are {@linkplain Collection#isEmpty() empty}, or if the
   * collection of {@linkplain org.microbean.assign.Qualifiers#qualifiers(Collection) qualifiers present} in {@code
   * payloadAnnotations} {@linkplain Collection#containsAll(Collection) contains all} of the {@linkplain
   * org.microbean.assign.Qualifiers#qualifiers(Collection) qualifiers present} in {@code receiverAnnotations}.
   *
   * @param receiverAnnotations a {@link Collection} of {@link AnnotationMirror} instances; must not be {@code null}
   *
   * @param payloadAnnotations a {@link Collection} of {@link AnnotationMirror} instances; must not be {@code null}
   *
   * @return {@code true} if and only if either the {@linkplain org.microbean.assign.Qualifiers#qualifiers(Collection)
   * qualifiers present} in {@code receiverAnnotations} are {@linkplain Collection#isEmpty() empty}, or if the
   * collection of {@linkplain org.microbean.assign.Qualifiers#qualifiers(Collection) qualifiers present} in {@code
   * payloadAnnotations} {@linkplain Collection#containsAll(Collection) contains all} of the {@linkplain
   * org.microbean.assign.Qualifiers#qualifiers(Collection) qualifiers present} in {@code receiverAnnotations}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  @Override // Matcher<Collection<? extends NamedAttributeMap<?>>, Collection<? extends NamedAttributeMap<?>>>
  public final boolean test(final Collection<? extends AnnotationMirror> receiverAnnotations,
                            final Collection<? extends AnnotationMirror> payloadAnnotations) {
    // "An event is delivered to an observer method if...the observer method has no event qualifiers or has a subset of
    // the event qualifiers."
    final Collection<? extends AnnotationMirror> receiverQualifiers = qualifiers.qualifiers(receiverAnnotations);
    return receiverQualifiers.isEmpty() || qualifiers.qualifiers(payloadAnnotations).containsAll(receiverQualifiers);
  }

}
