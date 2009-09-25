/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |                                         **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$


package scala.collection
package mutable

/** <code>Queue</code> objects implement data structures that allow to
 *  insert and retrieve elements in a first-in-first-out (FIFO) manner.
 *
 *  @author  Matthias Zenger
 *  @version 1.1, 03/05/2004
 */
trait QueueProxy[A] extends Queue[A] with Proxy {

  def self: Queue[A]

  /** Access element number <code>n</code>.
   *
   *  @return  the element at index <code>n</code>.
   */
  override def apply(n: Int): A = self.apply(n)

  /** Returns the length of this queue.
   */
  override def length: Int = self.length

  /** Checks if the queue is empty.
   *
   *  @return true, iff there is no element in the queue.
   */
  override def isEmpty: Boolean = self.isEmpty

  /** Inserts a single element at the end of the queue.
   *
   *  @param  elem        the element to insert
   */
  override def +=(elem: A): this.type = { self += elem; this }

  /** Adds all elements provided by an <code>Iterable</code> object
   *  at the end of the queue. The elements are prepended in the order they
   *  are given out by the iterator.
   *
   *  @param  iter        an iterable object
   */
  def ++=(iter: scala.collection.Iterable[A]): this.type = {
    self ++= iter
    this
  }

  /** Adds all elements provided by an iterator
   *  at the end of the queue. The elements are prepended in the order they
   *  are given out by the iterator.
   *
   *  @param  iter        an iterator
   */
  override def ++=(it: Iterator[A]): this.type = {
    self ++= it
    this
  }

  /** Adds all elements to the queue.
   *
   *  @param  elems       the elements to add.
   */
  override def enqueue(elems: A*): Unit = self ++= elems

  /** Returns the first element in the queue, and removes this element
   *  from the queue.
   *
   *  @return the first element of the queue.
   */
  override def dequeue(): A = self.dequeue

  /** Returns the first element in the queue, or throws an error if there
   *  is no element contained in the queue.
   *
   *  @return the first element.
   */
  override def front: A = self.front

  /** Removes all elements from the queue. After this operation is completed,
   *  the queue will be empty.
   */
  override def clear(): Unit = self.clear

  /** Returns an iterator over all elements on the queue.
   *
   *  @return an iterator over all queue elements.
   */
  override def iterator: Iterator[A] = self.iterator

  /** This method clones the queue.
   *
   *  @return  a queue with the same elements.
   */
  override def clone(): Queue[A] = new QueueProxy[A] {
    def self = QueueProxy.this.self.clone()
  }
}
