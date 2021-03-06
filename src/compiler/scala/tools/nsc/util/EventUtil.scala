package scala.tools.nsc
package util

import transform._

/**
 * This object contains some utility methods for the events
 */
trait EventUtil {
  self: Transform =>
  
  import global._

  def buildBeforeEventName(meth: Symbol) =
    internalBuild(meth, "$before")

  def buildAfterEventName(meth: Symbol) =
    internalBuild(meth, "$after")

  def buildExecutionEventName(meth: Symbol) =
    internalBuild(meth, "$execution")

  def buildImplMethodName(meth: Symbol) =
    internalBuild(meth, "$impl")

  private def internalBuild(meth: Symbol, suffix: String) = {
    meth.tpe match {
      case mt @ MethodType(params, retType) =>
        // build the string representing the parameters
        val paramString = mt.paramTypes.foldLeft("")(
          (prefix, pt) => prefix + "$" + pt.typeSymbol.rawname
        )
        // and the final name
        meth.name + paramString + suffix
      case _ => ""
    }
  }
    
  /*
   * Indicates whether the overridden method is observable
   * @param sym 
   *            the overriding symbol
   * @param from 
   *            seen from this class
   */
  def isSuperObservable(sym: Symbol, from: Symbol) = {
    val superSym = anySuperSymbol(sym, from)
    superSym != NoSymbol && superSym.isObservable
  }

  /*
   * Indicates whether the overridden method is instrumented
   * @param sym 
   *            the overriding symbol
   * @param from 
   *            seen from this class
   */
  def isSuperInstrumented(sym: Symbol, from: Symbol) = {
    val superSym = anySuperSymbol(sym, from)
    superSym != NoSymbol && superSym.isInstrumented
  }
  
  def anySuperSymbol(sym: Symbol, from: Symbol): Symbol = {
    val superClasses = from.info.baseClasses.dropWhile(sym.owner !=)
    var bcs = 
      if(superClasses.isEmpty)
        Nil
      else
        superClasses.tail

    var ssym: Symbol = NoSymbol
    while (!bcs.isEmpty && ssym == NoSymbol) {
      ssym = sym.matchingSymbol(bcs.head, from.thisType)
      bcs = bcs.tail
    }
    ssym
  }
  
  def buildBeforeEventName(meth: Name, parameters: List[List[ValDef]]) = {
    internalBuild(meth, parameters, "before")
  }
  
  def buildAfterEventName(meth: Name, parameters: List[List[ValDef]]) = {
    internalBuild(meth, parameters, "after")
  }

  def buildExecutionEventName(meth: Name, parameters: List[List[ValDef]]) = {
    internalBuild(meth, parameters, "execution")
  }
  
  def buildImplMethodName(meth: Name, parameters: List[List[ValDef]]) = {
    internalBuild(meth, parameters, "impl")
  }
  
  private def internalBuild(meth: Name, parameters: List[List[ValDef]], suffix: String) = {
    var result = meth
    
     parameters.flatten.foreach(vd => {
       result += "$" + vd.tpt.tpe.typeSymbol.rawname
     })
     result += "$" + suffix
     result
  }

}
