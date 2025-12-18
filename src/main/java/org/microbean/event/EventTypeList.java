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

import javax.lang.model.type.TypeMirror;

import org.microbean.assign.SupertypeList;

import static java.util.Objects.requireNonNull;

/**
 * An immutable {@link AbstractList} of {@link TypeMirror}s that contains only {@linkplain
 * EventTypes#legalEventType(TypeMirror) legal event types}, sorted in a specific manner.
 *
 * <p><strong>Note:</strong> Two {@link TypeMirror} instances may represent the {@linkplain
 * org.microbean.construct.Domain#sameType(TypeMirror, TypeMirror) same type} while not being {@linkplain
 * TypeMirror#equals(Object) equal to} one another. {@link java.util.List} implementations such as this one that contain
 * {@link TypeMirror} elements may also represent the same types without being equal to each other.</p>
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 */
public final class EventTypeList extends AbstractList<TypeMirror> {

  static final EventTypeList EMPTY_LIST = new EventTypeList(SupertypeList.of());
  
  private final SupertypeList types;
  
  EventTypeList(final SupertypeList types) {
    super();
    this.types = requireNonNull(types, "types");
  }

  @Override // AbstractList<TypeMirror>
  public final TypeMirror get(final int index) {
    return this.types.get(index);
  }
  
  @Override // AbstractList<TypeMirror>
  public final int size() {
    return this.types.size();
  }
  
}
