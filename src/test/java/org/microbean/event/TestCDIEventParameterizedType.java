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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.event.Event;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import jakarta.enterprise.util.TypeLiteral;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestCDIEventParameterizedType {

  private SeContainer c;
  
  private TestCDIEventParameterizedType() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.c = SeContainerInitializer.newInstance()
      .disableDiscovery()
      .addBeanClasses(DummyBean.class)
      .initialize();
  }

  @AfterEach
  final void teardown() {
    if (this.c != null) {
      this.c.close();
    }
  }

  @Test
  final void testFireParameterizedSubtype() {
    final Event<ArrayList<String>> e = this.c.select(new TypeLiteral<Event<ArrayList<String>>>() {}).get();
    final ArrayList<String> l = new ArrayListWithTwoTypeVariables<String, Integer>();
    // l's raw type is ArrayListWithTwoTypeVariables.
    // The specified type is ArrayList<String>, so we can infer one of two type arguments. We can't infer the second
    // one, so this fails.
    assertThrows(IllegalArgumentException.class, () -> e.fire(l));
  }
  
  @Test
  final void testFireArrayListString() {
    final Event<ArrayList<String>> e = this.c.select(new TypeLiteral<Event<ArrayList<String>>>() {}).get();
    // The CDI specification says that the event types of an object serving as an event "include all superclasses and
    // interfaces of the runtime class of the event object". Somewhat interestingly, the "runtime class" of the event
    // being fired here is, of course, simply, java.util.ArrayList. Nevertheless, in Weld, one of the event types of the
    // event is java.util.ArrayList<String> (set a breakpoint on the fire() call below and find out for
    // yourself). java.util.ArrayList<String> is a ParameterizedType and not a "runtime class" so this is kind of
    // interesting. See also https://github.com/jakartaee/cdi/issues/884.
    e.fire(new ArrayList<String>());
  }

  @Test
  final void testFireSubtypeWithWildcards0() {
    final Event<? super List<String>> e = this.c.select(new TypeLiteral<Event<List<String>>>() {}).get();
    e.fire(new ArrayList<String>());
  }

  @Test
  final void testFireSubtypeWithWildcards1() {
    final Event<? super List<String>> e = this.c.select(new TypeLiteral<Event<? super List<String>>>() {}).get();
    // Weld really should permit this
    assertThrows(IllegalArgumentException.class, () -> e.fire(new ArrayList<String>()));
  }

  @Test
  final void testFireSubtypeWithWildcards2() {
    final Event<? super List<? extends String>> e = this.c.select(new TypeLiteral<Event<? super List<? extends String>>>() {}).get();
    // Weld really should permit this
    assertThrows(IllegalArgumentException.class, () -> e.fire(new ArrayList<String>()));
  }

  @Test
  final void testFireOuterInner() {
    final Event<Outer<Integer>.Inner<String>> e = this.c.select(new TypeLiteral<Event<Outer<Integer>.Inner<String>>>() {}).get();
    e.fire(new Outer<Integer>().new Inner<String>());
  }

  @Test
  final void testInstanceWithWildcards() {
    // Obvious.
    assertNotNull(this.c.select(DummyBean.class).get());
    // Same formulation, just using TypeLiteral. Obvious.
    assertNotNull(this.c.select(new TypeLiteral<DummyBean>() {}).get());
    // Maybe less obvious at first glance, but still obvious.
    assertNotNull(this.c.select(new TypeLiteral<Instance<DummyBean>>() {}).get().get());
    // Kind of weird until you remember that wildcards are illegal bean types. Still kind of weird. It is true that if
    // this *did* return an unknown-type-that-extends-DummyBean it's not clear you could call destroy(Object) or
    // remove(Object) on the iterator.
    assertThrows(UnsatisfiedResolutionException.class, this.c.select(new TypeLiteral<Instance<?>>() {}).get()::get);
    assertThrows(UnsatisfiedResolutionException.class, this.c.select(new TypeLiteral<Instance<? extends DummyBean>>() {}).get()::get);
    // See above. Here you wouldn't be able to iterate.
    assertThrows(UnsatisfiedResolutionException.class, this.c.select(new TypeLiteral<Instance<? super DummyBean>>() {}).get()::get);
  }

  @Test
  final void testInference() throws ReflectiveOperationException {
    final Event<Sup<String, Object>> e = this.c.select(new TypeLiteral<Event<Sup<String, Object>>>() {}).get();
    final Sub<Object, String> sub = new Sub<>();
    final Method m = e.getClass().getDeclaredMethod("getEventType", Class.class);
    assertTrue(m.trySetAccessible());
    final ParameterizedType p = (ParameterizedType)m.invoke(e, sub.getClass());
    assertSame(p.getRawType(), Sub.class);
    assertSame(Object.class, p.getActualTypeArguments()[0]);
    assertSame(String.class, p.getActualTypeArguments()[1]);
  }

  private static class Sup<X, Y> {}

  private static class Sub<A, B> extends Sup<B, A> {}  

  private static final class DummyBean {}

  private static final class Outer<O> {
    private final class Inner<I> extends ArrayList<O> {
      private static final long serialVersionUID = 1L;
    }
  }

  private static final class ArrayListWithTwoTypeVariables<X, Y> extends ArrayList<X> {
    private static final long serialVersionUID = 1L;
  };
  
}
