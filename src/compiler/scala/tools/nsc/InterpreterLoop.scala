/* NSC -- new Scala compiler
 * Copyright 2005-2007 LAMP/EPFL
 * @author Alexander Spoon
 */
// $Id$

package scala.tools.nsc

import java.lang.{ClassLoader, System}
import java.io.{BufferedReader, InputStreamReader, File, FileReader, PrintWriter}
import java.io.IOException

import scala.tools.nsc.util.{ClassPath, Position}
import nsc.{InterpreterResults=>IR}
import nsc.interpreter._

/** The 
 *  <a href="http://scala-lang.org/" target="_top">Scala</a>
 *  interactive shell.  It provides a read-eval-print loop around
 *  the Interpreter class.
 *  After instantiation, clients should call the <code>main()</code> method.
 *
 *  <p>If no in0 is specified, then input will come from the console, and
 *  the class will attempt to provide input editing feature such as
 *  input history.
 *
 *  @author Moez A. Abdel-Gawad
 *  @author  Lex Spoon
 *  @version 1.2
 */
class InterpreterLoop(in0: Option[BufferedReader], out: PrintWriter) {
  def this(in0: BufferedReader, out: PrintWriter) =
    this(Some(in0), out)

  def this() = this(None, new PrintWriter(Console.out))

  /** The input stream from which interpreter commands come */
  var in: InteractiveReader = _  //set by main()

  /** The context class loader at the time this object was created */
  protected val originalClassLoader =
    Thread.currentThread.getContextClassLoader
  
  var settings: Settings = _ // set by main()
  var interpreter: Interpreter = null // set by createInterpreter()
  def isettings = interpreter.isettings

  /** A reverse list of commands to replay if the user
    * requests a :replay */
  var replayCommandsRev: List[String] = Nil

  /** A list of commands to replay if the user requests a :replay */
  def replayCommands = replayCommandsRev.reverse

  /** Record a command for replay should the user requset a :replay */
  def addReplay(cmd: String) =
    replayCommandsRev = cmd :: replayCommandsRev

  /** Close the interpreter, if there is one, and set
    * interpreter to <code>null</code>. */
  def closeInterpreter() {
    if (interpreter ne null) {
      interpreter.close
      interpreter = null
      Thread.currentThread.setContextClassLoader(originalClassLoader)
    }
  }

  /* As soon as the Eclipse plugin no longer needs it, delete uglinessxxx,
   * parentClassLoader0, and the parentClassLoader method in Interpreter
   */
  var uglinessxxx: ClassLoader = _
  def parentClassLoader0: ClassLoader = uglinessxxx

  /** Create a new interpreter.  Close the old one, if there
    * is one. */
  def createInterpreter() {
    //closeInterpreter()

    interpreter = new Interpreter(settings, out) {
      override protected def parentClassLoader = parentClassLoader0
    }
    interpreter.setContextClassLoader()
  }

  /** Bind the settings so that evaluated code can modiy them */
  def bindSettings() {
    interpreter.beQuietDuring {
      interpreter.compileString(InterpreterSettings.sourceCodeForClass)
	
      interpreter.bind(
        "settings",
        "scala.tools.nsc.InterpreterSettings", 
        isettings)
    }
  }


  /** print a friendly help message */
  def printHelp {
    printWelcome
    out.println("Type :load followed by a filename to load a Scala file.")
    out.println("Type :replay to reset execution and replay all previous commands.")
    out.println("Type :quit to exit the interpreter.")
  }
  
  /** Print a welcome message */
  def printWelcome() {
    out.println("Welcome to Scala " + Properties.versionString + ".")
    out.println("Type in expressions to have them evaluated.")
    out.println("Type :help for more information.")
    out.flush()
  }
  
  /** Prompt to print when awaiting input */
  val prompt = Properties.shellPromptString

  /** The main read-eval-print loop for the interpreter.  It calls
   *  <code>command()</code> for each line of input, and stops when
   *  <code>command()</code> returns <code>false</code>.
   */
  def repl() {
    var first = true
    while (true) {
      out.flush()

      val line =
	if (first) {
          /* For some reason, the first interpreted command always takes
           * a second or two.  So, wait until the welcome message
           * has been printed before calling bindSettings.  That way,
           * the user can read the welcome message while this
           * command executes.
           */
          val futLine = scala.concurrent.ops.future(in.readLine(prompt))

          bindSettings()
          first = false

	  futLine()
      } else {
	in.readLine(prompt)
      }

      if (line eq null)
        return ()  // assumes null means EOF

      val (keepGoing, finalLineMaybe) = command(line)

      if (!keepGoing)
        return
        
      finalLineMaybe match {
        case Some(finalLine) => addReplay(finalLine)
        case None => ()
      }
    }
  }

