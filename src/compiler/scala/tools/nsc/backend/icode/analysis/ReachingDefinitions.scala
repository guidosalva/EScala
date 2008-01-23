/* NSC -- new Scala compiler
 * Copyright 2005-2008 LAMP/EPFL
 * @author  Martin Odersky
 */

// $Id$

package scala.tools.nsc.backend.icode.analysis

import scala.collection.immutable.{Set, ListSet, HashSet}
import scala.collection.jcl.{HashMap, Map}

/** Compute reaching definitions. We are only interested in reaching
 *  definitions for local variables, since values on the stack
 *  behave as-if in SSA form: the closest instruction which produces a value
 *  on the stack is a reaching definition.
 */
abstract class ReachingDefinitions {
  val global: Global
  import global._
  import icodes._

  /** The lattice for reaching definitions. Elements are
   *  a triple (local variable, basic block, index of instruction of that basic block)
   */
  object rdefLattice extends CompleteLattice {
    type Definition = (Local, BasicBlock, Int)
    type Elem = IState[Set[Definition], Stack]
    type StackPos = Set[(BasicBlock, Int)]
    type Stack = List[StackPos]

    val top: Elem = IState(new ListSet[Definition](), Nil)

    val bottom: Elem = IState(new ListSet[Definition]() {
      override def equals(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]
      override def toString = "<bottom>"
    }, Nil)

    /** The least upper bound is set inclusion for locals, and pairwise set inclusion for stacks. */
    def lub2(a: Elem, b: Elem): Elem = 
      if (bottom == a) b
      else if (bottom == b) a
      else {
        val locals = a.vars ++ b.vars
        val stack = if (a.stack == Nil) 
          b.stack
        else if (b.stack == Nil) a.stack 
        else List.map2(a.stack, b.stack) (_ ++ _)
        
        val res = IState(locals, stack)
      
//        Console.println("\tlub2: " + a + ", " + b)
//        Console.println("\tis: " + res)
      
//        if (res._1 eq bottom._1) (new ListSet[Definition], Nil)
//        else res
        res
      }
  }

  class ReachingDefinitionsAnalysis extends DataFlowAnalysis[rdefLattice.type] {
    type P = BasicBlock
    val lattice = rdefLattice
    import lattice.Definition
    import lattice.Stack
    import lattice.Elem

    var method: IMethod = _

    val gen: Map[BasicBlock, Set[Definition]] = new HashMap()
    val kill:Map[BasicBlock, Set[Local]]      = new HashMap()
    val drops: Map[BasicBlock, Int]           = new HashMap()
    val outStack: Map[BasicBlock, Stack]      = new HashMap()

    def init(m: IMethod) {
      this.method = m
      gen.clear;   kill.clear
      drops.clear; outStack.clear

      for (b <- m.code.blocks.toList;
           (g, k) = genAndKill(b);
           (d, st) = dropsAndGen(b)) {
        gen  += (b -> g)
        kill += (b -> k)
        drops += (b -> d)
        outStack += (b -> st)
      }

      init {
        worklist ++= m.code.blocks.toList
        m.code.blocks.foreach { b =>
          in(b)  = lattice.bottom
          out(b) = lattice.bottom
        } 
        m.exh foreach { e =>
          in(e.startBlock) = lattice.IState(new ListSet[Definition], List(new ListSet[(BasicBlock, Int)]))
        }
        
      }
    }
    
    import opcodes._
    
    def genAndKill(b: BasicBlock): (Set[Definition], Set[Local]) = {
      var genSet: Set[Definition] = new HashSet
      var killSet: Set[Local] = new HashSet
      for (val (i, idx) <- b.toList.zipWithIndex) i match {
        case STORE_LOCAL(local) => 
          killSet = killSet + local
          genSet  = updateReachingDefinition(b, idx, genSet)
        case _ => ()
      }
      (genSet, killSet)
    }
    
    private def dropsAndGen(b: BasicBlock): (Int, List[Set[(BasicBlock, Int)]]) = {
      var depth = 0
      var drops = 0
      var stackOut: List[Set[(BasicBlock, Int)]] = Nil
      
      for (val (instr, idx) <- b.toList.zipWithIndex) {
        if (instr == LOAD_EXCEPTION()) 
          ()
        else if (instr.consumed > depth) {
          drops = drops + (instr.consumed - depth)
          depth = 0
          stackOut = Nil
        } else {
          stackOut = stackOut.drop(instr.consumed)
          depth = depth - instr.consumed
        }
        var prod = instr.produced
        depth = depth + prod
        while (prod > 0) {
          stackOut = (new collection.immutable.Set1((b, idx))) :: stackOut
          prod = prod - 1
        }
      }
//      Console.println("drops(" + b + ") = " + drops)
//      Console.println("stackout(" + b + ") = " + stackOut)
      (drops, stackOut)
    }
    
