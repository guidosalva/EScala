/* NSC -- new scala compiler
 * Copyright 2005 LAMP/EPFL
 * @author  Martin Odersky
 */
// $Id$
package scala.tools.nsc.ast;

import scala.concurrent._;
import symtab.Flags._;


import java.lang.Math;
import java.util.HashMap;
import java.io.StringWriter;

import javax.swing.tree._;
import javax.swing.event.TreeModelListener;
import javax.swing._;

import java.awt.BorderLayout;
import java.awt.{List => awtList, _};
import java.awt.event._;

import scala.text._;

/**
 * Tree browsers can show the AST in a graphical and interactive
 * way, useful for debugging and understanding.
 */
abstract class TreeBrowsers {

  val global: Global;
  import global._;
  import nme.EMPTY;

  /** Pseudo tree class, so that all JTree nodes are treated uniformly */
  case class ProgramTree(units: List[UnitTree]) extends Tree {
    override def toString(): String = "Program";
  }

  /** Pseudo tree class, so that all JTree nodes are treated uniformly */
  case class UnitTree(unit: CompilationUnit) extends Tree {
    override def toString(): String = unit.toString();
  }

  def create(): SwingBrowser = new SwingBrowser();

  /**
   * Java Swing pretty printer for Scala abstract syntax trees.
   */
  class SwingBrowser {

    def browse(t: Tree): Unit = {
      val phase: Phase = globalPhase;

      val tm = new ASTTreeModel(t);

      val frame = new BrowserFrame();
      frame.setTreeModel(tm);

      val lock = new Lock();
      frame.createFrame(lock);

      // wait for the frame to be closed
      lock.acquire;
    }

    def browse(units: Iterator[CompilationUnit]): Unit = 
      browse(units.toList);

    /** print the whole program */
    def browse(units: List[CompilationUnit]): Unit = {
      val phase: Phase = globalPhase;
      var unitList: List[UnitTree] = Nil;


      for (val i <- units)
        unitList = UnitTree(i) :: unitList;
      val tm = new ASTTreeModel(ProgramTree(unitList));

      val frame = new BrowserFrame();
      frame.setTreeModel(tm);

      val lock = new Lock();
      frame.createFrame(lock);

      // wait for the frame to be closed
      lock.acquire;
    }
  }

  /** Tree model for abstract syntax trees */
  class ASTTreeModel(val program: Tree) extends TreeModel {
    var listeners: List[TreeModelListener] = Nil;

    /** Add a listener to this tree */
    def addTreeModelListener(l : TreeModelListener): Unit = listeners = l :: listeners;

    /** Return the index'th child of parent */
    def getChild(parent: Any, index: Int): AnyRef = {
      packChildren(parent).drop(index).head;
    }

    /** Return the number of children this 'parent' has */
    def getChildCount(parent: Any): Int = 
      packChildren(parent).length;

    /** Return the index of the given child */
    def getIndexOfChild(parent: Any, child: Any): Int = 
      packChildren(parent).dropWhile(c => c != child).length;

    /** Return the root node */
    def getRoot(): AnyRef = program;
    
    /** Test whether the given node is a leaf */
    def isLeaf(node: Any): Boolean = packChildren(node).length == 0;

    def removeTreeModelListener(l: TreeModelListener): Unit = 
      listeners remove (x => x == l);

    /** we ignore this message for now */
    def valueForPathChanged(path: TreePath, newValue: Any) = ();

    /** 
     * Return a list of children for the given node.
     */
    def packChildren(t: Any): List[AnyRef] = 
        TreeInfo.children(t.asInstanceOf[Tree]);
  }


  /** 
   * A window that can host the Tree widget and provide methods for 
   * displaying information 
   */
  class BrowserFrame {
    val frame = new JFrame("Scala AST");
    val topLeftPane = new JPanel(new BorderLayout());
    val topRightPane = new JPanel(new BorderLayout());
    val bottomPane = new JPanel(new BorderLayout());
    var splitPane: JSplitPane = _;
    var treeModel: TreeModel = _;

    val textArea: JTextArea = new JTextArea(20, 50);
    val infoPanel = new TextInfoPanel();
    

