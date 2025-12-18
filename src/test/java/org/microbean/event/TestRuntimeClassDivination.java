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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestRuntimeClassDivination {

  private TestRuntimeClassDivination() {
    super();
  }

  @Test
  final void testReflectionAssumptionsAboutRawArrayList() {
    // Generic superclass is a Class (specifically ArrayList.class); no type arguments
    assertSame(ArrayList.class, RawArrayList.class.getGenericSuperclass());
    assertEquals(1, ArrayList.class.getTypeParameters().length);
  }

  @Test
  final void testReflectionAssumptionsAboutCookedArrayList() {
    // Generic superclass is a ParameterizedType (specifically ArrayList<String>); one type argument
    final ParameterizedType pt = (ParameterizedType)CookedArrayList.class.getGenericSuperclass();
    assertSame(ArrayList.class, pt.getRawType());
    final Type[] tas = pt.getActualTypeArguments();
    assertEquals(1, tas.length);
    assertSame(String.class, tas[0]);
  }

  @Test
  final void testOuterInner() {
    final Outer<Integer>.Inner<String> i = new Outer<Integer>().new Inner<String>();
    assertSame(Outer.Inner.class, i.getClass());
    assertSame(Outer.class, Outer.Inner.class.getEnclosingClass());
    assertSame(this.getClass(), Outer.class.getEnclosingClass());

    // Get ArrayList<O>. Can't get ArrayList<Integer>.
    final ParameterizedType innerSupertype = (ParameterizedType)Outer.Inner.class.getGenericSuperclass();
    assertSame(ArrayList.class, innerSupertype.getRawType());
    final Type[] innerSupertypeTypeArguments = innerSupertype.getActualTypeArguments();
    assertEquals(1, innerSupertypeTypeArguments.length);
    assertTrue(innerSupertypeTypeArguments[0] instanceof TypeVariable<?>);
   
  }

  @SuppressWarnings("rawtypes")
  private static final class RawArrayList extends ArrayList {
    private static final long serialVersionUID = 1L;
  }

  private static final class CookedArrayList extends ArrayList<String> {
    private static final long serialVersionUID = 1L;
  }

  private static final class Outer<O> {
    private final class Inner<I> extends ArrayList<O> {
      private static final long serialVersionUID = 1L;
    }
  }

}
