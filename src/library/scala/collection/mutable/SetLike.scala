/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$


package scala.collection
package mutable

import generic._
import script._

/** A template trait for mutable sets of type `mutable.Set[A]`.
 *  @tparam A    the type of the elements of the set
 *  @tparam This the type of the set itself.
 *
 *  $setnote
 * 
 *  @author  Martin Odersky
 *  @version 2.8
 *  @since 2.8
 *
 *  @define setnote @note
 *    This trait provides most of the operations of a `mutable.Set` independently of its representation.
 *    It is typically inherited by concrete implementations of sets.
 * 
 *    To implement a concrete mutable set, you need to provide implementations
 *    of the following methods:
 *    {{{
 *       def contains(elem: A): Boolean
 *       def iterator: Iterator[A]
 *       def += (elem: A): this.type
 *       def -= (elem: A): this.type</pre>
 *    }}}
 *    If you wish that methods like `take`,
 *    `drop`, `filter` return the same kind of set,
 *    you should also override:
 *    {{{
 *       def empty: This</pre>
 *    }}}
 *    It is also good idea to override methods `foreach` and
 *    `size` for efficiency.
 *  @define coll mutable set
 *  @define Coll mutable.Set
 */
trait SetLike[A, +This <: SetLike[A, This] with Set[A]] 
  extends scala.collection.SetLike[A, This]
     with Scriptable[A]
     with Builder[A, This]
     with Growable[A]
     with Shrinkable[A] 
     with Cloneable[mutable.Set[A]] 
{ self =>
  
  /** A common implementation of `newBuilder` for all mutable sets
   *  in terms of `empty`. Overrides the implementation in `collection.SetLike`
   *  for better efficiency.
   */
  override protected[this] def newBuilder: Builder[A, This] = empty

  /** Adds an element to this $coll.
   *
   *  @param elem the element to be added
   *  @return `true` if the element was not yet present in the set, `false` otherwise.
   */
  def add(elem: A): Boolean = {
    val r = contains(elem)
    this += elem
    r
  }

  /** Removes an element from this set.
   *
   *  @param elem  The element to be removed.
   *  @return  `true` if the element was previously present in the set, `false` otherwise.
   */
  def remove(elem: A): Boolean = {
    val r = contains(elem)
    this -= elem
    r
  }

  /** Updates the presence of a single element in this set.
   *
   * This method allows one to add or remove an element `elem`
   *  from this set depending on the value of parameter `included`.
   *  Typically, one would use the following syntax:
   *  {{{
   *     set(elem) = true  // adds element
   *     set(elem) = false // removes element
   *  }}}
   *
   *  @param elem     the element to be added or removed
   *  @param included a flag indicating whether element should be included or excluded.
   */
  def update(elem: A, included: Boolean) {
    if (included) this += elem else this -= elem
  }

  // abstract methods from Growable/Shrinkable
  
  def +=(elem: A): this.type
  def -=(elem: A): this.type

  /** Removes all elements from the set for which do not satisfy a predicate.
   *  @param  p  the predicate used to test elements. Only elements for
   *             while `p` returns `true` are retained in the set; all others
   *             are removed.
   */
  def retain(p: A => Boolean): Unit = for (elem <- this.toList) if (!p(elem)) this -= elem

  /** Removes all elements from the set. After this operation is completed,
   *  the set will be empty.
   */
  def clear() { foreach(-=) }

  override def clone(): mutable.Set[A] = empty ++= repr

  /** The result when this set is used as a builder
   *  @return  the set representation itself.
   */
  def result: This = repr

  /** Adds a single element to this collection and returns 
   *  the collection itself.
   *
   *  @param elem  the element to add.
   */
  @deprecated("Use += instead if you intend to add by side effect to an existing collection.\n"+
              "Use `clone() +=' if you intend to create a new collection.")
 override def + (elem: A): This = { +=(elem); repr }

  /** Adds two or more elements to this collection and returns
   *  the collection itself.
   *
   *  @param elem1 the first element to add.
   *  @param elem2 the second element to add.
   *  @param elems the remaining elements to add.
   */
  @deprecated("Use += instead if you intend to add by side effect to an existing collection.\n"+
              "Use `clone() +=' if you intend to create a new collection.")
  override def + (elem1: A, elem2: A, elems: A*): This = {
    this += elem1 += elem2 ++= elems
    repr
  }

  /** Adds a number of elements provided by a traversable object and returns
   *  either the collection itself.
   *
   *  @param iter     the iterable object.
   */
  @deprecated("Use ++= instead if you intend to add by side effect to an existing collection.\n"+
              "Use `clone() ++=' if you intend to create a new collection.")
  override def ++(iter: scala.collection.Traversable[A]): This = { 
    for (elem <- iter) +=(elem)
    repr
  }

  /** Adds a number of elements provided by an iterator and returns
   *  the collection itself.
   *
   *  @param iter   the iterator
   */
  @deprecated("Use ++= instead if you intend to add by side effect to an existing collection.\n"+
              "Use `clone() ++=' if you intend to create a new collection.")
  override def ++ (iter: Iterator[A]): This = {
    for (elem <- iter) +=(elem)
    repr
  }

  /** Removes a single element from this collection and returns 
   *  the collection itself.
   *
   *  @param elem  the element to remove.
   */
  @deprecated("Use -= instead if you intend to remove by side effect from an existing collection.\n"+
              "Use `clone() -=' if you intend to create a new collection.")
  override def -(elem: A): This = { -=(elem); repr }

  /** Removes two or more elements from this collection and returns
   *  the collection itself.
   *
   *  @param elem1 the first element to remove.
   *  @param elem2 the second element to remove.
   *  @param elems the remaining elements to remove.
   */
  @deprecated("Use -= instead if you intend to remove by side effect from an existing collection.\n"+
              "Use `clone() -=' if you intend to create a new collection.")
  override def -(elem1: A, elem2: A, elems: A*): This = {
    this -= elem1 -= elem2 --= elems
    repr
  }

  /** Removes a number of elements provided by a Traversable object and returns
   *  the collection itself.
   *
   *  @param iter     the Traversable object.
   */
  @deprecated("Use --= instead if you intend to remove by side effect from an existing collection.\n"+
              "Use `clone() --=' if you intend to create a new collection.")
  override def --(iter: scala.collection.Traversable[A]): This = { 
    for (elem <- iter) -=(elem)
    repr
  }

  /** Removes a number of elements provided by an iterator and returns
   *  the collection itself.
   *
   *  @param iter   the iterator
   */
  @deprecated("Use --= instead if you intend to remove by side effect from an existing collection.\n"+
              "Use `clone() --=' if you intend to create a new collection.")
  override def --(iter: Iterator[A]): This = { 
    for (elem <- iter) -=(elem)
    repr
  }

  /** Send a message to this scriptable object.
   *
   *  @param cmd  the message to send.
   *  @throws `Predef.UnsupportedOperationException`
   *  if the message was not understood.
   */
   def <<(cmd: Message[A]): Unit = cmd match {
     case Include(_, x)     => this += x
     case Remove(_, x)      => this -= x
     case Reset()           => clear
     case s: Script[_]      => s.iterator foreach <<
     case _                 => throw new UnsupportedOperationException("message " + cmd + " not understood")
   }
}
