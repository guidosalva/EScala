/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2006, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |                                         **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$


package scala.xml

/**
 * This class acts as a Buffer for nodes. If it is used as a sequence
 * of nodes <code>Seq[Node]</code>, it must be ensured that no updates
 * occur after that point, because <code>scala.xml.Node</code> is assumed
 * to be immutable.
 *
 * Despite this being a sequence, don't use it as key in a hashtable.
 * Calling the hashcode function will result in a runtime error.
 *
 * @author  Burak Emir
 * @version 1.0
 */
class NodeBuffer extends scala.collection.mutable.ArrayBuffer[Node] {

  /**
   * Append given object to this buffer, returns reference on this NodeBuffer 
   * for convenience. Some rules apply: If o is null, it is ignored. If it is 
   * an Iterator or Iterable, its elements will be added. If o is a node, it is
   * added as it is. If it is anything else, it gets wrapped in an Atom.
   *
   *
   * @param o converts to an xml node and adds to this node buffer
   * @return  this nodebuffer
   */
  def &+(o: Any): NodeBuffer = {
    o match { 
      case null | _:Unit | Text("")=>
	// ignore

      case it:Iterator[_] =>
        while(it.hasNext) 
          this &+ it.next

      case n:Node =>
        super.+(n)

      case ns:Iterable[_] =>
        this &+ ns.elements

      case d =>
        super.+(new Atom(d))
    }
    this 
  }

}