    override def run {
      forwardAnalysis(blockTransfer)
      if (settings.debug.value) {
        linearizer.linearize(method).foreach(b => if (b != method.code.startBlock)
          assert(lattice.bottom != in(b),
            "Block " + b + " in " + this.method + " has input equal to bottom -- not visited? " + in(b)
                 + ": bot: " + lattice.bottom
                 + "\nin(b) == bottom: " + (in(b) == lattice.bottom)
                 + "\nbottom == in(b): " + (lattice.bottom == in(b))));
      }
    }

    import opcodes._
    import lattice.IState
    def updateReachingDefinition(b: BasicBlock, idx: Int, rd: Set[Definition]): Set[Definition] = {
      val STORE_LOCAL(local) = b(idx)
      var tmp = local
      (rd filter { case (l, _, _) => l != tmp }) + ((tmp, b, idx))
    }
    
    private def blockTransfer(b: BasicBlock, in: lattice.Elem): lattice.Elem = {
      var locals: Set[Definition] = (in.vars filter { case (l, _, _) => !kill(b)(l) }) ++ gen(b)
      if (locals eq lattice.bottom.vars) locals = new ListSet[Definition]
      IState(locals, outStack(b) ::: in.stack.drop(drops(b)))
    }
    
    /** Return the reaching definitions corresponding to the point after idx. */
    def interpret(b: BasicBlock, idx: Int, in: lattice.Elem): Elem = {
      var locals = in.vars
      var stack  = in.stack
      val instr = b(idx)
      instr match {
        case STORE_LOCAL(l1) =>
          locals = updateReachingDefinition(b, idx, locals)
          stack = stack.drop(instr.consumed)
        case LOAD_EXCEPTION() =>
          stack = Nil
        case _ =>
          stack = stack.drop(instr.consumed)
      }

      var prod = instr.produced
      while (prod > 0) {
        stack = (new collection.immutable.Set1((b, idx))) :: stack
        prod -= 1
      }

      IState(locals, stack)
    }

    /** Return the instructions that produced the 'm' elements on the stack, below given 'depth'.
     *  for instance, findefs(bb, idx, 1, 1) returns the instructions that might have produced the
     *  value found below the topmost element of the stack. 
     */
    def findDefs(bb: BasicBlock, idx: Int, m: Int, depth: Int): List[(BasicBlock, Int)] = if (idx > 0) {
      assert(bb.isClosed)
      var instrs = bb.getArray
      var res: List[(BasicBlock, Int)] = Nil
      var i = idx
      var n = m
      var d = depth
      // "I look for who produced the 'n' elements below the 'd' topmost slots of the stack"
      while (n > 0 && i > 0) {
        i -= 1
        val prod = instrs(i).produced
        if (prod > d) {
          res = (bb, i) :: res
          n   = n - (prod - d)
          if (bb(i) != LOAD_EXCEPTION)
            d = instrs(i).consumed
        } else {
          d -= prod
          d += instrs(i).consumed
        }
      }
      
      if (n > 0) { 
        val stack = this.in(bb).stack
        assert(stack.length >= n, "entry stack is too small, expected: " + n + " found: " + stack)
        stack.drop(d).take(n) foreach { defs =>
          res = defs.toList ::: res
        }
      }
      res
    } else {
      val stack = this.in(bb).stack
      assert(stack.length >= m, "entry stack is too small, expected: " + m + " found: " + stack)
      stack.take(m) flatMap (_.toList)
    }

    /** Return the definitions that produced the topmost 'm' elements on the stack,
     *  and that reach the instruction at index 'idx' in basic block 'bb'.
     */
    def findDefs(bb: BasicBlock, idx: Int, m: Int): List[(BasicBlock, Int)] = 
      findDefs(bb, idx, m, 0)

    override def toString: String = {
      val sb = new StringBuilder
      sb.append("rdef: \n")
      for (b <- method.code.blocks)
        sb.append("rdef_entry(" + b + ")= " + in(b)).append("\nrdef_exit(" + b + ")= " + out(b))
      sb.toString()
    }

  }
}
