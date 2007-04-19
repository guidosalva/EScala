/* NSC -- new Scala compiler
 * Copyright 2005-2007 LAMP/EPFL
 * @author  Sean McDirmid
 */
// $Id$

package scala.tools.nsc.doc

import java.io.{File, FileWriter}

import scala.collection.jcl
import scala.compat.Platform.{EOL => LINE_SEPARATOR}
import scala.xml._

/** This class provides HTML document framing functionality. 
  *
  *  @author  Sean McDirmid, Stephane Micheloud
  */
trait ModelFrames extends ModelExtractor {
  import DocUtil._;
  def outdir: String;
  def windowTitle: String;
  def documentTitle: String;
  def contentFrame = "contentFrame";
  def classesFrame = "classesFrame";
  def modulesFrame = "modulesFrame";
  protected val FILE_EXTENSION_HTML = ".html";
  protected val NAME_SUFFIX_OBJECT  = "$object";
  protected val NAME_SUFFIX_PACKAGE = "$package";
  
  
  def rootTitle = <div class="page-title"> Scala 2<br/>API Specification</div>;
  def rootDesc = <p>This document is the API specification for Scala 2.</p>;

  final def hasLink(sym: global.Symbol) : Boolean = {
    if (sym == global.NoSymbol) false;
    else if (hasLink0(sym)) true;
    else {
      hasLink(decode(sym.owner));
    }
  }
  def hasLink0(sym: global.Symbol): Boolean = true;
  
  abstract class Frame extends UrlContext {
    { // just save.
      save(page(title, body, hasBody));
    }
    def path: String // relative to outdir
    def relative: String = {
      assert(path ne null)
      var idx = 0
      var ct = new StringBuilder
      while (idx != -1) {
        idx = path.indexOf('/', idx)
        //System.err.println(path + " idx=" + idx)
        ct.append(if (idx != -1) "../" else "")
        idx = idx + (if (idx == -1) 0 else 1)
      }
      ct.toString
    }
    def save(nodes: NodeSeq) = {
      val path0 = outdir + File.separator + path + FILE_EXTENSION_HTML
      //if (settings.debug.value) inform("Writing XML nodes to " + path0)
      val file = new File(path0)
      val parent = file.getParentFile()
      if (!parent.exists()) parent.mkdirs()
      val writer = new FileWriter(file)
      val str = dtype + LINE_SEPARATOR + nodes.toString()
      writer.write(str, 0, str.length())
      writer.close()
    }
    protected def body: NodeSeq;
    protected def title: String;
    protected def hasBody = true;
    
    //def urlFor(entity : Entity, target : String) : NodeSeq;
    def urlFor(entity : Entity) : String = {
      val ret = this.urlFor(entity.sym);
      assert(ret != null); ret;
    }
    def link(entity : Entity, target : String) = aref(urlFor(entity), target, entity.name);
    protected def shortHeader(entity : Entity) : NodeSeq;
    protected def  longHeader(entity : Entity) : NodeSeq;
    import global._;
    import symtab.Flags;

    def urlFor(sym: Symbol): String = sym match {
    case sym : TypeSymbol if sym == definitions.AnyRefClass => urlFor0(sym,sym) + FILE_EXTENSION_HTML;
    case psym : ModuleSymbol if psym.isPackage => urlFor0(sym,sym) + FILE_EXTENSION_HTML;
    case sym if !hasLink(sym) => null;
    case msym: ModuleSymbol => urlFor0(sym, sym) + FILE_EXTENSION_HTML
    case csym: ClassSymbol =>  urlFor0(sym, sym) + FILE_EXTENSION_HTML
    case _ => 
      val cnt = urlFor(decode(sym.owner));
      if (cnt == null) null else cnt + "#" + docName(sym)
    }
    def docName(sym: Symbol): String = {
      def javaParams(paramTypes: List[Type]): String = {
        def javaName(pt: Type): String = {
          val s = pt.toString
          val matVal = patVal.matcher(s)
          if (matVal.matches) matVal.group(1).toLowerCase
          else s.replaceAll("\\$", ".")
        }
        paramTypes.map(pt => javaName(pt)).mkString("(", ",", ")")
      }
      def scalaParams(paramTypes: List[Type]): String = {
        def scalaName(pt: Type): String = pt.toString.replaceAll(" ", "")
        paramTypes.map(pt => scalaName(pt)).mkString("(", ",", ")")
      }
      java.net.URLEncoder.encode(sym.nameString +
        (sym.tpe match {
          case MethodType(paramTypes, _) =>
            if (sym hasFlag Flags.JAVA) javaParams(paramTypes)
            else scalaParams(paramTypes)
          case PolyType(_, MethodType(paramTypes, _)) =>
            if (sym hasFlag Flags.JAVA) javaParams(paramTypes)
            else scalaParams(paramTypes)
          case _ => ""
        }), encoding)
    }
    def urlFor0(sym: Symbol, orig: Symbol): String = {
      (if (sym == NoSymbol) "XXX"
       else if (sym.owner.isPackageClass) {
         rootFor(sym) + sym.fullNameString('/');
       } else urlFor0(decode(sym.owner), orig) + "." + Utility.escape(sym.nameString)) +
      (sym match {
        case msym: ModuleSymbol =>
          if (msym hasFlag Flags.PACKAGE) NAME_SUFFIX_PACKAGE
          else NAME_SUFFIX_OBJECT
        case csym: ClassSymbol =>
          if (csym.isModuleClass) {
            if (csym hasFlag Flags.PACKAGE) NAME_SUFFIX_PACKAGE
            else NAME_SUFFIX_OBJECT
          }
          else ""
        case _ => ""
      })
    }
  }
  protected def rootFor(sym: global.Symbol) = "";
  
