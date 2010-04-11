/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2005-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$

package scala.actors

/**
 * The <code>OutputChannel</code> trait provides a common interface
 * for all channels to which values can be sent.
 *
 * @author Philipp Haller
 *
 * @define actor `OutputChannel`
 */
trait OutputChannel[-Msg] extends AbstractReactor[Msg] {

  /**
   * Sends <code>msg</code> to this $actor (asynchronous).
   */
  def !(msg: Msg): Unit

  /**
   * Sends <code>msg</code> to this $actor (asynchronous) supplying
   * explicit reply destination.
   *
   * @param  msg      the message to send
   * @param  replyTo  the reply destination
   */
  def send(msg: Msg, replyTo: OutputChannel[Any]): Unit

  /**
   * Forwards <code>msg</code> to this $actor (asynchronous).
   */
  def forward(msg: Msg): Unit

  /**
   * Returns the <code>Actor</code> that is receiving from this $actor.
   */
  def receiver: Actor
}
