/* NSC -- new Scala compiler
 * Copyright 2005-2007 LAMP/EPFL
 * @author Martin Odersky
 */
// $Id$

package scala.tools.nsc.ast.parser

/** An nsc sub-component.
 */
abstract class SyntaxAnalyzer extends SubComponent with Parsers with MarkupParsers with Scanners {

  val phaseName = "parser"

  def newPhase(prev: Phase): StdPhase = new ParserPhase(prev)

  class ParserPhase(prev: scala.tools.nsc.Phase) extends StdPhase(prev) {
    def apply(unit: global.CompilationUnit) {
      global.informProgress("parsing " + unit)
      unit.body = new UnitParser(unit).parse()
    }
  }
}