    /** Create a frame that displays the AST.
     * 
     * @param lock The lock is used in order to stop the compilation thread
     * until the user is done with the tree inspection. Swing creates its
     * own threads when the frame is packed, and therefore execution 
     * would continue. However, this is not what we want, as the tree and
     * especially symbols/types would change while the window is visible.
     */
    def createFrame(lock: Lock): Unit = {
      lock.acquire; // keep the lock until the user closes the window

      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

      frame.addWindowListener(new WindowAdapter() {
        /** Release the lock, so compilation may resume after the window is closed. */
        override def windowClosed(e: WindowEvent): Unit = lock.release;
      });

      val tree = new JTree(treeModel) {
        /** Return the string for a tree node. */
        override def convertValueToText(value: Any, sel: Boolean, 
				        exp: Boolean, leaf: Boolean, 
				        row: Int, hasFocus: Boolean) = {
	    val Pair(cls, name) = TreeInfo.treeName(value.asInstanceOf[Tree]); 
            if (name != EMPTY)
	      cls + "[" + name.toString() + "]";
	    else
	      cls;
	  
        }
      }

      tree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
        def valueChanged(e: javax.swing.event.TreeSelectionEvent): Unit = {
	  textArea.setText(e.getPath().getLastPathComponent().toString());
	  infoPanel.update(e.getPath().getLastPathComponent());
        }
      });
      
