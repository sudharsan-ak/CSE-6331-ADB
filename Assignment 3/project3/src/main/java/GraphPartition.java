// Sudharsan Srinivasan (UTA-ID: 1001755919)
// List of Hadoop classes required for this:
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;

// List of Java classes required for this:
import java.io.*;
import java.io.Serializable.*;
import java.lang.*;
import java.util.*;
import java.util.Scanner;

public class GraphPartition{

	// As given in the pseudocode by the Professor:
	static Vector<Long> centroids = new Vector<Long>();
	final static short max_depth = 8;
        static short BFS_depth = 0;

	// Creating a class to represent the Vertex with ID, Adjacent, Centroid and Depth:
	public static class Vertex implements Writable {
		public long id;
		public Vector<Long> adjacent;
		public long centroid;
		public short depth;

		// Getters and Setters for the above created variables:
		public long getId(){
			return id;
		}

		public void setId(long id){
			this.id = id;
		}

		public Vector<Long> getAdjacent(){
			return adjacent;
		}

		public void setAdjacent(Vector<Long> adjacent){
			this.adjacent = adjacent;
		}

		public long getCentroid(){
			return centroid;
		}

		public void setCentroid(long centroid){
			this.centroid = centroid;
		}

		public short getDepth(){
			return depth;
		}

		public void setDepth(short depth){
			this.depth = depth;
		}

		public Vertex(){
		}


		public Vertex(long id, Vector<Long> adjacent, long centroid, short depth){
			this.id = id;
			this.adjacent = adjacent;
			this.centroid = centroid;
			this.depth = depth;
		}

		public final void readFields(DataInput in) throws IOException {
		this.id = in.readLong();
		this.centroid = in.readLong();
		this.depth = in.readShort();
		this.adjacent= readingVector(in);
		}

		public void write(DataOutput out) throws IOException {
			out.writeLong(this.id);
			out.writeLong(this.centroid);
			out.writeShort(this.depth);
			for(int i=0;i<this.adjacent.size();i++){
				out.writeLong(this.adjacent.get(i));
			}
    }

		public static Vector readingVector(DataInput in) throws IOException {
			int i=1;
			Vector<Long> vector = new Vector<Long>();
            Long put;
            while(i>0){
				try{
					// If the long variable's input is not equal to -1, insert it to the vector. Else i = 0.
					if((put=in.readLong())!= -1)
						vector.add(put);
                    else
										i = 0;
                }
								// End-Of-File Exception
                catch(EOFException eof){
									i = 0;
                }
            }
            return vector;
        }

	public int comparingTo(Vertex o){
			return 0;
		}

		public String toString(){
			return(id+","+adjacent+","+centroid+","+depth);
		}
	}

	public static class FirstMapper extends Mapper<LongWritable,Text,LongWritable,Vertex>{
		private int index;
		private Short depth;

		protected void setup(Context context) throws IOException, InterruptedException{
			depth = 0;
			index = 0;
		}

		public void map(LongWritable key, Text value, Context con) throws IOException,InterruptedException{
			long centroid;
			Vector<Long> adjacent = new Vector<Long>();
			String line = value.toString();
			String[] lineSplitArray = line.split(","); //lineSplitArray is the array in which all the values from the input line are stored after splitting
			long id = Long.parseLong(lineSplitArray[0]);
			// for the first 10 vertices, centroid = id; for all the others, centroid = -1
			for(int i=1;i<lineSplitArray.length;i++){
				adjacent.add(Long.parseLong(lineSplitArray[i]));
			}
			if(index<10){
				centroid = id;
				con.write(new LongWritable(id), new Vertex(id,adjacent,centroid,depth));
			}
			else{
				centroid = -1;
				con.write(new LongWritable(id), new Vertex(id,adjacent,centroid,depth));
			}
			index = index + 1;
		}
	}

