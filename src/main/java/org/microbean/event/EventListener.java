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

import java.util.SequencedSet;

import javax.lang.model.element.Element;

import org.microbean.assign.Aggregate;
import org.microbean.assign.AttributedElement;
import org.microbean.assign.AttributedType;
import org.microbean.assign.AttributedTyped;

import org.microbean.bean.ReferencesSelector;

/**
 * An {@link java.util.EventListener EventListener}, an {@link Aggregate}, and an {@link AttributedTyped} that
 * {@linkplain #eventReceived(Object, ReferencesSelector) receives} <dfn>events</dfn>.
 *
 * @param <R> the result of reception; almost always {@link Void}
 *
 * @param <E> the event type
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see Events
 */
// Used for receiving events.
//
// TODO: IntEventListener, BooleanEventListener, etc.?
//
// <R> is almost always Void.
// <E> is the event type.
//
// I know this looks like a Bean, but it is not. Specifically, the dependencies here are not thsoe required to create
// the EventListener, but those required to receive the event.
//
// Given:
//
//   private void onEvent(@Observes /* or similar */ String e, @Complicated Frob f) {}
//
// * R is Void
// * E is String
// * dependencies() includes VariableElement-representing-f and nothing else
// * eventDependency() includes VariableElement-representing-e and nothing else
// * receive() calls onEvent() supplying it with event and a @Complicated Frob acquired via r
//
// TODO: Does this actually need to be an AttributedTyped?
public interface EventListener<R, E> extends AttributedTyped, Aggregate, java.util.EventListener {

  /**
   * Returns a non-{@code null}, determinate {@link AttributedType} describing the kinds of events this {@link
   * EventListener} is prepared to handle.
   *
   * <p>The default implementation of this method extracts this information from an invocation of the {@link
   * #eventDependency()} method ({@linkplain #eventDependency() <i>q.v.</i>}).</p>
   *
   * @return a non-{@code null}, determinate {@link AttributedType}
   *
   * @exception NullPointerException if the default implementation of this method receives a {@code null} return value
   * from an invocation of the {@link #eventDependency()} method
   *
   * @see #eventDependency()
   */
  @Override // AttributedTypedAggregate (AttributedTyped)
  public default AttributedType attributedType() {
    return this.eventDependency().attributedType();
  }
  
  /**
   * Returns a non-{@code null}, determinate, immutable {@link SequencedSet} of {@link AttributedElement}s representing
   * dependencies this {@link EventListener} has that must be resolved before any invocation of the {@link
   * #eventReceived(Object, ReferencesSelector)} method may properly occur.
   *
   * <p>Implementations of this method must not include a result of any invocation of the {@link #eventDependency()}
   * method as an element of the return value.</p>
   *
   * <p>The default implementation of this method returns an {@linkplain SequencedSet#isEmpty() empty} {@link
   * SequencedSet}. Overrides are expected.</p>
   *
   * @return a non-{@code null}, determinate, immutable {@link SequencedSet} of {@link AttributedElement}s
   *
   * @see #eventReceived(Object, ReferencesSelector)
   *
   * @see #eventDependency()
   */
  // Returns dependencies that are not the event dependency. These are resolved by the system. Think of an observer
  // method with an observed parameter and other parameters. The other parameters are these dependencies.
  @Override // AttributedTypedAggregate (Aggregate)
  public default SequencedSet<AttributedElement> dependencies() {
    return Aggregate.super.dependencies();
  }

  /**
   * Returns a non-{@code null}, determinate {@link AttributedElement} representing the program element for which an
   * event is destined.
   *
   * <p>The result of an invocation of this method must not appear as an element of the return value of an invocation of
   * the {@link #dependencies()} method.</p>
   *
   * <p>Note that the default implementation of the {@link #attributedType()} method calls this method and requires it
   * to return a non-{@code null} value.</p>
   *
   * @return a non-{@code null}, determinate {@link AttributedElement}
   *
   * @see #dependencies()
   */
  // An AttributedElement describing the event event "slot" (the observed parameter).
  // Conceptually just another dependency (see dependencies()) but it is supplied by the user, not the system.
  // Normally a method parameter.
  // T's argument (the event event type) must be among its types.
  public AttributedElement eventDependency();

  /**
   * Receives and handles an event, normally as delivered by an invocation of the {@link Events#fire(TypeMirror, List,
   * Object, ReferencesSelector)} method, returning the result (which is often {@link Void} ({@code null})).
   *
   * @param event the event; must not be {@code null}
   *
   * @param r a {@link ReferencesSelector} typically used to {@linkplain #assign(java.util.Function) assign} {@linkplain
   * #dependencies() dependencies}; must not be {@code null}
   *
   * @return the result of reception, which may be {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   */
  // R must be compatible with attributedType().
  // Implementation must arrange for all dependencies() to be assigned, probably using r.
  // Implementation must arrange for event to be assigned to eventDependency(), whatever that might mean. (Normally a method call.)
  // Implementation must return the result of reception (usually Void, so null).
  // When invoked by the system, r should probably also be an AutoCloseableRegistry and an AutoCloseable.
  public R eventReceived(final E event, final ReferencesSelector r);
  
}
