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

import java.lang.System.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import org.microbean.assign.SupertypeList;
import org.microbean.assign.Types;

import org.microbean.construct.Domain;

import static javax.lang.model.element.Modifier.ABSTRACT;

import static java.lang.System.Logger.Level.WARNING;

import static java.util.HashMap.newHashMap;

import static java.util.Objects.requireNonNull;

import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.BOOLEAN;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.TYPEVAR;
import static javax.lang.model.type.TypeKind.VOID;
import static javax.lang.model.type.TypeKind.WILDCARD;

/**
 * A utility for working with <dfn>event types</dfn>.
 *
 * @author <a href="https://about.me/lairdnelson" target="_top">Laird Nelson</a>
 *
 * @see #eventTypes(TypeMirror, Object)
 *
 * @see #legalEventType(TypeMirror)
 */
public final class EventTypes extends Types {


  /*
   * Static fields.
   */


  private static final Logger LOGGER = System.getLogger(EventTypes.class.getName());


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link EventTypes}.
   *
   * @param domain a {@link Domain}; must not be {@code null}
   *
   * @exception NullPointerException if {@code domain} is {@code null}
   */
  public EventTypes(final Domain domain) {
    super(domain);
  }


  /*
   * Instance methods.
   */


  /**
   * Returns an {@link EventTypeList} of {@linkplain #legalEventType(TypeMirror) legal event types} that the supplied
   * {@link TypeMirror} bears.
   *
   * <p>The returned {@link EventTypeList} may be {@linkplain EventTypeList#isEmpty() empty}.</p>
   *
   * @param typeArgumentSource a {@link TypeMirror} used for <dfn>{@index "event type argument inference"}</dfn>; must
   * not be {@code null}; must {@linkplain TypeMirror#getKind() have a kind} of either {@link TypeKind#ARRAY} or {@link
   * TypeKind#DECLARED}
   *
   * @param event the event object whose event types should be returned; must not be {@code null}
   *
   * @return an {@link EventTypeList}; never {@code null}
   *
   * @exception NullPointerException if any argument is {@code null}
   *
   * @exception IllegalArgumentException if {@code typeArgumentSource} is not legal
   */
  public final EventTypeList eventTypes(final TypeMirror typeArgumentSource, final Object event) {
    return new EventTypeList(this.supertypes(this.eventType(typeArgumentSource, event.getClass()), EventTypes::legalEventType));
  }

  // This MUST NOT become public.
  final TypeMirror eventType(final TypeMirror typeArgumentSource, final Class<?> eventClass) {
    if (eventClass == void.class) {
      // Optimization: void.class is never the answer.
      throw new IllegalArgumentException("eventClass: " + eventClass);
    }
    if (eventClass.isPrimitive()) {
      // Optimization: if it is primitive, there's no further work to be done.
      return this.domain().primitiveType(eventClass.getCanonicalName());
    }
    int dimensions = 0;
    Class<?> c = eventClass;
    while (c.isArray()) {
      c = c.getComponentType();
      ++dimensions;
    }
    final Domain d = this.domain();
    TypeMirror t =
      this.eventType(typeArgumentSource,
                     c.isPrimitive() ? d.primitiveType(c.getCanonicalName()) : d.declaredType(d.typeElement(c.getCanonicalName())));
    for (int i = 0; i < dimensions; i++) {
      t = d.arrayTypeOf(t);
    }
    return t;
  }

  // This MUST NOT become public.
  private final TypeMirror eventType(final TypeMirror typeArgumentSource, final TypeMirror eventType) {
    //
    // Given:
    //
    //   class Sup<S, T> {}
    //   class Sub<A, B> extends Sup<T, S> {}
    //   class Sub2<T> extends Sup<String, T> {}
    //
    //       +-----------------------+-------------+--------------------------+---------------------------------------------------+
    //       | typeArgumentSource    | eventType   | return value/exception   | notes                                             |
    // +-----+-----------------------+-------------+--------------------------+---------------------------------------------------+
    // |  1. | N/A                   | int         | int                      | (typeArgumentSource not checked or used)          |
    // |  2. | N/A                   | int[]       | int[]                    | (typeArgumentSource not checked or used)          |
    // |  3. | N/A                   | Object      | Object                   | (typeArgumentSource not checked or used)          |
    // |  4. | N/A                   | Object[]    | Object[]                 | (typeArgumentSource not checked or used)          |
    // |  5. | int                   | ArrayList   | IllegalArgumentException | (typeArgumentSource cannot supply type arguments) |
    // |  6. | int[]                 | ArrayList   | IllegalArgumentException | (typeArgumentSource cannot supply type arguments) |
    // |  7. | List                  | ArrayList   | IllegalArgumentException | (typeArgumentSource cannot supply type arguments) |
    // |  8. | List[]                | ArrayList   | IllegalArgumentException | (typeArgumentSource cannot supply type arguments) |
    // |  9. | AbstractList<String>  | ArrayList   | ArrayList<String>        |                                                   |
    // | 10. | List<String>[]        | ArrayList[] | ArrayList<String>[]      |                                                   |
    // | 11. | List<String>[]        | ArrayList   | ArrayList<String>        | (typeArgumentSource dimensions don't matter)      |
    // | 12. | List<T>               | ArrayList   | IllegalArgumentException | (type arguments must be declared or array types)  |
    // | 13. | List<T>[]             | ArrayList   | IllegalArgumentException | (type arguments must be declared or array types)  |
    // | 14. | List<?>               | ArrayList   | IllegalArgumentException | (type arguments must be declared or array types)  |
    // | 15. | List<?>[]             | ArrayList   | IllegalArgumentException | (type arguments must be declared or array types)  |
    // | 16. | Map<String, String>   | ArrayList   | IllegalArgumentException | (typeArgumentSource is not a supertype)           |
    // | 17. | Sup<String, Object>   | Sub         | Sub<Object, String>      |                                                   |
    // | 18. | Sup<String, Object>   | Sub2        | Sub2<Object>             |                                                   |
    // +-----+-----------------------+-------------+--------------------------+---------------------------------------------------+
    //
    return switch (eventType.getKind()) {
    case ARRAY -> {
      final Domain d = this.domain();
      int dimensions = 1;
      TypeMirror t = ((ArrayType)eventType).getComponentType();
      while (t.getKind() == ARRAY) {
        t = ((ArrayType)t).getComponentType();
        ++dimensions;
      }
      yield switch (t.getKind()) {
      case ARRAY -> throw new AssertionError();
      case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> eventType; // Case 2 above
      case DECLARED -> {
        t = this.eventType(typeArgumentSource, (DeclaredType)t); // see below
        assert t.getKind() == DECLARED;
        for (int i = 0; i < dimensions; i++) {
          t = d.arrayTypeOf(t);
        }
        yield t;
      }
      default -> throw new IllegalArgumentException("eventType: " + eventType);
      };
    }
    case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> eventType; // Case 1 above
    case DECLARED -> this.eventType(typeArgumentSource, (DeclaredType)eventType); // see below
    default -> throw new IllegalArgumentException("eventType: " + eventType);
    };
  }

  private final TypeMirror eventType(TypeMirror typeArgumentSource, final DeclaredType eventType) {
    if (eventType.getKind() != DECLARED) {
      throw new IllegalArgumentException("eventType: " + eventType);
    }

    final TypeElement eventTypeElement = (TypeElement)eventType.asElement();
    final int typeParametersCount = eventTypeElement.getTypeParameters().size();
    if (typeParametersCount <= 0) {
      // Non-generic declared type. Case 3 above (and 4 by extension).
      return eventType;
    }

    // Optimization: if all the event type type arguments are already of kind ARRAY or DECLARED or WILDCARD, then we're
    // done. Otherwise there is at least one type argument that has to be inferred.
    final List<? extends TypeMirror> eventTypeTypeArguments = eventType.getTypeArguments();
    OPTIMIZATION:
    if (!eventTypeTypeArguments.isEmpty()) {
      for (final TypeMirror ta : eventTypeTypeArguments) {
        switch (ta.getKind()) {
        case TYPEVAR:
          // Optimization does not apply; break out of the OPTIMIZATION block entirely
          break OPTIMIZATION;
        case ARRAY:
        case DECLARED:
        case WILDCARD:
          break; // go to the next for loop iteration
        default:
          throw new AssertionError("ta: " + ta);
        }
      }
      return eventType;
    }

    // "De-arrayize" the specified type; whether it is an array or not does not matter. We only care about its element
    // type since that is the source of inferred/replacement type arguments.
    final TypeMirror originalTypeArgumentSource = typeArgumentSource;
    while (typeArgumentSource.getKind() == ARRAY) {
      typeArgumentSource = ((ArrayType)typeArgumentSource).getComponentType();
    }
    if (typeArgumentSource.getKind() != DECLARED) {
      throw new IllegalArgumentException("typeArgumentSource: " + originalTypeArgumentSource + "; eventType: " + eventType);
    }

    final TypeElement typeArgumentSourceElement = (TypeElement)((DeclaredType)typeArgumentSource).asElement();

    // The event type is a generic declared type. As we begin, it is important in what follows to use the prototypical
    // type, not a raw type. The prototypical type will ensure that type parameter arguments (type variables) will be
    // "propagated" up the inheritance hierarchy.
    //
    // (The concept of a prototypical type is not defined in the Java Language Specification but only in the Java
    // language model specification. See
    // https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/lang/model/element/TypeElement.html#prototypicaltype.)
    //
    // For example, suppose the event type is ArrayList<String>. We "go up" to its element via asElement(), yielding the
    // TypeElement ArrayList. Then we "go down" using its asType() argument, which does _not_ yield ArrayList<String>,
    // but, rather, ArrayList<E>. ArrayList<E> is the prototypical type defined by the TypeElement ArrayList. The
    // getSuperclass() method on the ArrayList TypeElement "propagates" E-defined-by-ArrayList "up" to AbstractList<E>,
    // such that the E in AbstractList<E> simply is the E-defined-by-ArrayList.
    final DeclaredType eventPrototypicalType = (DeclaredType)eventTypeElement.asType();
    assert !eventPrototypicalType.getTypeArguments().isEmpty();
    assert allAreTypeVariables(eventPrototypicalType.getTypeArguments());

    // Given, e.g:
    //
    //   class Sup<S, T> {}
    //   class Sub<A, B> extends Sup<B, A> {}
    //
    // ...and:
    //
    //   Sup<String, Object> // specified type
    //
    // ...we want to (ultimately) yield Sub<Object, String>.
    //
    // Visually, in what follows, the "right hand side" is the event type hierarchy starting from the prototypical event
    // type. The "left hand side" is specified type usage.
    //
    // We need to substitute type arguments from the "left hand side" appropriately into the "right hand side".

    // First, get the "congruent supertype" (a term I made up). Given Sub<A, B> (the prototypical event type), yield
    // Sup<B, A> (not Sup<S, T>). It is congruent with Sup<String, Object> in some fashion.
    final DeclaredType rhs = congruentSupertype(typeArgumentSourceElement, eventPrototypicalType);
    if (rhs == null) {
      // typeArgumentSource and eventType were distinct types. We COULD take type arguments from the specified type but that
      // would ultimately be very confusing and almost certainly reflects programmer error.
      throw new IllegalArgumentException("typeArgumentSource: " + originalTypeArgumentSource + "; eventType: " + eventType);
    } else if (rhs == eventPrototypicalType) {
      // Optimization:
      //
      // The contract of congruentSupertype() says that when the return value is identical to the second argument then
      // no substitution was necessary. If no substitution was necessary, then just return what we already know.
      return eventType;
    }

    // B, A in Sup<B, A>
    final List<? extends TypeMirror> rhsTypeArguments = rhs.getTypeArguments();
    assert allAreTypeVariables(rhsTypeArguments) : "rhsTypeArguments: " + rhsTypeArguments;

    // Sup in Sup<B, A>
    final TypeElement rhsTypeElement = (TypeElement)rhs.asElement();

    // Now we have the proper left and right hand sides suitable for type argument substitution (Sup<String, Object> and
    // Sup<B, A> respectively).

    // String, Object in Sup<String, Object>
    final List<? extends TypeMirror> lhsTypeArguments = ((DeclaredType)typeArgumentSource).getTypeArguments();

    int size = lhsTypeArguments.size();
    if (size <= 0) {
      // lhs was non-generic or raw and hence is incapable of supplying (needed) type arguments.
      throw new IllegalArgumentException("typeArgumentSource: " + originalTypeArgumentSource + "; eventType: " + eventType);
    }

    // This Map will hold entries mapping, e.g., B to String and A to Object. For visualization purposes, this
    // "connects" the left hand side to the right hand side. The map will have a size that is guaranteed to be exactly
    // equal to the number of type parameters.
    final Map<TypeVariable, TypeMirror> m = newHashMap(size);

    for (int i = 0; i < size; i++) {

      // For every left hand side type argument...

      // On the left hand side:
      //
      // Sup<S,      T     >
      //    <String, Object>

      // String, Object
      final TypeMirror lhsTypeArgument = lhsTypeArguments.get(i);
      switch (lhsTypeArgument.getKind()) {
      case ARRAY:
      case DECLARED:
      case WILDCARD:
        break;
      default:
        throw new IllegalArgumentException("typeArgumentSource: " + typeArgumentSource);
      }

      // On the right hand side:
      //
      // Sup<B, A>

      // B, A
      final TypeMirror rhsTypeArgument = rhsTypeArguments.get(i);
      switch (rhsTypeArgument.getKind()) {
      case ARRAY:
      case DECLARED:
        // This argument is already taken care of. We're interested only in replacing type variables with
        // non-type-variable reference types.
        continue;
      case TYPEVAR:
        break;
      case WILDCARD:
        // (Don't have to handle wildcards because we're working with the prototypical type hierarchy on the right hand
        // side which will never have them.)
        // fall through
      default:
        // Other type kinds are not reference type kinds.
        throw new AssertionError();
      }
      final TypeVariable rhsTypeVariable = (TypeVariable)rhsTypeArgument;

      // Assert that B and A in Sup<B, A> are type variables defined by Sub<A, B>. (This is why using the prototypical
      // type was important; see above.)
      assert
        ((TypeParameterElement)rhsTypeVariable.asElement()).getGenericElement().equals(eventPrototypicalType.asElement()) :
      "rhsTypeVariable.asElement(): " + rhsTypeVariable.asElement() +
        "; ((TypeParameterElement)rhsTypeVariable.asElement()).getGenericElement(): " + ((TypeParameterElement)rhsTypeVariable.asElement()).getGenericElement() +
        "; eventPrototypicalElementType.asElement(): " + eventPrototypicalType.asElement();

      // B in Sup<B, A> := String
      // A in Sup<B, A> := Object
      m.put(rhsTypeVariable, lhsTypeArgument);

    }

    if (m.isEmpty()) {
      // No entries in the map means no substitution was performed for whatever reason, so return the event type unchanged.
      return eventType;
    }

    // Use the proper type arguments we stored in the Map; e.g. use Object for A (S) and String for B (T).

    // Use the type parameters from the left hand side type element, because that may represent a supertype. The
    // substitutions we stored line up one for one with these type parameters.
    //
    // For example, consider a supertype (left hand side) of AbstractMap<K, V>, a usage of it (AbstractMap<String,
    // Object>), and a subtype of class SpecialMap<V> extends AbstractMap<String, V>.

    final List<? extends TypeParameterElement> tpes = eventTypeElement.getTypeParameters();
    size = tpes.size();
    final List<TypeMirror> tas = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      final TypeMirror ta = m.get(tpes.get(i).asType());
      assert ta != null : "tpe: " + tpes.get(i) + "; tpe.asType(): " + tpes.get(i).asType() + "; tpe.getGenericElement(): " + tpes.get(i).getGenericElement() + "; m: " + m;
      tas.add(ta);
    }
    assert !tas.isEmpty() : "tas: " + tas;
    return this.domain().declaredType(eventTypeElement, tas.toArray(new TypeMirror[0]));
  }


  /*
   * Static methods.
   */


  private static final boolean allAreTypeVariables(final Iterable<? extends TypeMirror> i) {
    for (final TypeMirror t : i) {
      if (t.getKind() != TYPEVAR) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if and only if the supplied {@link TypeMirror} is a <dfn>legal event type</dfn>.
   *
   * <p>Legal event types are, exactly:</p>
   *
   * <ol>
   *
   * <li>{@linkplain javax.lang.model.type.TypeKind#ARRAY Array} types whose {@linkplain ArrayType#getComponentType()
   * component type}s are primitive types or legal event types</li>
   *
   * <li>{@linkplain javax.lang.model.type.TypeKind#DECLARED Declared} types that do not refer to type variables</li>
   *
   * </ol>
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if {@code t} is a legal event type; {@code false} otherwise
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @microbean.idempotency This method is idempotent and deterministic.
   *
   * @microbean.threadsafety This method itself is safe for concurrent use by multiple threads, but {@link TypeMirror}
   * implementations and {@link Domain} implementations may not be safe for such use.
   */
  public static final boolean legalEventType(final TypeMirror t) {
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#event_types_and_qualifier_types
    return switch (t.getKind()) {

    case ARRAY -> {
      final TypeMirror componentType = ((ArrayType)t).getComponentType();
      if (!componentType.getKind().isPrimitive() && !legalEventType(componentType)) {
        if (LOGGER.isLoggable(WARNING)) {
          LOGGER.log(WARNING, t + " has a component type that is an illegal event type (" + componentType);
        }
        yield false;
      }
      yield true;
    }

    // As of this writing, you can't fire a primitive event in CDI, because the fire() method takes an Object, but
    // there's nothing stopping a primitive event type from being legal otherwise (you can envision a fire(int) method,
    // for example). Consequently, this may end up being uncommented later.
    //
    // case BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT -> true;

    case DECLARED -> {
      // "An event type may not contain an unresolvable [sic] type variable. A wildcard type is not considered an
      // unresolvable type variable."
      //
      // We interpret "contain" to mean "have as a type argument, or as a wildcard bound, recursively, anywhere".
      for (final TypeMirror ta : ((DeclaredType)t).getTypeArguments()) {
        switch (ta.getKind()) {
        case WILDCARD:
          final WildcardType wta = (WildcardType)ta;
          TypeMirror b = wta.getExtendsBound();
          if (b != null && !legalEventType(b)) {
            if (LOGGER.isLoggable(WARNING)) {
              LOGGER.log(WARNING, t + " has a type argument that is an illegal event type (" + ta + ")");
            }
            yield false;
          }
          b = wta.getSuperBound();
          if (b != null && !legalEventType(b)) {
            if (LOGGER.isLoggable(WARNING)) {
              LOGGER.log(WARNING, t + " has a type argument that is an illegal event type (" + ta + ")");
            }
            yield false;
          }
          break;
        default:
          if (!legalEventType(ta)) {
            if (LOGGER.isLoggable(WARNING)) {
              LOGGER.log(WARNING, t + " has a type argument that is an illegal event type (" + ta + ")");
            }
            yield false;
          }
          break;
        }
      }
      yield true;
    }

    default -> {
      // (Including, of course, TYPEVAR.)
      if (LOGGER.isLoggable(WARNING)) {
        LOGGER.log(WARNING, t + " is an illegal event type");
      }
      yield false;
    }
    };
  }

  /**
   * Returns {@code true} if and only if the supplied {@link TypeMirror} is a <dfn>legal observed event type</dfn>.
   *
   * <p>A legal observed event type is <a
   * href="https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#event_types_and_qualifier_types">any Java type
   * that a method parameter may bear</a>.
   *
   * @param t a {@link TypeMirror}; must not be {@code null}
   *
   * @return {@code true} if and only if {@code t} is a legal observed event type; {@code false} otherwise
   *
   * @exception NullPointerException if {@code t} is {@code null}
   *
   * @microbean.idempotency This method is idempotent and deterministic.
   *
   * @microbean.threadsafety This method itself is safe for concurrent use by multiple threads, but {@link TypeMirror}
   * implementations and {@link Domain} implementations may not be safe for such use.
   */
  public static final boolean legalObservedEventType(final TypeMirror t) {
    // https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#event_types_and_qualifier_types
    // "Any Java type [that a method parameter element may bear] may be an observed event type."
    return switch (t.getKind()) {
    case ARRAY, BOOLEAN, BYTE, CHAR, DECLARED, DOUBLE, FLOAT, INT, LONG, SHORT, TYPEVAR -> true;
    default -> false;
    };
  }

  private static final DeclaredType congruentSupertype(final TypeElement lhsTypeElement, DeclaredType subtype) {
    if (subtype.getKind() != DECLARED) {
      throw new IllegalArgumentException("subtype: " + subtype);
    }
    if (lhsTypeElement == null) {
      return subtype;
    }
    // Make sure the subtype is the prototypical subtype. Very important.
    TypeElement subtypeTypeElement = (TypeElement)subtype.asElement();
    if (subtype.getTypeArguments().isEmpty()) {
      subtype = (DeclaredType)subtypeTypeElement.asType();
    }
    TypeElement rhsTypeElement = subtypeTypeElement;
    DeclaredType rhs = subtype;
    while (rhsTypeElement != null && !lhsTypeElement.equals(rhsTypeElement)) {
      final TypeMirror rhsSuperclass = rhsTypeElement.getSuperclass();
      switch (rhsSuperclass.getKind()) {
      case NONE:
        rhs = null;
        rhsTypeElement = null; // no superclass means it was Object
        break;
      case DECLARED:
        rhs = (DeclaredType)rhsSuperclass; // extremely important
        rhsTypeElement = (TypeElement)rhs.asElement();
        break;
      }
    }
    if (rhsTypeElement == null) {
      final Set<TypeElement> seen = new HashSet<>();
      final List<TypeMirror> interfaces = new LinkedList<>();
      for (final TypeMirror iface : subtypeTypeElement.getInterfaces()) {
        if (seen.add((TypeElement)((DeclaredType)iface).asElement())) {
          interfaces.add(iface);
        }
      }
      INTERFACES_LOOP:
      while (!interfaces.isEmpty()) {
        final DeclaredType iface = (DeclaredType)interfaces.removeFirst();
        final TypeElement ifaceTypeElement = (TypeElement)iface.asElement();
        if (lhsTypeElement.equals(ifaceTypeElement)) {
          // we're done
          rhs = iface; // extremely important
          break INTERFACES_LOOP;
        }
        for (final TypeMirror superinterface : ifaceTypeElement.getInterfaces()) {
          if (seen.add((TypeElement)((DeclaredType)superinterface).asElement())) {
            interfaces.add(superinterface);
          }
        }
      }
    }
    return rhs;
  }

  /*
  public static final class EventTypeList extends AbstractList<TypeMirror> {

    private static final EventTypeList EMPTY_LIST = new EventTypeList(SupertypeList.of());

    private final SupertypeList types;

    private EventTypeList(final SupertypeList types) {
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
  */

}
