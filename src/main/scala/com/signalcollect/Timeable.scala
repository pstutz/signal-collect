package com.signalcollect

import com.signalcollect.interfaces.AggregationOperation
import scala.collection.immutable.SortedMap

/** Container for the deliver and collect duration measurements */
case class ActivityTime(deliver: Int, collect: Int) extends Ordered[ActivityTime] {
  override def toString: String = s"${deliver/1000000000.0}/${collect/1000000000.0}"
  def compare(that: ActivityTime) = ((that.deliver + that.collect) - (this.deliver + this.collect))
}

/** Finds the vertices in the graph which were active for the longest duration
  *
  * @param n the number of top vertices to find
  */
class TopActivityAggregator[Id](n: Int)
  extends AggregationOperation[SortedMap[ActivityTime,Id]] {
  type ActivityMap = SortedMap[ActivityTime,Id]
  def extract(v: Vertex[_, _]): ActivityMap = v match {
    case t: Timeable[Id, _] => SortedMap((ActivityTime(t.deliverTime, t.collectTime) -> t.id))
    case _ => SortedMap[ActivityTime,Id]()
  }
  def reduce(activities: Stream[ActivityMap]): ActivityMap = {
    activities.foldLeft(SortedMap[ActivityTime,Id]()) { (acc, m) => acc ++ m }.take(n)
  }
}

/** Allows measuring how long a vertex stays in deliverSignal and collect*/
trait Timeable[Id, State] extends Vertex[Id, State] {
  var deliverTime: Int = 0
  var collectTime: Int = 0
  def time[R](block: => R): (R, Int) = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    (result, (t1 - t0).toInt)
  }
  abstract override def deliverSignal(signal: Any, sourceId: Option[Any],
                        graphEditor: GraphEditor[Any, Any]): Boolean = {
    val (result, t) = time(super.deliverSignal(signal, sourceId, graphEditor))
    deliverTime += t
    result
  }
  /*abstract override def collect(signal: Signal): State = {
    val (result, t) = time(super.collect(signal))
    collectTime += t
    result
  }*/
}