  /** interpret all lines from a specified file */
  def interpretAllFrom(filename: String) {
    val fileIn = try {
      new FileReader(filename)
    } catch {
      case _:IOException =>
        out.println("Error opening file: " + filename)
        return
    }
    val oldIn = in
    try {
      val inFile = new BufferedReader(fileIn)
      in = new SimpleReader(inFile, out, false)
      out.println("Loading " + filename + "...")
      out.flush
      repl
    } finally {
      in = oldIn
      fileIn.close
    }
  }

  /** create a new interpreter and replay all commands so far */
  def replay() {
    closeInterpreter()
    createInterpreter()
    for (cmd <- replayCommands) {
      out.println("Replaying: " + cmd)
      command(cmd)
      out.println
    }
  }

  /** Run one command submitted by the user.  Three values are returned:
    * (1) whether to keep running, (2) the line to record for replay,
    * if any. */
  def command(line: String): (Boolean, Option[String]) = {
    def withFile(command: String)(action: String => Unit): Unit = {
      val spaceIdx = command.indexOf(' ')
      if (spaceIdx <= 0) {
        out.println("That command requires a filename to be specified.")
        return ()
      }
      val filename = command.substring(spaceIdx).trim
      if (! new File(filename).exists) {
        out.println("That file does not exist")
        return ()
      }
      action(filename)
    }

    val helpRegexp    = ":h(e(l(p)?)?)?"
    val quitRegexp    = ":q(u(i(t)?)?)?"
    val loadRegexp    = ":l(o(a(d)?)?)?.*"
    val replayRegexp  = ":r(e(p(l(a(y)?)?)?)?)?.*"

    var shouldReplay: Option[String] = None

    if (line.matches(helpRegexp))
      printHelp
    else if (line.matches(quitRegexp))
      return (false, None)
    else if (line.matches(loadRegexp)) {
      withFile(line)(f => {
        interpretAllFrom(f)
        shouldReplay = Some(line)
      })
    }
    else if (line matches replayRegexp)
      replay
    else if (line startsWith ":")
      out.println("Unknown command.  Type :help for help.")
    else 
      shouldReplay = interpretStartingWith(line)

    (true, shouldReplay)
  }
  
  /** Interpret expressions starting with the first line.
    * Read lines until a complete compilation unit is available
    * or until a syntax error has been seen.  If a full unit is
    * read, go ahead and interpret it.  Return the full string
    * to be recorded for replay, if any.
    */
  def interpretStartingWith(code: String): Option[String] =
  {
    interpreter.interpret(code) match {
      case IR.Success => Some(code)
      case IR.Error => None
      case IR.Incomplete =>
        if (in.interactive && code.endsWith("\n\n")) {
          out.println("You typed two blank lines.  Starting a new command.")
          None
        } else {
          val nextLine = in.readLine("     | ")
          if (nextLine == null)
            None  // end of file
          else
            interpretStartingWith(code + "\n" + nextLine)
        }
    }
  }


      
  def main(settings: Settings) {
    this.settings = settings

    in = 
      in0 match {
	case Some(in0) => new SimpleReader(in0, out, true)

	case None =>
	  if (settings.Xnojline.value)
	    new SimpleReader()
	  else
	    InteractiveReader.createDefault()
      }


    uglinessxxx = 
      new java.net.URLClassLoader(
                 ClassPath.expandPath(settings.classpath.value).
                         map(s => new File(s).toURL).toArray,
		 classOf[InterpreterLoop].getClassLoader)

    createInterpreter()

    try {
      if (interpreter.reporter.hasErrors) {
        return // it is broken on startup; go ahead and exit
      }
      printWelcome()
      repl()
    } finally {
      closeInterpreter()
    }
  }
  
  /** process command-line arguments and do as they request */
  def main(args: Array[String]) {
    def error1(msg: String) { out.println("scala: " + msg) }
    val command = new InterpreterCommand(List.fromArray(args), error1)

    if (!command.ok || command.settings.help.value || command.settings.Xhelp.value) {
      // either the command line is wrong, or the user
      // explicitly requested a help listing
      if (command.settings.help.value) out.println(command.usageMsg)
      if (command.settings.Xhelp.value) out.println(command.xusageMsg)
      out.flush
    }
    else
      main(command.settings)
  }
}
