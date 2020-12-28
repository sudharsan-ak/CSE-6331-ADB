import java.io.DataInput;
import java.io.DataOutput; 
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.ReflectionUtils;

public class Multiply {

 	public static class Elem implements Writable {
 		int tag;
 		int index;
 		double value;

 		Elem() {

 		}

 		Elem(int t, int i, double v) {
 			this.tag = t;
 			this.index = i;
 			this.value = v;
 		}

 		public void write(DataOutput out) throws IOException {
 			out.writeInt(tag);
 			out.writeInt(index);
 			out.writeDouble(value);

 		}

 		public void readFields(DataInput in) throws IOException {
 			tag = in.readInt();
 			index = in.readInt();
 			value = in.readDouble();

 		}

 	}

 	public static class Pair implements WritableComparable<Pair> {
 		int i;
 		int j;

 		Pair() {

 		}

 		Pair(int i, int j) {
 			this.i = i;
 			this.j = j;
 		}

 		public void write(DataOutput out) throws IOException {
 			out.writeInt(i);
 			out.writeInt(j);

 		}

 		public void readFields(DataInput in) throws IOException {
 			i = in.readInt();
 			j = in.readInt();

 		}

 		public int compareTo(Pair pair) {
 			if (i > pair.i) {
 				return 1;
 			} else if (i < pair.i) {
 				return -1;
 			} else {
 				if (j > pair.j) {
 					return 1;
 				} else if (j < pair.j) {
 					return -1;
 				}
 			}
 			return 0;

 		}

 		public String toString() {
 			return i + " " + j + " ";

 		}

 	}

 	public static class MMatrix extends Mapper<Object, Text, IntWritable, Elem> {

 		public void map(Object key, Text value, Context con) throws IOException, InterruptedException {
 			Scanner s = new Scanner(value.toString()).useDelimiter(",");
 			int i = s.nextInt();
 			int j = s.nextInt();
 			double val = s.nextDouble();

 			con.write(new IntWritable(j), new Elem(0, i, val));
 			s.close();
 		}
 	}

 	public static class NMatrix extends Mapper<Object, Text, IntWritable, Elem> {

 		public void map(Object key, Text value, Context con) throws IOException, InterruptedException {
 			Scanner s = new Scanner(value.toString()).useDelimiter(",");
 			int j = s.nextInt();
 			int i = s.nextInt();
 			double val = s.nextDouble();
 			con.write(new IntWritable(j), new Elem(1, i, val));
 			s.close();
 		}
 	}

 	public static class MatrixReducer extends Reducer<IntWritable, Elem, Pair, DoubleWritable> {

 		public void reduce(IntWritable key, Iterable<Elem> values, Context con)
 				throws IOException, InterruptedException {
 			ArrayList<Elem> m = new ArrayList<Elem>();
 			ArrayList<Elem> n = new ArrayList<Elem>();

 			Configuration config = con.getConfiguration();
 			for (Elem e : values) {
 				Elem tmp = ReflectionUtils.newInstance(Elem.class, config);
 				ReflectionUtils.copy(config, e, tmp);

 				if (e.tag == 0) {
 					m.add(tmp);
 				} else if (e.tag == 1) {
 					n.add(tmp);
 				}
 			}

 			for (int i = 0; i < m.size(); i++) {
 				for (int k = 0; k < n.size(); k++) {
 					Pair p = new Pair(m.get(i).index, n.get(k).index);
 					Double multiply_Result = m.get(i).value * n.get(k).value;

 					con.write(p, new DoubleWritable(multiply_Result));

 				}
 			}

 		}
 	}

 	public static class ProductMapper extends Mapper<Object, DoubleWritable, Pair, DoubleWritable> {
 		public void map(Object key, Text values, Context con) throws IOException, InterruptedException {
 			Scanner s = new Scanner(values.toString()).useDelimiter("\\s+");

 			int x = s.nextInt();
 			int y = s.nextInt();
 			double val = s.nextDouble();

 			Pair p = new Pair(x, y);
 			con.write(p, new DoubleWritable(val));

 		}

 	}

 	public static class ProductReducer extends Reducer<Pair, DoubleWritable, Pair, DoubleWritable> {
 		public void reduce(Pair key, Iterable<DoubleWritable> values, Context con)
 				throws IOException, InterruptedException {
 			double reduce_total = 0.0;
 			for (DoubleWritable val : values) {
 				reduce_total += val.get();
 			}
 			con.write(key, new DoubleWritable(reduce_total));
 		}
 	}

 	public static boolean deleteDirectory(File directory) {
 		if (directory.exists()) {
 			File[] files = directory.listFiles();
 			if (null != files) {
 				for (int i = 0; i < files.length; i++) {
 					if (files[i].isDirectory()) {
 						deleteDirectory(files[i]);
 					} else {
 						files[i].delete();
 					}
 				}
 			}
 		}
 		return (directory.delete());
 	}

 	public static void main(String[] args) throws Exception {
 		File tmp = new File("Intermediate");
 		deleteDirectory(tmp);
 		Configuration config = new Configuration();
 		Job job = Job.getInstance(config, "Job");
 		job.setJarByClass(Multiply.class);
 		job.setMapOutputKeyClass(IntWritable.class);
 		job.setMapOutputValueClass(Elem.class);

 		MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, MMatrix.class);
 		MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, NMatrix.class);

 		job.setReducerClass(MatrixReducer.class);
 		job.setOutputKeyClass(Pair.class);
 		job.setOutputValueClass(DoubleWritable.class);

 		job.setOutputFormatClass(SequenceFileOutputFormat.class);
 		SequenceFileOutputFormat.setOutputPath(job, new Path(args[2]));

 		job.waitForCompletion(true);

 		Job job2 = Job.getInstance(config, "Job2");
 		job2.setJarByClass(Multiply.class);
 		job2.setMapperClass(ProductMapper.class);
 		job2.setMapOutputKeyClass(Pair.class);
 		job2.setMapOutputValueClass(DoubleWritable.class);

 		job2.setInputFormatClass(SequenceFileInputFormat.class);
 		SequenceFileInputFormat.addInputPath(job2, new Path(args[2]));

 		job2.setReducerClass(ProductReducer.class);
 		job2.setOutputFormatClass(TextOutputFormat.class);

 		FileOutputFormat.setOutputPath(job2,  new Path(args[3]));

 		job2.waitForCompletion(true);
 	}

 }