  private val doctitle: NodeSeq =
    <div class="doctitle-larger">{load(documentTitle)}</div>;

  abstract class AllPackagesFrame extends Frame {
    override val path  = "modules";
    override val title = "List of all packages";
    def packages : Iterable[Package];
    override def body: NodeSeq =
      <div>
        {doctitle}
        <a href="all-classes.html" target={classesFrame} onclick="resetKind();">{"All objects and classes"}</a>
      </div>
      <div class="kinds">Packages</div>
      <ul class="list">{sort(packages).mkXML("","\n","")(pkg => {
        <li><a href={urlFor(pkg)} target={classesFrame} onclick="resetKind();">
          {pkg.fullName('.')}</a></li>
      })}
      </ul>;
  }
  abstract class PackagesContentFrame extends Frame {
    val path  = "root-content";
    val title = "All Packages";
    def packages : Iterable[Package];
    //def modules: TreeMap[String, ModuleClassSymbol]
    def body: NodeSeq =
      {rootTitle} ++ {rootDesc} ++ <hr/> ++
      <table cellpadding="3" class="member" summary="">
        <tr><td colspan="2" class="title">Package Summary</td></tr> 
        {sort(packages).mkXML("","\n","")(pkg => <tr><td class="signature">
          <code>package
          {aref(pkg.fullName('/') + "$content.html", "_self", pkg.fullName('.'))}
          </code>
        </td></tr>)}
      </table>;
  }

  val classFrameKinds = Objects :: Classes :: Nil;
  abstract class ListClassFrame extends Frame {
    def classes: Iterable[ClassOrObject]
    def navLabel: String
    private def navPath = {
      val p = path;
      (if (p endsWith NAME_SUFFIX_PACKAGE)
        p.substring(0, p.length() - NAME_SUFFIX_PACKAGE.length());
      else p) + navSuffix;
    }
    protected def navSuffix = "$content.html";
    
