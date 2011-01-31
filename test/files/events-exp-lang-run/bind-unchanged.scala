import scala.events._

class C {
  imperative evt e1[Int,String]
  evt e2(x: Int, y: String) = e1(x,y)

  imperative evt e3[Int]
  evt e4(x: Int) = e3(x)
}

object Test {
  def main(args: Array[String]) {
    val o = new C
    o.e2 += { (x: Int,y: String) => println("e2 "+x+" "+y) }
    o.e4 += { (x: Int) => println("e4 "+x) }

    o.e1(1,"Hello")
    o.e3(1)

  }
}
