/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: Sequence.scala 16092 2008-09-12 10:37:06Z nielsen $


package scala.collection.generic

import TraversibleView.NoBuilder

/** A non-strict projection of an iterable. 
 * @author Sean McDirmid
 * @author Martin Odersky
 * @version 2.8
 */
trait VectorView[+A, +Coll <: Vector[_]] extends VectorViewTemplate[A, Coll, VectorView[A, Coll]]

object VectorView {
  type Coll = TraversibleView[_, _]
  implicit def builderFactory[A]: BuilderFactory[A, VectorView[A, Vector[_]], Coll] = new BuilderFactory[A, VectorView[A, Vector[_]], Coll] { def apply(from: Coll) = new NoBuilder }
}
