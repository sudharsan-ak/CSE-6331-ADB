import org.apache.spark.rdd.RDD._
import org.apache.spark.{SparkConf, SparkContext}
import scala.collection.mutable.ListBuffer
import scala.math

object Partition {
  val depth = 6

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Graph")
    val cont = new SparkContext(conf)
    var count = 0
    var graph = cont.textFile(args(0)).map(x => x.split(",")).map(x => x.map(_.toLong)).map(x => if (count < 5) {count += 1;(x(0), x(0), x.drop(1).toList)} else {count += 1;(x(0), -1.toLong, x.drop(1).toList)})
    for (i <- 1 to depth) {
      graph = graph.flatMap{f1(_)}.reduceByKey(math.max(_, _)).join(graph.map(x => f2(x))).map(x => f3(x))

    }
    graph.map(x => (x._2, 1)).reduceByKey(_ + _).collect().foreach(println)
	
	
	
	
	def f1(i: (Long, Long, List[Long])) = {
      var buffer_list = new ListBuffer[(Long, Long)]
      var temp_val = (i._1, i._2)
      buffer_list += temp_val
      if (i._2 > 0) {
        for (x <- i._3) {
          temp_val = (x, i._2)
          buffer_list += temp_val
        }
      }
      buffer_list
    }

    def f2(i: (Long, Long, List[Long])): (Long, (Long, List[Long])) = {
      var f2_op = (i._1, (i._2, i._3))
      f2_op
    }

    

    def f3(id: (Long, (Long, (Long, List[Long])))): (Long, Long, List[Long]) = {
      var final_op = (id._1, id._2._1, id._2._2._2)
      if (id._1 != -1 && id._2._2._1 != -1) {
        final_op = (id._1, id._2._2._1, id._2._2._2)
      }
      if (id._1 != -1 && id._2._2._1 == -1) {
        final_op = (id._1, id._2._1, id._2._2._2)
      }
      final_op
    }
  }
}