package scala.events.pingpong

import scala.events.BetweenEvent
import scala.events.pingpong.World
import scala.events.ImperativeEvent

class Mover(val world : World) {	
  val moved = new ImperativeEvent[ModelObject]

  def move(o: ModelObject) {
	  
    if (o.velocity == (0, 0)) return
    o.position = (o.position._1 + o.velocity._1, o.position._2 + o.velocity._2)
    moved(o)
  }

  val reverseYVelocity = ((o: ModelObject) => {
    o.velocity = (o.velocity._1, -o.velocity._2)
  })
  val reverseXVelocity = ((o: ModelObject) => {
    o.velocity = (-o.velocity._1, o.velocity._2)
  })

  moved && (o => colliding(o, world.upperWall )) += {o => reverseYVelocity(o); o.position = (o.position._1,world.upperWall.position._2 + world.upperWall.boundingBox._2 + 2)}
  moved && (o => colliding(o, world.lowerWall )) += {o => reverseYVelocity(o); o.position = (o.position._1,world.lowerWall.position._2 - o.boundingBox._2 - 2)}

  val ballMoved = moved && (o => o.isInstanceOf[Ball]) map ((o: ModelObject) => o.asInstanceOf[Ball])

  ballMoved && (o => colliding(o, world.player1Bar) || colliding(o, world.player2Bar)) += reverseXVelocity
  ballMoved && (o => colliding(o,world.player1Goal )) += (_ => "Point for Player2")
  ballMoved && (o => colliding(o,world.player2Goal )) += (_ => "Point for Player1")

  def colliding(o1: ModelObject, o2: ModelObject) = o1.isCollidingWith(o2) || o2.isCollidingWith(o1)
}