      val topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, topLeftPane, topRightPane);
      topSplitPane.setResizeWeight(0.5);

      topLeftPane.add(new JScrollPane(tree), BorderLayout.CENTER);
      topRightPane.add(new JScrollPane(infoPanel), BorderLayout.CENTER);

      bottomPane.add(new JScrollPane(textArea), BorderLayout.CENTER);
      textArea.setFont(new Font("monospaced", Font.PLAIN, 14));
      textArea.setEditable(false);

      splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplitPane, bottomPane);
      frame.getContentPane().add(splitPane);
      frame.pack();
      frame.setVisible(true);
    }

    def setTreeModel(tm: TreeModel): Unit = treeModel = tm;
  }

  /**
   * Present detailed information about the selected tree node.
   */
  class TextInfoPanel extends JTextArea(30, 40) {

    setFont(new Font("monospaced", Font.PLAIN, 12));

    def update(v: AnyRef): Unit = {
      val t: Tree = v.asInstanceOf[Tree];
      val str = new StringBuffer();
      var buf = new StringWriter();

      t match {
        case ProgramTree(_) => ();
        case UnitTree(_)    => ();
        case _ =>
          str.append("Symbol: ").append(TreeInfo.symbolText(t));
          str.append("\nSymbol info: \n");
          TreeInfo.symbolTypeDoc(t).format(getWidth() / getColumnWidth(), buf);
          str.append(buf.toString());
          str.append("\nSymbol tpe: \n");
          if (t.symbol != null) {
            buf = new StringWriter();
            TypePrinter.toDocument(t.symbol.tpe).format(getWidth() / getColumnWidth(), buf);
            str.append(buf.toString());
          }
          str.append("\nSymbol Attributes: \n").append(TreeInfo.symbolAttributes(t));
          str.append("\nType: \n").append(if (t.tpe ne null) t.tpe.toString() else "");
        }
      setText(str.toString());
    }
  }


  /** Computes different information about a tree node. It
   *  is used as central place to do all pattern matching against
   *  Tree. 
   */
  object TreeInfo {

    /** Return the case class name and the Name, if the node defines one */
    def treeName(t: Tree): Pair[String, Name] = t match {
      case ProgramTree(units) =>
        Pair("Program", EMPTY);

      case UnitTree(unit) =>
        Pair("CompilationUnit", unit.toString());

      case DocDef(comment, definition) =>
        Pair("DocDef", EMPTY);

      case ClassDef(mods, name, tparams, tpt, impl) => 
        Pair("ClassDef", name);

      case PackageDef(packaged, impl) => 
        Pair("PackageDef", EMPTY);

      case ModuleDef(mods, name, impl) =>
        Pair("ModuleDef", name);

      case ValDef(mods, name, tpe, rhs) =>
        Pair("ValDef", name);

      case DefDef(mods, name, tparams, vparams, tpe, rhs) => 
        Pair("DefDef", name);

      case AbsTypeDef(mods, name, rhs, lobound) =>
        Pair("AbsTypeDef", name);

      case AliasTypeDef(mods, name, tparams, rhs) => 
        Pair("AliasTypeDef", name);

      case Import(expr, selectors) => 
        Pair("Import", EMPTY);

      case CaseDef(pat, guard, body) =>
        Pair("CaseDef", EMPTY);

      case Template(parents, body) => 
        Pair("Template", EMPTY);

      case LabelDef(name, params, rhs) =>
        Pair("LabelDef", name);

      case Block(stats, expr) =>
        Pair("Block", EMPTY);

      case Sequence(trees) =>
        Pair("Sequence", EMPTY);

      case Alternative(trees) =>
        Pair("Alternative", EMPTY);

      case Bind(name, rhs) =>
        Pair("Bind", name);

      case Match(selector, cases) =>
        Pair("Visitor", EMPTY);

      case Function(vparams, body) =>
        Pair("Function", EMPTY);

      case Assign(lhs, rhs) =>
        Pair("Assign", EMPTY);

      case If(cond, thenp, elsep) =>
        Pair("If", EMPTY);

      case Return(expr) =>
        Pair("Return", EMPTY);

      case Throw(expr) =>
        Pair("Throw", EMPTY);

      case New(init) =>
        Pair("New", EMPTY);

      case Typed(expr, tpe) =>
        Pair("Typed", EMPTY);

      case TypeApply(fun, args) =>
        Pair("TypeApply", EMPTY);

      case Apply(fun, args) =>
        Pair("Apply", EMPTY);

      case Super(qualif, mix) =>
        Pair("Super", qualif.toString() + ", mix: " + mix.toString());

      case This(qualifier) =>
        Pair("This", qualifier);

      case Select(qualifier, selector) =>
        Pair("Select", selector);

      case Ident(name) =>
        Pair("Ident", name);

      case Literal(value) =>
        Pair("Literal", EMPTY);

      case TypeTree() =>
        Pair("TypeTree", EMPTY);

      case SingletonTypeTree(ref) =>
        Pair("SingletonType", EMPTY);

      case SelectFromTypeTree(qualifier, selector) =>
        Pair("SelectFromType", selector);
      
      case CompoundTypeTree(template) =>
        Pair("CompoundType", EMPTY);

      case AppliedTypeTree(tpe, args) =>
        Pair("AppliedType", EMPTY);

      case Try(block, catcher, finalizer) =>
        Pair("Try", EMPTY);

      case EmptyTree =>
        Pair("Empty", EMPTY);

      case ArrayValue(elemtpt, trees) =>
        Pair("ArrayValue", EMPTY);

      case Star(t) =>
        Pair("Star", EMPTY);
    }

    /** Return a list of children for the given tree node */
    def children(t: Tree): List[Tree] = t match {
      case ProgramTree(units) =>
        units;

      case UnitTree(unit) =>
        List(unit.body);

      case DocDef(comment, definition) =>
        List(definition);

      case ClassDef(mods, name, tparams, tpt, impl) => {
        var children: List[Tree] = List();
        children = tparams ::: children;
        tpt :: impl :: children
      }

      case PackageDef(name, stats) => 
        stats;

      case ModuleDef(mods, name, impl) =>
        List(impl);

      case ValDef(mods, name, tpe, rhs) =>
        List(tpe, rhs);

      case DefDef(mods, name, tparams, vparams, tpe, rhs) => {
        var children: List[Tree] = List();
        children = tparams ::: children;
        children = List.flatten(vparams) ::: children;
        tpe :: rhs :: children
      }

      case AbsTypeDef(mods, name, rhs, lobound) =>
        List(rhs, lobound);

      case AliasTypeDef(mods, name, tparams, rhs) => {
        var children: List[Tree] = List();
        children = tparams ::: children;
        rhs :: children
      }

      case Import(expr, selectors) => {
        var children: List[Tree] = List(expr);
        children
      }

      case CaseDef(pat, guard, body) =>
        List(pat, guard, body);

      case Template(parents, body) => 
        parents ::: body;

      case LabelDef(name, params, rhs) =>
        params ::: List(rhs);

      case Block(stats, expr) =>
        stats ::: List(expr);

      case Sequence(trees) =>
        trees;

      case Alternative(trees) =>
        trees;

      case Bind(name, rhs) =>
        List(rhs);

      case Match(selector, cases) =>
        selector :: cases;

      case Function(vparams, body) =>
        vparams ::: List(body);

      case Assign(lhs, rhs) =>
        List(lhs, rhs);

      case If(cond, thenp, elsep) =>
        List(cond, thenp, elsep);

      case Return(expr) =>
        List(expr);

      case Throw(expr) =>
        List(expr);

      case New(init) =>
        List(init);

      case Typed(expr, tpe) =>
        List(expr, tpe);

      case TypeApply(fun, args) =>
        List(fun) ::: args;

      case Apply(fun, args) =>
        List(fun) ::: args;

      case Super(qualif, mix) =>
        Nil;

      case This(qualif) =>
        Nil

      case Select(qualif, selector) =>
        List(qualif);

      case Ident(name) =>
        Nil;

      case Literal(value) =>
        Nil;

      case TypeTree() =>
        Nil;

      case SingletonTypeTree(ref) =>
        List(ref);

      case SelectFromTypeTree(qualif, selector) =>
        List(qualif);

      case CompoundTypeTree(templ) =>
        List(templ);

      case AppliedTypeTree(tpe, args) =>
        tpe :: args;

      case Try(block, catches, finalizer) =>
        block :: catches ::: List(finalizer);

      case ArrayValue(elemtpt, elems) =>
        elemtpt :: elems;
      
      case EmptyTree =>
        Nil;

      case Star(t) =>
        List(t)
    }

    /** Return a textual representation of this t's symbol */
    def symbolText(t: Tree): String = {
      var prefix = "";
      
      if (t.hasSymbol)
        prefix = "[has] ";
      if (t.isDef)
        prefix = "[defines] ";
      
      prefix + t.symbol
    }

    /** Return t's symbol type  */
    def symbolTypeDoc(t: Tree): Document = {
      val s = t.symbol;
      if (s != null) 
        TypePrinter.toDocument(s.info);
      else
        DocNil;
    }

    /** Return a textual representation of (some of) the symbol's
     * attributes */
    def symbolAttributes(t: Tree): String = {
      val s = t.symbol;
      var att = "";

      if (s != null) {
        var str = flagsToString(s.flags);
        if (s.hasFlag(STATIC) || s.hasFlag(STATICMEMBER))
          str = str + " isStatic ";
        str
      }
        else "";
    }
  }

  object TypePrinter {

    ///////////////// Document pretty printer ////////////////

    implicit def view(n: String): Document = DocText(n);

    def toDocument(sym: Symbol): Document = 
      toDocument(sym.info);

    def symsToDocument(syms: List[Symbol]): Document = syms match {
      case Nil => DocNil;
      case s :: Nil => Document.group(toDocument(s));
      case _ =>  
        Document.group(
          syms.tail.foldLeft (toDocument(syms.head) :: ", ") ( 
            (d: Document, s2: Symbol) => toDocument(s2) :: ", " :/: d) );
    }

    def toDocument(ts: List[Type]): Document = ts match {
      case Nil => DocNil;
      case t :: Nil => Document.group(toDocument(t));
      case _ =>  
        Document.group(
          ts.tail.foldLeft (toDocument(ts.head) :: ", ") ( 
            (d: Document, t2: Type) => toDocument(t2) :: ", " :/: d) );
    }

    def toDocument(t: Type): Document = t match {
      case ErrorType => "ErrorType()";
      case WildcardType => "WildcardType()";
      case NoType => "NoType()";
      case NoPrefix => "NoPrefix()";
      case ThisType(s) => "ThisType(" + s.name + ")";

      case SingleType(pre, sym) => 
        Document.group(
          Document.nest(4, "SingleType(" :/:  
                      toDocument(pre) :: ", " :/: sym.name.toString() :: ")") 
        );

      case ConstantType(value) =>
         "ConstantType(" + value + ")";

      case TypeRef(pre, sym, args) =>
        Document.group(
          Document.nest(4, "TypeRef(" :/: 
                        toDocument(pre) :: ", " :/:
                        sym.name.toString() :: ", " :/:
                        "[ " :: toDocument(args) ::"]" :: ")")
          );

       case TypeBounds(lo, hi) =>
         Document.group(
           Document.nest(4, "TypeBounds(" :/: 
                         toDocument(lo) :: ", " :/: 
                         toDocument(hi) :: ")")
         );

       case RefinedType(parents, defs) =>
        Document.group(
          Document.nest(4, "RefinedType(" :/:
                        toDocument(parents) :: ")")
        );

      case ClassInfoType(parents, defs, clazz) =>
        Document.group(
          Document.nest(4,"ClassInfoType(" :/:
                        toDocument(parents) :: ", " :/:
                        clazz.name.toString() :: ")")
        );

      case MethodType(paramtypes, result) =>
        Document.group(
          Document.nest(4, "MethodType(" :/:
                        Document.group("(" :/:
                                       toDocument(paramtypes) :/:
                                       "), ") :/:
                        toDocument(result) :: ")")
          );

      case PolyType(tparams, result) =>
        Document.group(
          Document.nest(4,"PolyType(" :/:
                        Document.group("(" :/: 
                                       symsToDocument(tparams) :/:
                                       "), ") :/:
                        toDocument(result) :: ")")
        );

      case _ => abort("Unknown case: " + t.toString());
    }
  }

}