	public static class SecondMapper extends Mapper<LongWritable,Vertex,LongWritable,Vertex>{
		public void map(LongWritable key, Vertex value, Context con) throws IOException,InterruptedException{
			con.write(new LongWritable(value.getId()), value); // pass the graph topology
			Long centroid = value.getCentroid();
			Vector<Long> adjacentForSecondMapper = new Vector<Long>();
			adjacentForSecondMapper = value.getAdjacent();
			Vector<Long> Vect = new Vector<Long>();
			if(centroid > 0){
				for(int i=0;i<adjacentForSecondMapper.size();i++){
					con.write(new LongWritable(adjacentForSecondMapper.get(i)), new Vertex(adjacentForSecondMapper.get(i),Vect,centroid,BFS_depth));
				}
			}
		}
	}

	public static class SecondReducer extends Reducer<LongWritable,Vertex,LongWritable,Vertex>{
		public void reduce(LongWritable key, Iterable<Vertex> values, Context con) throws IOException, InterruptedException{
			Short min_depth = 1000; //try with short
			Vector<Long> vec = new Vector<Long>();
			Short tempVariable = 0;
			Vertex m = new Vertex(-1,vec,-1,tempVariable);
			for(Vertex v: values){
				if(!v.getAdjacent().isEmpty()){
					m.setAdjacent(v.getAdjacent());
				}
				if(v.getCentroid()>0 && v.getDepth()<min_depth){
					min_depth = v.getDepth();
					m.setCentroid(v.getCentroid());
				}
				m.setId(v.getId());
			}
			m.setDepth(min_depth);
			con.write(key,m);
		}
	}

	public static class ThirdMapper extends Mapper<LongWritable,Vertex,LongWritable,IntWritable>{
		public void map(LongWritable key, Vertex value, Context con) throws IOException,InterruptedException{
			Long cent = value.getCentroid();
			con.write(new LongWritable(cent), new IntWritable(1));
		}
	}

	public static class ThirdReducer extends Reducer<LongWritable,IntWritable,LongWritable,IntWritable>{
		public void reduce(LongWritable key, Iterable<IntWritable> values, Context con) throws IOException, InterruptedException{
			int m = 0;
			for(IntWritable v: values){
				m = m + v.get();
			}
			con.write(key,new IntWritable(m));
		}
	}

	public static void main ( String[] args ) throws Exception {
        Job job = Job.getInstance();
        job.setJobName("MyJob");
        job.setJarByClass(GraphPartition.class);
        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(Vertex.class);
        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(Vertex.class);
        job.setMapperClass(FirstMapper.class);
        FileInputFormat.setInputPaths(job,new Path(args[0]));
        FileOutputFormat.setOutputPath(job,new Path(args[1]+"/i0"));
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        job.waitForCompletion(true);
        for ( short i = 0; i < max_depth; i++ ) {
            BFS_depth++;
            job = Job.getInstance();
           job.setJobName("MyJob");
           job.setJarByClass(GraphPartition.class);
           job.setOutputKeyClass(LongWritable.class);
           job.setOutputValueClass(Vertex.class);
           job.setMapOutputKeyClass(LongWritable.class);
					 job.setMapOutputValueClass(Vertex.class);
           job.setMapperClass(SecondMapper.class);
           job.setReducerClass(SecondReducer.class);
           FileInputFormat.setInputPaths(job,new Path(args[1]+"/i"+i));
           FileOutputFormat.setOutputPath(job,new Path(args[1]+"/i"+(i+1)));
           job.setInputFormatClass(SequenceFileInputFormat.class);
           job.setOutputFormatClass(SequenceFileOutputFormat.class);
           job.waitForCompletion(true);
        }
        job = Job.getInstance();
        job.setJobName("MyJob");
        job.setJarByClass(GraphPartition.class);
        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(IntWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
			  job.setMapOutputKeyClass(LongWritable.class);
        job.setMapperClass(ThirdMapper.class);
        job.setReducerClass(ThirdReducer.class);
        FileInputFormat.setInputPaths(job,new Path(args[1]+"/i8"));
        FileOutputFormat.setOutputPath(job,new Path(args[2]));
        job.setInputFormatClass(SequenceFileInputFormat.class);
			  job.waitForCompletion(true);
    }
}