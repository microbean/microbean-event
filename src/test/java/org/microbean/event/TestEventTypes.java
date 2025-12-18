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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.microbean.construct.DefaultDomain;
import org.microbean.construct.Domain;

import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.INT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestEventTypes {

  private Domain domain;

  private EventTypes eventTypes;
  
  private TestEventTypes() {
    super();
  }

  @BeforeEach
  final void setup() {
    this.domain = new DefaultDomain();
    this.eventTypes = new EventTypes(this.domain);
  }

  @Test
  final void testEventTypeCase1() {
    final TypeMirror na = domain.declaredType(domain.javaLangObject());
    final TypeMirror rv = eventTypes.eventType(na, int.class);
    assertTrue(rv instanceof PrimitiveType);
    assertSame(INT, rv.getKind());
  }

  @Test
  final void testEventTypeCase2() {
    final TypeMirror na = domain.declaredType(domain.javaLangObject());
    final TypeMirror rv = eventTypes.eventType(na, int[].class);
    assertSame(ARRAY, rv.getKind());
    assertSame(INT, ((ArrayType)rv).getComponentType().getKind());
  }

  @Test
  final void testEventTypeCase3() {
    final TypeMirror na = domain.declaredType(domain.javaLangObject());
    final TypeMirror rv = eventTypes.eventType(na, Object.class);
    assertSame(DECLARED, rv.getKind());
    assertTrue(domain.javaLangObject(rv));
  }

  @Test
  final void testEventTypeCase4() {
    final TypeMirror na = domain.declaredType(domain.javaLangObject());
    final TypeMirror rv = eventTypes.eventType(na, Object[].class);
    assertSame(ARRAY, rv.getKind());
    assertTrue(domain.javaLangObject(((ArrayType)rv).getComponentType()));
  }

  @Test
  final void testEventTypeCase5() {
    final TypeMirror intScalar = domain.primitiveType(INT);
    assertThrows(IllegalArgumentException.class, () -> eventTypes.eventType(intScalar, ArrayList.class));
  }

  @Test
  final void testEventTypeCase6() {
    final TypeMirror intArray = domain.arrayTypeOf(domain.primitiveType(INT));
    assertThrows(IllegalArgumentException.class, () -> eventTypes.eventType(intArray, ArrayList.class));
  }

  @Test
  final void testEventTypeCase7() {
    final TypeMirror list = domain.declaredType(domain.typeElement(List.class.getCanonicalName()));
    assertThrows(IllegalArgumentException.class, () -> eventTypes.eventType(list, ArrayList.class));
  }

  @Test
  final void testEventTypeCase8() {
    final TypeMirror listArray = domain.arrayTypeOf(domain.declaredType(domain.typeElement(List.class.getCanonicalName())));
    assertThrows(IllegalArgumentException.class, () -> eventTypes.eventType(listArray, ArrayList.class));
  }

  @Test
  final void testEventTypeCase9() {
    final TypeMirror string = domain.declaredType(domain.typeElement(String.class.getCanonicalName()));
    final TypeMirror abstractListString = domain.declaredType(domain.typeElement(AbstractList.class.getCanonicalName()), string);
    assertTrue(domain.sameType(domain.declaredType(domain.typeElement(ArrayList.class.getCanonicalName()), string),
                               eventTypes.eventType(abstractListString, ArrayList.class)));
  }

  @Test
  final void testEventTypeCase10() {
    final TypeMirror string = domain.declaredType(domain.typeElement(String.class.getCanonicalName()));
    final TypeMirror listStringArray = domain.arrayTypeOf(domain.declaredType(domain.typeElement(List.class.getCanonicalName()),
                                                                              string));
    assertTrue(domain.sameType(domain.arrayTypeOf(domain.declaredType(domain.typeElement(ArrayList.class.getCanonicalName()),
                                                                      string)),
                               eventTypes.eventType(listStringArray, ArrayList[].class)));
  }

  @Test
  final void testEventTypeCase11() {
    final TypeMirror string = domain.declaredType(domain.typeElement(String.class.getCanonicalName()));
    final TypeMirror listStringArray = domain.arrayTypeOf(domain.declaredType(domain.typeElement(List.class.getCanonicalName()),
                                                                              string));
    assertTrue(domain.sameType(domain.declaredType(domain.typeElement(ArrayList.class.getCanonicalName()), string),
                               eventTypes.eventType(listStringArray, ArrayList.class)));
  }

  
}
