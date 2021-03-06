package com.netease.weblogOffline.statistics.tokaola;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.slf4j.LoggerFactory;

import com.netease.jurassic.hadoopjob.MRJob;
import com.netease.jurassic.hadoopjob.data.JobConstant;
import com.netease.weblogCommon.utils.JsonUtils;
import com.netease.weblogCommon.utils.TextUtils;
import com.netease.weblogOffline.common.DirConstant;
import com.netease.weblogOffline.utils.HadoopUtils;


/**
 * ownurlpvuv
 *
 */

public class OwnUrlPVUVMR extends MRJob {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(OwnUrlPVUVMR.class);

	@Override
	public boolean init(String date) {
        //输入列表
        inputList.add(DirConstant.WEBLOG_STATISTICS_DIR+"result_other/zyownurlpvuv/" + date);
        //输出列表
        outputList.add(DirConstant.WEBLOG_STATISTICS_DIR+"result_other/groupbyownpvuv/" + date);
 

		return true;
	}

    
    @Override
    public int run(String[] args) throws Exception {
    	int jobState = JobConstant.SUCCESSFUL;
        
    	Job job = HadoopUtils.getJob(this.getClass(), this.getClass().getName());


    	MultipleInputs.addInputPath(job, new Path(inputList.get(0)), SequenceFileInputFormat.class, ZyOwmUrlMapper.class);
        
        //mapper
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        
        
        //reducer
        job.setReducerClass(PvUvReducer.class);
        job.setNumReduceTasks(8);
        FileOutputFormat.setOutputPath(job, new Path(outputList.get(0)));
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);
        
        if(!job.waitForCompletion(true)){
            jobState = JobConstant.FAILED;
        }
        
        return jobState;
    }
    
    public static class ZyOwmUrlMapper extends Mapper<Text, NullWritable, Text, Text> {
    	
    	
    	private Text outputkey = new Text();
    	private Text outputValue = new Text();
    	private Map<String, String> map = new HashMap<String, String>();

    	
        @Override
        public void map(Text key, NullWritable value, Context context) throws IOException, InterruptedException {
        	map.clear();
        	try {
		        String[] keystr = key.toString().split(",");
		        if (keystr.length>3){
		        	for (int i =0;i<keystr.length-2;i++){
		        		keystr[0]+=keystr[i];
		        	}
		        	keystr[0]=keystr[0].replace(",", "");
		        	keystr[1]=keystr[keystr.length-2];
		        	keystr[2]=keystr[keystr.length-1];
		        }
				String url = keystr[0];
				
				
		

		        	String[] str =url.split("\\?");
		        	
					String[] urlParams = str[1].split("#")[0].split("&");
					Map<String, String> result = new HashMap<String, String>();
					
					for (String s : urlParams){
						
							int first = s.indexOf("=");
							if (first != -1){
								result.put(s.substring(0, first).trim(), TextUtils.notNullStr(s.substring(first + 1, s.length())));
							}
			       	}
					    url= str[0]+"?own="+result.get("own");
						
						map.put("pv", keystr[1]);
						map.put("uv", keystr[2]);
						outputkey.set(url);
						outputValue.set(JsonUtils.toJson(map));
						context.write(outputkey, outputValue);
			} catch (Exception e) {
				context.getCounter("LogMapper", "mapException").increment(1);
			}
        }
        
    }
    

    public static class PvUvReducer extends Reducer<Text, Text, Text, NullWritable> {
    	
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
    
        	int pv = 0;
        	int uv = 0;
        	
        	for(Text val : values){
        		Map<String,String> map = JsonUtils.json2Map(val.toString());
        		pv += Integer.valueOf(map.get("pv"));
        		uv += Integer.valueOf(map.get("uv"));
        	}
			
			context.write(new Text(key.toString()+","+pv+","+uv), NullWritable.get());
        }
    }
    	  
	  
}


