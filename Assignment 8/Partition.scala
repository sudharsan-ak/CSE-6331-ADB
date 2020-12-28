import org.apache.spark.graphx.{Graph, VertexId, Edge}
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

object Partition {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Partition")
    val sc = new SparkContext(conf)

    // Used for reading the edges
    val edges: RDD[Edge[Long]] = sc
      .textFile(args(0))
      .map(line => {
        val (node, adjacent) = line.split(",").splitAt(1)
        var vertexid = node(0).toLong
        (vertexid, adjacent.toList.map(_.toLong))
      })
      .flatMap(x => x._2.map(y => (x._1, y)))
      .map(n => {
        Edge(n._1, n._2, 0L)
      })

    // Below is for the first five vertices alone
    var count = 0
    val ff = sc
      .textFile(args(0))
      .map(line => {
        val (node, _) = line.split(",").splitAt(1)
        var vx = -1L
        if (count < 5) {
          vx = node(0).toLong
          count += 1
        }
        vx // RDD[Long]
      })

    val fffinal = ff.filter(_ != -1).collect().toList

    /*
    def mapVertices[VD2](map: (VertexId, VD) => VD2): Graph[VD2, ED]
     */
    // This is for creating the GraphX Graph from the edges and resetting the vertex numbers
    val graph: Graph[Long, Long] = Graph
      .fromEdges(edges, 0L)
      .mapVertices((id, _) => {
        var centroid = -1L
        if (fffinal.contains(id)) {
          centroid = id
        }
        centroid
      })

    val i = graph.pregel(Long.MinValue, 6)(
      (vid, vdata, candidateCluster) => {
        if (vdata == -1) {
          math.max(vdata, candidateCluster)
        } else {
          vdata
        }
      },
      triplet => {
        Iterator((triplet.dstId, triplet.srcAttr))
      },
      (a, b) => math.max(a, b)
    )

    /* Printing the partition sizes */
    var partitionSizes = i.vertices
      .map {
        case (id, centroid) =>
          (centroid, 1)
      }
      .reduceByKey(_ + _)

    partitionSizes.collect.foreach(println)
  }
}