    def body: NodeSeq = {
      val nav = if (navLabel == null) NodeSeq.Empty else
        <table class="navigation" summary="">
          <tr><td valign="top" class="navigation-links">
            {aref(navPath, contentFrame, navLabel)}
          </td></tr>
        </table>;
      val ids = new jcl.LinkedHashSet[String];
      def idFor(kind: Category, t: Entity)(seq : NodeSeq): NodeSeq = {
        val ch = t.listName.charAt(0);
        val id = kind.plural + "_" + ch;
        if (ids contains id) <li>{seq}</li>;
        else { 
          ids += id; 
          <li id={id}>{seq}</li>
        };
      }
      val body = <div>{classFrameKinds.mkXML("","\n","")(kind => {
        val classes = sort(this.classes.filter(e => kind.f(e.sym)));
        if (classes.isEmpty) NodeSeq.Empty; else
        <div id={kind.plural} class="kinds">{Text(kind.plural)}</div>
        <ul class="list">
        {classes.mkXML("","\n","")(cls => {
          idFor(kind, cls)(
            aref(urlFor(cls), contentFrame, cls.listName) ++ optional(cls)
          );
        })}
        </ul>;
      })}</div>;
      nav ++ body
    }
    def optional(cls : ClassOrObject) : NodeSeq = NodeSeq.Empty;
  }
  abstract class PackageContentFrame extends Frame {
    override def path  = pkg.fullName('/') + "$content";
    override def title = "All classes and objects in " + pkg.fullName('.');
    protected def pkg : Package;
    protected def classes : Iterable[ClassOrObject];
    def body: NodeSeq =
      {rootTitle} ++ {rootDesc} ++ {classFrameKinds.mkXML("","\n","")(kind => {
        val classes = sort(this.classes.filter(e => kind.f(e.sym) && e.isInstanceOf[TopLevel]));
        if (classes.isEmpty) NodeSeq.Empty else
        <table cellpadding="3" class="member" summary="">
        <tr><td colspan="2" class="title">{kind.label} Summary</td></tr>
        {classes.mkXML("","\n","")(shortHeader)}        
        </table>
      })};
  }
  abstract class ClassContentFrame extends Frame {
    def clazz: ClassOrObject;
    def body: NodeSeq = <span>{navigation}{header0}{longHeader(clazz)}</span>;
    final def path = urlFor0(clazz.sym, clazz.sym)
    private def navigation: NodeSeq =
      <table class="navigation" summary="">
        <tr>
          <td valign="top" class="navigation-links">
            <table><tr>
            </tr></table>
          </td>
          <td align="right" valign="top" style="white-space:nowrap;" rowspan="2">
            {doctitle}
          </td>
        </tr>
        <tr><td></td></tr>
      </table>;
    private def header0: NodeSeq = <span>
      <div class="entity">
        {Text(clazz.kind)}
        <span class="entity">{Text(clazz.name)}</span> in
        {aref(urlFor(decode(clazz.sym.owner)), "_self", decode(clazz.sym.owner).fullNameString('.'))}
      </div><hr/>
    </span>;
  }  
  def longComment(cmnt : Comment) : NodeSeq;
  
  val index =
    <frameset cols="25%, 75%">
    <frameset rows="50%, 28, 50%">
    <frame src="modules.html" name={modulesFrame}></frame>
    <frame src="nav-classes.html" name="navigationFrame"></frame>
    <frame src="all-classes.html" name={classesFrame}></frame>
    </frameset>
    <frame src="root-content.html" name={contentFrame}></frame>
    </frameset>;

  val root = <b></b>;
  
  abstract class RootFrame extends Frame {
    def title = windowTitle
    def body = index
    def path = "index"
    override def hasBody = false
  }

  val indexChars = 'A' :: 'B' :: 'C' :: 'D' :: 'E' :: 'G' :: 'I' :: 'L' :: 'M' :: 'P' :: 'R' :: 'T' :: 'V' :: 'X' :: Nil;

  abstract class NavigationFrame extends Frame {
    def title="navigation"
    def path="nav-classes"
    override def body0(hasBody: Boolean, nodes: NodeSeq): NodeSeq =
      if (!hasBody) nodes
      else <body style="margin:1px 0 0 1px; padding:1px 0 0 1px;">{nodes}</body>;
    def body =
      <form>
        <select id="kinds" onchange="gotoKind()">
          <option value="#Classes" selected="selected">Classes</option>
          <option value="#Objects">Objects</option>
        </select>
        <span id="alphabet" style="font-family:Courier;word-spacing:-8px;">{
          indexChars.mkXML("","\n","")(c => {
          <a href={Unparsed("javascript:gotoName(\'" + c + "\')")}>{c}</a>
          });
	}
        </span>
      </form>
  }
  
  private val loader = getClass().getClassLoader()
  def copyResources = {
    import java.io._
    // The name of a resource is a '/'-separated path name that identifies the resource.
    val rsrcdir = "scala/tools/nsc/doc/"
    for (val base <- List("style.css", "script.js")) {
      try {
        val in = loader.getResourceAsStream(rsrcdir + base)
        val out = new FileOutputStream(new File(outdir + File.separator + base))
        val buf = new Array[byte](1024)
        var len = 0
        while (len != -1) {
          out.write(buf, 0, len)
          len = in.read(buf)
        }
        in.close()
        out.close()
      } catch {
      case _ =>
        System.err.println("Resource file '" + rsrcdir + base + "' not found")
      }
    }
  }
  private val patVal = java.util.regex.Pattern.compile(
      "scala\\.(Byte|Boolean|Char|Double|Float|Int|Long|Short)")
}
