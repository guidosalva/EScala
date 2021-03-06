package scala.tools.nsc
package events

import transform._
import ast._
import symtab._
import Flags._
import util.EventUtil
import typechecker._

import scala.collection.mutable.{HashMap,MultiMap,Set}

/**
 * This phase marks the method that will be instrumented. It does not do the instrumentation yet.
 *
 * @author Lucas Satabin
 */
abstract class InstrumentMarker extends Transform
                       with TypingTransformers
                       with EventUtil {

  import global._
  import definitions._

  protected[events] var namer: analyzer.Namer = null

  class InstrumentMarkerTransformer(unit: CompilationUnit, methodsInClass: MultiMap[Symbol, Symbol]) extends TypingTransformer(unit) {

    var namer = analyzer.newNamer(analyzer.rootContext(unit))

    val allMethods = methodsInClass.values.flatten

    var clazz: Symbol = NoSymbol

    override def transform(tree: Tree) = {
      tree match {
        case cd @ ClassDef(mods, name, tparams, template @ Template(parents, self, body)) if methodsInClass.contains(cd.symbol) =>
          // set the instrument flag and override the method if necessary
          clazz = cd.symbol
          namer = analyzer.newNamer(namer.context.make(tree, clazz, clazz.info.decls))

          val methods = methodsInClass(cd.symbol)
          // the synthesized overridden methods if any
          var synthesized: List[DefDef] = Nil
          methods.foreach(markMethod(_, clazz) match {
              case Some(m) => synthesized = m :: synthesized
              case None => /* do nothing */
            })
          // copy the method with the new overridden methods
          treeCopy.ClassDef(cd, mods, name, tparams, treeCopy.Template(template, parents, self, super.transformTrees(body) ::: synthesized))
        case dd: DefDef if !dd.symbol.isConstructor && !dd.symbol.hasFlag(INSTRUMENTED)
                           && (allMethods.exists(anySuperSymbol(dd.symbol, dd.symbol.owner) ==) ||
                              (isSuperInstrumented(dd.symbol, dd.symbol.owner)) && !isSuperObservable(dd.symbol, dd.symbol.owner)) =>
          // if the method overrides an isntrumented method (but not observable) set the instrumented flag too
          // if the overridden method was declared as `observable' the type checker should already have checked it
          if(settings.Yeventsdebug.value)
            println(dd + " overrides instrumented method")
          dd.symbol.setFlag(INSTRUMENTED)
          super.transform(tree)
        case _ => super.transform(tree)
      }
    }

    /** Mark the method as instrumented.
     *  @returns Some method if the method was overridden, None otherwise
     */
    def markMethod(meth: Symbol, clazz: Symbol): Option[DefDef] = {
      if(meth.owner == clazz) {
        // just set the flag
        meth setFlag INSTRUMENTED
        None
      } else
        // override the method
        Some(generateMethod(meth, clazz))
    }

    /** Generates a new method overriding the given one and mark it as instrumented.
     */
    private def generateMethod(meth: Symbol, clazz: Symbol): DefDef = {
      if(settings.Yeventsdebug.value)
        println("overriding " + meth + " declared in " + meth.owner + " in " + clazz)
      // override the method defined in a parent class, marking it with the INSTRUMENTED flag
      // it simply calls the super method, instrumentation is done later

      val newSym = meth.cloneSymbol(clazz)        
      if (meth.isDeferred)
        newSym.setFlag(DEFERRED | INSTRUMENTED)
      else
        newSym.setFlag(OVERRIDE | INSTRUMENTED)
      namer.enterInScope(newSym)

      if (meth.isDeferred)
        DefDef(newSym, EmptyTree)
      else
        DefDef(newSym, genSuperCall(meth) _)
    }

    private def genSuperCall(meth: Symbol)(paramss: List[List[Symbol]]) = {
      val superMeth = Select(Super(tpnme.EMPTY, tpnme.EMPTY), meth)
      paramss.foldLeft[Tree](superMeth)((base, args) => Apply(base, args.map(s => Ident(s))))
    }

  }

}

// vim: set ts=4 sw=4 et:
