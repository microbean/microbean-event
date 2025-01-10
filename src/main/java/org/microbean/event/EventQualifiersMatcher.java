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

import java.util.Collection;

import org.microbean.assign.Matcher;

import org.microbean.qualifier.NamedAttributeMap;

import static org.microbean.assign.Qualifiers.anyQualifier;
import static org.microbean.assign.Qualifiers.defaultQualifier;
import static org.microbean.assign.Qualifiers.defaultQualifiers;
import static org.microbean.assign.Qualifiers.qualifiers;

/**
 * A {@link Matcher} encapsulating <a
 * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#observer_resolution">CDI-compatible event
 * qualifier matching rules</a>.
 *
 * @author <a href="https://about.me/lairdnelson/" target="_top">Laird Nelson</a>
 *
 * @see #test(Collection, Collection)
 */
public final class EventQualifiersMatcher
  implements Matcher<Collection<? extends NamedAttributeMap<?>>, Collection<? extends NamedAttributeMap<?>>> {


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link EventQualifiersMatcher}.
   */
  public EventQualifiersMatcher() {
    super();
  }


  /*
   * Instance methods.
   */


  /**
   * Returns {@code true} if and only if either the {@linkplain org.microbean.assign.Qualifiers#qualifiers(Collection)
   * qualifiers present} in {@code receiverAttributes} are {@linkplain Collection#isEmpty() empty}, or if the collection
   * of {@linkplain org.microbean.assign.Qualifiers#qualifiers(Collection) qualifiers present} in {@code
   * payloadAttributes} {@linkplain Collection#containsAll(Collection) contains all} of the {@linkplain
   * org.microbean.assign.Qualifiers#qualifiers(Collection) qualifiers present} in {@code receiverAttributes}.
   *
   * @param receiverAttributes a {@link Collection} of {@link NamedAttributeMap}s; must not be {@code null}
   *
   * @param payloadAttributes a {@link Collection} of {@link NamedAttributeMap}s; must not be {@code null}
   *
   * @return {@code true} if and only if either the {@linkplain org.microbean.assign.Qualifiers#qualifiers(Collection)
   * qualifiers present} in {@code receiverAttributes} are {@linkplain Collection#isEmpty() empty}, or if the collection
   * of {@linkplain org.microbean.assign.Qualifiers#qualifiers(Collection) qualifiers present} in {@code
   * payloadAttributes} {@linkplain Collection#containsAll(Collection) contains all} of the {@linkplain
   * org.microbean.assign.Qualifiers#qualifiers(Collection) qualifiers present} in {@code receiverAttributes}
   *
   * @exception NullPointerException if either argument is {@code null}
   */
  @Override // Matcher<Collection<? extends NamedAttributeMap<?>>, Collection<? extends NamedAttributeMap<?>>>
  public final boolean test(final Collection<? extends NamedAttributeMap<?>> receiverAttributes,
                            final Collection<? extends NamedAttributeMap<?>> payloadAttributes) {
    // "An event is delivered to an observer method if...the observer method has no event qualifiers or has a subset of
    // the event qualifiers."
    final Collection<? extends NamedAttributeMap<?>> receiverQualifiers = qualifiers(receiverAttributes);
    return receiverQualifiers.isEmpty() || qualifiers(payloadAttributes).containsAll(receiverQualifiers);
  }

}
