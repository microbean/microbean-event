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

import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import javax.lang.model.type.TypeMirror;

import org.microbean.assign.Annotated;

import org.microbean.bean.Qualifiers;
import org.microbean.bean.ReferencesSelector;

import org.microbean.construct.Domain;

import org.microbean.construct.type.UniversalType;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for working with <dfn>events</dfn>.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
// Deliberately not final.
public class Events {

  private final EventTypes eventTypes;

  private final org.microbean.assign.Qualifiers aq;

  private final EventTypeMatcher eventTypeMatcher;

  private final EventQualifiersMatcher eventQualifiersMatcher;

  private final Annotated<TypeMirror> eventListenerAnnotated;

  /**
   * Creates a new {@link Events}.
   *
   * @param domain a non-{@code null} Domain
   *
   * @param eventTypes a non-{@code null} {@link EventTypes}
   *
   * @param aq a non-{@code null} {@link org.microbean.assign.Qualifiers}
   *
   * @param bq a non-{@code null} {@link Qualifiers}
   *
   * @param eventTypeMatcher a non-{@code null} {@link EventTypeMatcher}
   *
   * @param eventQualifiersMatcher a non-{@code null} {@link EventQualifiersMatcher}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  public Events(final Domain domain,
                final EventTypes eventTypes,
                final org.microbean.assign.Qualifiers aq,
                final Qualifiers bq,
                final EventTypeMatcher eventTypeMatcher,
                final EventQualifiersMatcher eventQualifiersMatcher) {
    super();
    this.eventTypes = requireNonNull(eventTypes, "eventTypes");
    this.aq = requireNonNull(aq, "aq");
    this.eventTypeMatcher = requireNonNull(eventTypeMatcher, "eventTypeMatcher");
    this.eventQualifiersMatcher = requireNonNull(eventQualifiersMatcher, "eventQualifiersMatcher");
    this.eventListenerAnnotated =
      Annotated.of(new UniversalType(bq.anyQualifiers(),
                                     domain.declaredType(domain.typeElement(EventListener.class.getCanonicalName()),
                                                         domain.wildcardType(),
                                                         domain.wildcardType()),
                                     domain));
  }

  /**
   * Delivers ("fires") the supplied {@code event} to <dfn>suitable</dfn> {@link EventListener}s.
   *
   * @param typeArgumentSource handwave here about the specified type and type argument substitution
   *
   * @param annotations a {@link List} of {@link AnnotationMirror}s qualifying the event; must not be {@code null}
   *
   * @param event the event; must not be {@code null}
   *
   * @param rs a {@link ReferencesSelector}; used to find {@link EventListener EventListener&lt;?, ?&gt;} references;
   * must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception IllegalArgumentException if {@code typeArgumentSource} is unsuitable
   *
   * @see #fire(EventListener, Object, ReferencesSelector)
   *
   * @see EventTypes#eventTypes(TypeMirror, Object)
   */
  // Deliberately final.
  public final void fire(final TypeMirror typeArgumentSource,
                         final List<AnnotationMirror> annotations,
                         final Object event,
                         final ReferencesSelector rs) {
    final EventTypeList eventTypes = this.eventTypes.eventTypes(typeArgumentSource, event);
    final List<AnnotationMirror> eventQualifiers = this.aq.qualifiers(annotations);
    final Iterator<? extends EventListener<?, ? super Object>> i =
      rs.<EventListener<?, ? super Object>>references(this.eventListenerAnnotated).iterator();
    while (i.hasNext()) {
      final EventListener<?, ? super Object> el = i.next();
      try {
        final Annotated<? extends Element> slot = el.eventDependency();
        if (slot == null ||
            !this.eventQualifiersMatcher.test(this.aq.qualifiers(slot.annotations()), eventQualifiers)) {
          continue;
        }
        final TypeMirror slotType = slot.annotated().asType();
        for (final TypeMirror eventType : eventTypes) {
          if (this.eventTypeMatcher.test(slotType, eventType)) {
            // This level of indirection permits asynchronous notification.
            this.fire(el, event, rs);
            break;
          }
        }
      } finally {
        i.remove(); // if the EventListener is in a scope where it can be removed, do so, otherwise no-op
      }
    }
  }

  /**
   * Delivers ("Fires") the supplied {@code event} to the supplied {@link EventListener} via its {@link
   * EventListener#eventReceived(Object, ReferencesSelector)} method.
   *
   * <p>The default implementation of this method behaves as if its body were exactly {@link EventListener
   * el}<code>.</code>{@link EventListener#eventReceived(Object, ReferencesSelector) eventReceived(event, rs)}.</p>
   *
   * <p>When this method is invoked by the {@link #fire(TypeMirror, List, Object, ReferencesSelector)} method, it is
   * guaranteed that the supplied {@link EventListener} is a suitable one for the supplied {@code event}.</p>
   *
   * <p>Overrides of this method must not call the {@link #fire(TypeMirror, List, Object, ReferencesSelector)} method,
   * or an infinite loop may result.</p>
   *
   * @param el an {@link EventListener} that has been determined to be suitable for the supplied {@code event}; must
   * not be {@code null}
   *
   * @param event the event to deliver; must not be {@code null}
   *
   * @param rs a {@link ReferencesSelector}; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  protected void fire(final EventListener<?, ? super Object> el,
                      final Object event,
                      final ReferencesSelector rs) {
    el.eventReceived(event, rs);
  }

}
