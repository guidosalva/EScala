package scala.events

object emptyevent extends Event[Nothing] {
  def +=(sink: Sink) { /* do nothing */ }
  def -=(sink: Sink) { /* do nothing */ }
  def +=(react: Nothing => Unit) { /* do nothing */ }
  def -=(react: Nothing => Unit) { /* do nothing */ }
  def redeploy{}
  def pullIsActivated(Id : Int) = None
}

// vim: set ts=4 sw=4 et:
