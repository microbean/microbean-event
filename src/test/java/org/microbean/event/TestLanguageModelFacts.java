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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.junit.jupiter.api.Test;

import org.microbean.construct.DefaultDomain;
import org.microbean.construct.Domain;

import org.microbean.construct.element.UniversalElement;

import org.microbean.construct.type.UniversalType;

import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.METHOD;

import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.TYPEVAR;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestLanguageModelFacts {

  private static final Domain domain = new DefaultDomain();

  private TestLanguageModelFacts() {
    super();
  }

  @Test
  final void testAnnotationMirrorEnclosingAndEnclosedElements() {
    final TypeElement e = domain.typeElement("java.security.AccessControlException"); // has @Deprecated on it
    final List<? extends AnnotationMirror> as = e.getAnnotationMirrors();
    assertEquals(1, as.size());
    final AnnotationMirror deprecated = as.get(0);
    final Map<? extends ExecutableElement, ? extends AnnotationValue> m = deprecated.getElementValues();
    assertEquals(2, m.size(), String.valueOf(m)); // since, forRemoval
    // Just get whichever one happens to come up
    final ExecutableElement element = m.keySet().iterator().next();
    final TypeElement deprecatedElement = (TypeElement)element.getEnclosingElement();
    assertSame(ANNOTATION_TYPE, deprecatedElement.getKind());
    final List<? extends Element> enclosedElements = deprecatedElement.getEnclosedElements();
    assertEquals(2, enclosedElements.size());
    enclosedElements.forEach(ee -> assertSame(METHOD, ee.getKind()));
    System.out.println("*** modifiers: " + deprecatedElement.getModifiers());
    enclosedElements.forEach(ee -> System.out.println("    modifiers for " + ee + ": " + ee.getModifiers()));

  }
  
  @Test
  final void testPrototypicalTypeAndDeclaredType() {
    final TypeElement string = domain.typeElement("java.lang.String");
    final DeclaredType prototypicalStringType = (DeclaredType)string.asType();
    final DeclaredType stringTypeUsage = domain.declaredType(string);
    assertTrue(string instanceof UniversalElement);
    assertTrue(prototypicalStringType instanceof UniversalType);
    assertTrue(stringTypeUsage instanceof UniversalType);
    assertNotSame(prototypicalStringType, stringTypeUsage);
    // This surprises me, but it ends up being true because types in the compiler are always compared by identity, and
    // UniversalType delegates to its delegate's equals() implementation. So the prototype type is indeed not the same
    // as and therefore not equal to the type usage.
    assertNotEquals(prototypicalStringType, stringTypeUsage);
    assertTrue(domain.sameType(prototypicalStringType, stringTypeUsage));
  }

  @Test
  final void testRawPropagation() {
    final DeclaredType arrayListTU = domain.declaredType(domain.typeElement("java.util.ArrayList"));
    assertTrue(arrayListTU.getTypeArguments().isEmpty());

    // Hypothesis: if you use directSupertypes, it will sub in the type arguments. (For inferring event type arguments
    // that's not what we want.) If you supply it a raw type, then all the supertypes will be raw as well. Again, not
    // what we want.
    assertEquals(List.of(), ((DeclaredType)domain.directSupertypes(arrayListTU).get(0)).getTypeArguments());
  }

  @Test
  final void testNoTypeVariablePropagationInTypeUsages() {
    final TypeElement arrayList = domain.typeElement("java.util.ArrayList");
    final DeclaredType abstractListSC = (DeclaredType)arrayList.getSuperclass();
    final TypeElement abstractList = (TypeElement)abstractListSC.asElement();
    assertEquals(domain.typeElement("java.util.AbstractList"), abstractList);
    final TypeParameterElement abstractListTPE = abstractList.getTypeParameters().get(0);
    final TypeVariable abstractListTV = (TypeVariable)abstractListTPE.asType();
    assertEquals(abstractListTPE, abstractListTV.asElement()); // note that the arraylist E died
  }

  @Test
  final void testJLSViolation() {
    final TypeElement arrayList = domain.typeElement("java.util.ArrayList");
    final TypeElement string = domain.typeElement("java.lang.String");
    final DeclaredType arrayListTU = domain.declaredType(arrayList, string.asType());
    final List<? extends TypeMirror> directSupertypes = domain.directSupertypes(arrayListTU);
    // The first type in the list/set is AbstractList<String>.
    final DeclaredType abstractListST = (DeclaredType)directSupertypes.get(0);
    assertEquals(1, abstractListST.getTypeArguments().size());
    // The next type in the list/set is List<String>. Interface types are guaranteed to appear after non-interface types
    // so this shows that the raw type represented by, simply, ArrayList does not appear in the set. The JLS says it
    // should, among other types. See also https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.10.2 and
    // https://bugs.openjdk.org/browse/JDK-8055219 and
    // https://stackoverflow.com/questions/79817198/why-does-directsupertypes-not-return-a-raw-type-as-required-by-jls-4-10.
    //
    // My guess is that Types#directSupertypes(TypeMirror) should *really* be specified to return direct supertypes
    // *that can be declared in the Java language*. This would make sense given that the annotation processing model was
    // partially reverse engineered out of the existing guts of javac. So, for example, since you can write neither:
    //
    //   // Invalid Java
    //   public class ArrayList<E> extends ArrayList, AbstractList<E>...
    //
    // ...nor:
    //
    //   // Invalid Java
    //   public class ArrayLIst<E> extends ArrayList<? extends WTF>...
    //
    // ...nor anything analogous, the types represented by ArrayList and ArrayList<? extends WTF> will not appear in the
    // return value.
    assertTrue(((DeclaredType)directSupertypes.get(1)).asElement().getKind().isInterface());
  }

  @Test
  final void testPrototypicalTypeStructure() {

    // ArrayList in ArrayList<E>. The declaring type element.
    //
    //  <<TypeElement>>
    //  (arrayList)
    // +---------------+
    // |    ArrayList  |
    // +---------------+
    //
    final TypeElement arrayList = domain.typeElement("java.util.ArrayList");

    // <E> in ArrayList<E>. The declaring type parameter element.
    //
    //  <<TypeElement>>     <<TypeParameterElement>>
    //  (arrayList)         (arrayListE)
    // +---------------+   +------------------------+
    // |    ArrayList  <--->            E           |
    // +---------------+   +------------------------+
    //
    final TypeParameterElement arrayListE = (TypeParameterElement)arrayList.getTypeParameters().get(0);
    assertEquals(arrayList, arrayListE.getGenericElement());

    // The type variable declared by <E>.
    //
    //  <<TypeElement>>     <<TypeParameterElement>>
    //  (arrayList)         (arrayListE)
    // +---------------+   +------------------------+
    // |    ArrayList  <--->            E           |
    // +---------------+   +------------^-----------+
    //                                  |
    //                                  |
    //                                  V
    //                            <<TypeVariable>>
    //                            (arrayListETV)
    //                           +----------------+
    //                           |      (E)       |
    //                           +----------------+
    //
    final TypeVariable arrayListETV = (TypeVariable)arrayListE.asType();
    assertEquals(arrayListE, arrayListETV.asElement());
    assertEquals(arrayListETV, arrayListE.asType());

    // The prototypical type declared by ArrayList<E>.
    //
    //  <<TypeElement>>     <<TypeParameterElement>>
    //  (arrayList)         (arrayListE)
    // +---------------+   +------------------------+
    // |    ArrayList  <--->            E           |
    // +--------^------+   +------------^-----------+
    //          |                       |
    //          V                       |
    //  <<DeclaredType>>                V
    //  <<prototypical type>>     <<TypeVariable>>
    //  (arrayListPT)             (arrayListETV)
    // +---------------------+   +----------------+
    // |     (ArrayList)     |   |      (E)       |
    // +---------------------+   +----------------+
    final DeclaredType arrayListPT = (DeclaredType)arrayList.asType();
    assertEquals(arrayList, arrayListPT.asElement());

    // Its type argument is the type variable underlying <E>. This completes a cycle:
    //
    //  <<TypeElement>>     <<TypeParameterElement>>
    //  (arrayList)         (arrayListE)
    // +---------------+   +------------------------+
    // |    ArrayList  <--->            E           |
    // +--------^------+   +------------^-----------+
    //          |                       |
    //          V                       |
    //  <<DeclaredType>>                V
    //  <<prototypical type>>     <<TypeVariable>>
    //  (arrayListPT)             (arrayListETV)
    // +---------------------+   +----------------+
    // |     (ArrayList)     +--->      (E)       |
    // +---------------------+   +----------------+
    //
    // The prototypical type is not raw, because it _does_ have type arguments. The type argument is a type variable,
    // not nothing.
    assertEquals(arrayListETV, arrayListPT.getTypeArguments().get(0));

  }

  @Test
  final void testAsMemberOf() {
    final TypeElement arrayList = domain.typeElement("java.util.ArrayList");
    final TypeElement list = domain.typeElement("java.util.List");
    final TypeElement string = domain.typeElement("java.lang.String");

    final DeclaredType stringTypeUsage = domain.declaredType(string);
    final DeclaredType listStringTypeUsage = domain.declaredType(list, stringTypeUsage);

    final DeclaredType arrayListRawTypeUsage = domain.declaredType(arrayList);

    // Can we do this? No.
    assertThrows(IllegalArgumentException.class, () -> domain.asMemberOf(listStringTypeUsage, arrayList));

    // OK, can we do this? No.
    assertThrows(IllegalArgumentException.class, () -> domain.asMemberOf(listStringTypeUsage, domain.typeParameterElement(arrayList, "E")));
  }

  @Test
  final void testRawTypeRepresentation() {
    final List<String> l = new ArrayList<>();

    // Here's a TypeElement representing ArrayList (not ArrayList<String>, not ArrayList<?>, not really ArrayList<E>).
    final TypeElement list = domain.typeElement(l.getClass().getCanonicalName());

    // rawArrayListTypeUsage here is a *usage* of the type declared by the java.util.ArrayList TypeElement. This usage has no
    // type arguments. This makes the declared type "in" this usage a raw type.
    final DeclaredType rawArrayListTypeUsage = domain.declaredType(list); // note: no type arguments supplied
    assertSame(DECLARED, rawArrayListTypeUsage.getKind());

    // rawArrayListTypeUsage has no type arguments. (It can't; none were supplied. See above.)
    List<? extends TypeMirror> typeArguments = rawArrayListTypeUsage.getTypeArguments();
    assertEquals(0, typeArguments.size());

    // The declared type's type element has one type parameter element (E). The fact that the declared type's type
    // argument count is zero and its delcaring type element's type parameter count is one is what makes the declared
    // type raw.
    final TypeElement e = (TypeElement)rawArrayListTypeUsage.asElement();
    assertSame(CLASS, e.getKind());
    assertEquals(list, e); // basically identical, but always call equals() to allow delegates and wrappers to work

    // java.util.ArrayList's sole type parameter element is "E" (as in public class java.util.ArrayList<E>...).
    final List<? extends TypeParameterElement> typeParameterElements = list.getTypeParameters();
    assertEquals(1, typeParameterElements.size());
    final TypeParameterElement typeParameterElement = typeParameterElements.get(0);
    assertTrue(typeParameterElement.getSimpleName().contentEquals("E"));

    // This type parameter element declares a type variable (definitionally unnamed).
    TypeVariable tv = (TypeVariable)typeParameterElement.asType();

    // (The type variable can get its declaring type parameter element.)
    assertEquals(typeParameterElement, (TypeParameterElement)tv.asElement());

    // Here is the type as it was *declared* by the java.util.ArrayList<E> TypeElement. Note that it *does* have type
    // arguments. This is distinct from rawArrayListTypeUsage above, which represents a declared type *usage*.
    final DeclaredType listDeclarationType = (DeclaredType)list.asType();
    assertSame(DECLARED, listDeclarationType.getKind());
    assertEquals(list, listDeclarationType.asElement());

    // So what *are* the type arguments in a type declaration created/implied by a TypeElement? The TypeVariables
    // declared by the TypeParameterElements.
    typeArguments = listDeclarationType.getTypeArguments();
    assertEquals(1, typeArguments.size());
    assertEquals(tv, typeArguments.get(0));

    // To recap so far:
    //
    // In this partial code snippet:
    //
    // public class ArrayList<E> ...
    //
    // * The TypeElement is named java.util.ArrayList
    // * It has one TypeParameterElement whose name is E
    // * That TypeParameterElement's asType() method returns a TypeVariable "backing" E
    // * The TypeVariable's asElement() method returns the TypeParameterElement (so mutual dependency)
    // * The TypeElement's asType() method returns a DeclaredType representing the actual declaration type
    // * This DeclaredType has the same number of type arguments as its declaring TypeElement has TypeParameterElements
    // * The sole type argument is the TypeVariable mentioned above
    //
    // In this partial code snippet:
    //
    // DeclaredType rawArrayListTypeUsage = domain.declaredType(domain.typeElement("java.util.ArrayList"));
    //
    // * rawArrayListTypeUsage is a type *usage*
    // * It has no type arguments at all
    // * Therefore it is a raw type (a raw type usage)
    // * Its asElement() returns the TypeElement declared by "public class ArrayList<E>"
    //   * Note that that TypeElement's asType() method does NOT return this type usage, but returns the type
    //     declaration "backing" the TypeElement
    //   * As previously noted it DOES have type arguments
    //
    //       +---------------------+              +---------------------------------+
    //       | TypeElement (named) <1--declares--1> DeclaredType (type declaration) +-----+
    //       +-----^------------^--+              +---------------------------------+     |type arguments
    //             1            1                         (equal to type parameter count) *
    //        ^    |            |            +------------------------------+     +-------v------+
    //        uses |            +-----------*> TypeParameterElement (named) <1---1> TypeVariable |
    //             |                         +------------------------------+     +--------------+
    //       +-----+---------------------+   +------------------------------------------------+
    //       | DeclaredType (type usage) +--*> TypeMirror (type argument)                     |
    //       +---------------------------+   |                                                |
    //                                       | (one of DeclaredType, ArrayType, TypeVariable) |
    //                                       | (TypeVariable may be surprising, but consider  |
    //                                       | a type usage in an "extends" or "implements"   |
    //                                       | clause)                                        |
    //                                       +------------------------------------------------+

  }

  private static class Sup<S, T> {

    private Sup() {
      super();
    }

  }

  private static final class Sub<A, B> extends Sup<B, A> {

    private Sub() {
      super();
    }

  }

}
