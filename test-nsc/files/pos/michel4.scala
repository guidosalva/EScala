class A() {
  val f : Int = 2
}

class B() extends A() {
  override val f : Int = super.f
}