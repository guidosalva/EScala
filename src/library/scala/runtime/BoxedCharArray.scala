/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$


package scala.runtime

@serializable
final class BoxedCharArray(val value: Array[Char]) extends BoxedArray[Char] {

  def length: Int = value.length

  def apply(index: Int): Char = value(index)

  def update(index: Int, elem: Char) {
    value(index) = elem
  }
  def unbox(elemClass: Class[_]): AnyRef = value
}
