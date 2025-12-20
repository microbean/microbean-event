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

import java.util.Iterator;
import java.util.List;

import javax.lang.model.type.TypeMirror;

import org.microbean.assign.AttributedElement;
import org.microbean.assign.AttributedType;

import org.microbean.attributes.Attributes;

import org.microbean.bean.Qualifiers;
import org.microbean.bean.ReferencesSelector;

import org.microbean.construct.Domain;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for working with <dfn>events</dfn>.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
// Deliberately not final.
public class Events {
  
  private final EventTypes eventTypes;

  private final Qualifiers qualifiers;
  
  private final EventTypeMatcher eventTypeMatcher;

  private final EventQualifiersMatcher eventQualifiersMatcher;

  private final AttributedType eventListenerAttributedType;

  /**
   * Creates a new {@link Events}.
   *
   * @param eventTypes an {@link EventTypes}; must not be {@code null}
   *
   * @param qualifiers a {@link Qualifiers}; must not be {@code null}
   *
   * @param eventTypeMatcher an {@link EventTypeMatcher}; must not be {@code null}
   *
   * @param eventQualifiersMatcher an {@link EventQualifiersMatcher}; must not be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  public Events(final EventTypes eventTypes,
                final Qualifiers qualifiers,
                final EventTypeMatcher eventTypeMatcher,
                final EventQualifiersMatcher eventQualifiersMatcher) {
    super();
    this.eventTypes = requireNonNull(eventTypes, "eventTypes");
    this.qualifiers = requireNonNull(qualifiers, "qualifiers");
    this.eventTypeMatcher = requireNonNull(eventTypeMatcher, "eventTypeMatcher");
    this.eventQualifiersMatcher = requireNonNull(eventQualifiersMatcher, "eventQualifiersMatcher");
    final Domain d = eventTypes.domain();
    this.eventListenerAttributedType =
      new AttributedType(d.declaredType(d.typeElement(EventListener.class.getCanonicalName()),
                                        d.wildcardType(),
                                        d.wildcardType(null, d.javaLangObjectType())),
                         this.qualifiers.anyQualifiers());
  }

  /**
   * Delivers ("fires") the supplied {@code event} to <dfn>suitable</dfn> {@link EventListener}s.
   *
   * <p>A suitable {@link EventListener} is one whose {@link EventListener#attributedType()} method returns an {@link
   * AttributedType}
   *
   * @param typeArgumentSource handwave here about the specified type and type argument substitution
   *
   * @param attributes a {@link List} of {@link Attributes} qualifying the event; must not be {@code null}
   *
   * @param event the event; must not be {@code null}
   *
   * @param rs a {@link ReferencesSelector}; used to find {@link EventListener EventListener&lt;?, ?&gt;} references; must not be
   * {@code null}
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
                         final List<Attributes> attributes,
                         final Object event,
                         final ReferencesSelector rs) {
    final EventTypeList eventTypes = this.eventTypes.eventTypes(typeArgumentSource, event);
    final List<Attributes> eventQualifiers = this.qualifiers.qualifiers(attributes);
    final Iterator<? extends EventListener<?, ? super Object>> i =
      rs.<EventListener<?, ? super Object>>references(this.eventListenerAttributedType).iterator();
    while (i.hasNext()) {
      final EventListener<?, ? super Object> el = i.next();
      try {
        final AttributedType slot = el.attributedType();
        if (slot == null || !this.eventQualifiersMatcher.test(this.qualifiers.qualifiers(slot.attributes()), eventQualifiers)) {
          continue;
        }
        final TypeMirror slotType = slot.type();
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
   * @param el an {@link EventListener} that has been determined to be suitable for the supplied {@code event}; must not
   * be {@code null}
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
