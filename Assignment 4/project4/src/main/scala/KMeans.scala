import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import scala.io.Source

object KMeans {
	type Point = (Double,Double)

	var centroids: Array[Point] = Array[Point]()

	def main(args: Array[ String ]) {
    /* ... */
	val conf = new SparkConf().setAppName("kmeans");
	val sc = new SparkContext(conf);

	
	
	 
	val r = sc.textFile(args(1))
	centroids = r.map(eL => {val temp = eL.split(",")
													(temp(0).toDouble,temp(1).toDouble).asInstanceOf[Point]}).collect()

  
	for ( i <- 1 to 5 ) {
       val cs = sc.broadcast(centroids)
       val k = sc.textFile(args(0))	    
       centroids = k.map(p => {val t = p.split(",")
											(t(0).toDouble,t(1).toDouble)}).map(p=>(distBP(p,cs.value),p)).groupByKey().map(kv => finalC(kv._2)).collect()

    }
	centroids.foreach(println)
  
  
  
	}
	def distBP(p:Point,aP:Array[Point]) = {
		var c=0
		var finalP:Point = (0.0,0.0)
		var leastDist:Double=0
		var dist:Double =0
		for(aPI:Point <- aP){
			dist = math.sqrt(math.pow((p._1-aPI._1),2) + math.pow((p._2-aPI._2),2))
			if(c==0){
				leastDist = dist
				finalP = aPI
				c = c+1
			}
			if(dist<leastDist){
				leastDist = dist
				finalP = aPI
			}
	  
		}
		finalP
	}

	def finalC(iP:Iterable[Point]) = {
		val c = iP.size
		var xT:Double = 0
		var yT:Double = 0
	
		for(eP:Point <- iP){
			xT = xT + eP._1
			yT = yT + eP._2
		}
		var fxT:Double = xT/c
		var fyT:Double = yT/c
		(fxT,fyT).asInstanceOf[Point]
	
	}


}





