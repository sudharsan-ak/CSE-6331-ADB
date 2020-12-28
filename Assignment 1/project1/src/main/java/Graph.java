import java.io.*;
import java.util.Scanner;
import java.io.File;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;


public class Graph {
	public static class MapperNeighbor extends Mapper<Object,Text,LongWritable,LongWritable> {
        	//@Override
        	public void map ( Object key, Text value, Context con )
                	        throws IOException, InterruptedException {
			
			LongWritable one = new LongWritable(1);

            		String[] commasplit = value.toString().split(",");
            		long w = Long.parseLong(commasplit[0]);
            		
	            	con.write(new LongWritable(w),one);
        
        }
    }

    public static class ReducerNeighbor extends Reducer<LongWritable,LongWritable,LongWritable,LongWritable> {
        //@Override
        public void reduce ( LongWritable key, Iterable<LongWritable> values, Context con )
                           throws IOException, InterruptedException {
            long reduce_total = 0;
            for (LongWritable v: values) {
                reduce_total += v.get();
            };
            con.write(key,new LongWritable(reduce_total));
        }
    }


	public static class MapperNeighborGroup extends Mapper<Object,LongWritable,LongWritable,LongWritable> {
        	//@Override
        	public void map ( Object key, LongWritable value, Context con )
                	        throws IOException, InterruptedException {
	
			LongWritable one = new LongWritable(1);

            		String[] tabsplit = value.toString().split("\t");
			
			
            		long w = Long.parseLong(tabsplit[(tabsplit.length - 1)]);
	            	con.write(new LongWritable(w),one);
        
        }
    }

    public static class ReducerNeighborGroup extends Reducer<LongWritable,LongWritable,LongWritable,LongWritable> {
        //@Override
        public void reduce ( LongWritable key, Iterable<LongWritable> values, Context con )
                           throws IOException, InterruptedException {
            long neighbor_total = 0;
            for (LongWritable v: values) {
                neighbor_total += v.get();
            };
            con.write(key,new LongWritable(neighbor_total));
        }
    }

    public static void main ( String[] args ) throws Exception {

	String TEMP_DIRECTORY = "temp";

	Job j1 = Job.getInstance();
        j1.setJobName("CountNeighborJob");
        j1.setJarByClass(Graph.class);
        j1.setOutputKeyClass(LongWritable.class);
        j1.setOutputValueClass(LongWritable.class);
        j1.setMapOutputKeyClass(LongWritable.class);
        j1.setMapOutputValueClass(LongWritable.class);
        j1.setMapperClass(MapperNeighbor.class);
        j1.setReducerClass(ReducerNeighbor.class);
        j1.setInputFormatClass(TextInputFormat.class);
        j1.setOutputFormatClass(SequenceFileOutputFormat.class);
        FileInputFormat.setInputPaths(j1,new Path(args[0]));
        FileOutputFormat.setOutputPath(j1,new Path(TEMP_DIRECTORY));
        boolean state = j1.waitForCompletion(true);

	if(state){
		Job j2 = Job.getInstance();
        	j2.setJobName("GroupNeighborJob");
        	j2.setJarByClass(Graph.class);
        	j2.setOutputKeyClass(LongWritable.class);
        	j2.setOutputValueClass(LongWritable.class);
        	j2.setMapOutputKeyClass(LongWritable.class);
        	j2.setMapOutputValueClass(LongWritable.class);
        	j2.setMapperClass(MapperNeighborGroup.class);
        	j2.setReducerClass(ReducerNeighborGroup.class);
        	j2.setInputFormatClass(SequenceFileInputFormat.class);
        	j2.setOutputFormatClass(TextOutputFormat.class);
        	FileInputFormat.setInputPaths(j2,new Path(TEMP_DIRECTORY));
        	FileOutputFormat.setOutputPath(j2,new Path(args[1]));
        	state = j2.waitForCompletion(true);
	}

	// deleting temporary directory	
	if(state){
		File temp_dir = new File(TEMP_DIRECTORY);
		if(temp_dir.exists() && temp_dir.isDirectory()){
			File[] files = temp_dir.listFiles();
			if(files != null){
				for(File f : files){
					f.delete();
				}
			}
			temp_dir.delete();
		}
	}
   }
}