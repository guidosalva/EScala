package scala.tools.nsc
package events

import transform._
import symtab._
import Flags._
import util.EventUtil
import typechecker._

/**
 * This class allows to intrument the observable class.
 *
 */
abstract class ObservableClass extends Transform 
                                         with EventUtil
                                         with TypingTransformers
                                         {

  import global._
  import definitions._

  val phaseName: String = "observableclass"

  def newTransformer(unit: CompilationUnit): Transformer = {
    new ObservablesClassTrans(unit)
  }

  /** Create a new phase which applies transformer */
  override def newPhase(prev: scala.tools.nsc.Phase): StdPhase = new Phase(prev)

  /** The phase defined by this transform */
  class Phase(prev: scala.tools.nsc.Phase) extends StdPhase(prev) {
    def apply(unit: global.CompilationUnit): Unit = {
      newTransformer(unit) transformUnit unit
    }
  }

  class ObservablesClassTrans(unit: CompilationUnit) extends TypingTransformer(unit) { 
        
    import symtab.Flags._
    
    override def transform(tree: Tree): Tree = {
      val sym = tree.symbol
      tree match {
          case cd @ ClassDef(mods, name, tparams, impl)
                if sym.isInstrumented =>
            if (settings.Yeventsdebug.value) {
                println("Transform of observable class called for :  " + name)
            }

            val modifiers = (cd.mods & ~OBSERVABLE & ~DEFERRED & ~INSTRUMENTED) | FINAL

            val allObjectName = name + "$all"
            val allObject = genAllObject(cd, modifiers,
            genAllObjectTpt(/* TODO */ tupledGenericParam), /*TODO*/ body ,pos)

            name.enterSyntheticSym(allObject)
            allObject = localTyper.typed(allObject).asInstanceOf[ValDef]
            //... TODO

            tree
          case _ => super.transform(tree)
       }
    }

  }
}
// vim: set ts=4 sw=4 et:
