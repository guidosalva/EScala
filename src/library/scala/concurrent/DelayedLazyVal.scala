/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |                                         **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$

package scala.concurrent

import annotation.experimental

/** A <code>DelayedLazyVal</code> is a wrapper for lengthy
 *  computations which have a valid partially computed result.
 *  The first argument is a function for obtaining the result
 *  at any given point in time, and the second is the lengthy
 *  computation.  Once the computation is complete, the apply()
 *  method will stop recalculating it and return a fixed value
 *  from that point forward.
 *
 *  @param  f      the function to obtain the current value at any point in time
 *  @param  body   the computation to run to completion in another thread
 *
 *  @author  Paul Phillips
 *  @version 2.8
 */
@experimental
class DelayedLazyVal[T](f: () => T, body: => Unit) {
  @volatile private[this] var isDone = false
  private[this] lazy val complete = f()
  
  /** The current result of f(), or the final result if complete.
   *
   *  @return the current value
   */
  def apply(): T = if (isDone) complete else f()
  
  ops.future {
    body
    isDone = true
  }
}