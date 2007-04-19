/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2006-2007, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$

package scala.collection.jcl

/** Analogous to a Java set.
 *
 *  @author Sean McDirmid
 */
trait Set[A] extends Collection[A] with scala.collection.mutable.Set[A] {

  /** Add will return false if "a" already exists in the set. **/
  override def add(a: A): Boolean

  override def ++(i: Iterable[A]) : this.type = super[Collection].++(i)
  override def --(i: Iterable[A]) : this.type = super[Collection].--(i)
  override def +(t: A) : this.type = super[Collection].+(t)
  override def -(t: A) : this.type = super[Collection].-(t)
  override def retain(f: A => Boolean) = super[Collection].retain(f)
  override def isEmpty = super[Collection].isEmpty
  override final def contains(a: A) = has(a)
  override def clear() = super.clear()
  override def subsetOf(set : scala.collection.Set[A]) = set match {
    case set : Set[_] => set.hasAll(this)
    case set => super.subsetOf(set)
  }

  override def transform(f: A => A) = {
    var toAdd : List[A] = Nil
    val i = elements
    while (i.hasNext) {
      val i0 = i.next
      val i1 = f(i0)
      if (i0 != i1) {
        i.remove; toAdd = i1 :: toAdd
      }
    }
    addAll(toAdd)
  }
  trait Projection extends super.Projection {
    override def filter(p : A => Boolean) : Set[A] = new Filter(p);
  }
  override def projection : Projection = new Projection {}
  class Filter(p : A => Boolean) extends super.Filter(p) with Set[A] {
    override def retain(p0 : A => Boolean): Unit = 
      Set.this.retain(e => !p(e) || p0(e))
    trait Projection extends super[Filter].Projection with super[Set].Projection {}
    override def projection : Projection = new Projection {}
  }
}
