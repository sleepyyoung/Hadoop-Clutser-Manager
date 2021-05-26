package com.philpy.utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

@Component
public class WordCount {
    static Configuration conf = new Configuration();

    static {
        conf.setStrings("result", "");
        conf.set("fs.default.name", "hdfs://192.168.213.128:8020");
    }

    public static class Map extends Mapper<Object, Text, Text, IntWritable> {
        private final IntWritable one = new IntWritable(1);

        @Override
        protected void map(Object key, Text value, Mapper<Object, Text, Text, IntWritable>.Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                context.write(new Text(itr.nextToken()), one);
            }
        }
    }

    static public class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Reducer<Text, IntWritable, Text, IntWritable>.Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            IntWritable writable = new IntWritable(sum);
            context.write(key, writable);
            String[] results = conf.getStrings("result");
            int length;
            if (results == null) {
                length = 0;
                String[] tmp = new String[length + 1];
                tmp[length] = "(" + key + "," + writable + ")";
                conf.setStrings("result", tmp);
            } else {
                length = results.length;
                String[] tmp = Arrays.copyOf(results, length + 1);
                tmp[length] = "(" + key + "," + writable + ")";
                conf.setStrings("result", tmp);
            }
        }
    }

    public String getWordCount(String inputFile, String outputDir) throws IOException, ClassNotFoundException, InterruptedException {
        conf.setStrings("result", "");
        Job job = Job.getInstance(conf);
        job.setJarByClass(WordCount.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);
        job.setMapOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(outputDir));
        job.waitForCompletion(true);
        return Arrays.toString(conf.getStrings("result"));
    }
}
