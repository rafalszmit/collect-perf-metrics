import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.*;

public class MyConnection {

	private String sendPost(String url, String urlParameters) throws Exception {

		URL u = new URL(url);

		HttpURLConnection con = (HttpURLConnection) u.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");

		byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
		int postDataLength = postData.length;

		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("charset", "utf-8");
		con.setRequestProperty("Content-Length", Integer.toString(postDataLength));

		try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
			wr.write(postData);
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		String grafData = sb.toString();
		return grafData;
	}

	private static Map<Long, ArrayList<Float>> parsePerTimestamps(String grafData) {

		JSONArray response = new JSONArray(grafData);
		JSONArray datapoints;
		Long timeStamp;
		Float valueData;
		String sData;
		ArrayList<Float> valueList = new ArrayList<Float>();

		Map<Long, ArrayList<Float>> stampsAndData = new HashMap<>();

		for (Object obj : response) {
			datapoints = ((JSONObject) obj).getJSONArray("datapoints");
			// targetName = ((JSONObject) obj).getString("target");

			for (Object datapointsEntry : (JSONArray) datapoints) {
				timeStamp = Long.parseLong(((JSONArray) datapointsEntry).get(1).toString());
				sData = ((JSONArray) datapointsEntry).get(0).toString();
				if (!sData.equals("null")) {
					valueData = Float.parseFloat(sData);
				} else {
					valueData = null;
				}
				if (!stampsAndData.containsKey(timeStamp)) {
					valueList = new ArrayList<Float>();
					valueList.add(valueData);
					stampsAndData.put(timeStamp, valueList);
				} else {
					stampsAndData.get(timeStamp).add(valueData);
				}
			}
		}
		return stampsAndData;
	}

	private static Map<String, List<Double>> parsePerSite(String grafData) {

		JSONArray response = new JSONArray(grafData);
		JSONArray datapoints;
		String targetName;
		Map<String, List<Double>> outputList = new HashMap<>();
		List<Double> valuePerSite = new ArrayList<Double>();

		for (Object obj : response) {
			targetName = ((JSONObject) obj).getString("target");
			datapoints = ((JSONObject) obj).getJSONArray("datapoints");

			for (Object datapointsEntry : (JSONArray) datapoints) {
				Stream<Double> pointValuesStream = datapoints.toList().stream()
						.filter(el -> ((ArrayList) el).get(0) != null)
						.map(el -> (Double.parseDouble(((ArrayList) el).get(0).toString())));
				valuePerSite = pointValuesStream.collect(Collectors.toList());
			}
			// Stream<Double> pointValuesStream = ((JSONObject)
			// response.get(0)).getJSONArray("datapoints").toList().stream()
			// .filter(el -> ((ArrayList) el).get(0) != null)
			// .map(el -> (Double.parseDouble(((ArrayList) el).get(0).toString())));
			//
			// valuePerSite = pointValuesStream.collect(Collectors.toList());
			outputList.put(targetName, valuePerSite);
		}
		return outputList;
	}

	private static void printMap(Map<String, List<Double>> inputedMap) {
		for (Map.Entry<String, List<Double>> entry : inputedMap.entrySet()) {
			String key = entry.getKey();
			List<Double> value = entry.getValue();
			System.out.println("------------------" + key + "-------------------");
			//System.out.println("Min   Max   Mean   Standard Deviation");

			// String content;
			Stream<Double> stream2 = value.stream();
			System.out.println("min\n" + stream2.map(x -> {
				//if (x != null) {
					return ((Double) x).toString();
//				} else {
//					return "".toString();
//				}
			}).collect(Collectors.joining(", ")));
		}
	}

	private static void saveToTxt(Map<String, List<Double>> inputedMap, String fileName) {
		fileName = fileName + ".txt";
		try {
			FileWriter dataFile;
			PrintWriter dataWriter;
			File userDataFile = new File(fileName);
			if (userDataFile.exists()) {
				dataFile = new FileWriter(fileName, true);
				System.out.println("Added to existing file: " + fileName);
			} else {
				dataFile = new FileWriter(fileName);
				System.out.println("Created new file: " + fileName);
			}
			dataWriter = new PrintWriter(dataFile);
			for (Map.Entry<String, List<Double>> entry : inputedMap.entrySet()) {
				String key = entry.getKey();
				List<Double> value = entry.getValue();
				Stream<Double> stream2 = value.stream();
				dataWriter.println(key+" "+stream2.map(x -> {
					//if (x != null) {
						return ((Double) x).toString();
//					} else {
//						return "".toString();
//					}
				}).collect(Collectors.joining(", ")));

			}
			dataWriter.close();

		} catch (Exception e) {
			System.out.println("File Error");
		}
	}

	private static Map<Long, ArrayList<Float>> filterEmptyStamps(Map<Long, ArrayList<Float>> inputedMap) {
		Map<Long, ArrayList<Float>> outputData = new HashMap<>();
		for (Map.Entry<Long, ArrayList<Float>> entry : inputedMap.entrySet()) {
			Long key = entry.getKey();
			ArrayList<Float> value = entry.getValue();
			int counter = 0;

			for (int i = 0; i < value.size(); i++) {
				if (value.get(i) == null) {
					counter++;
				}
			}
			if (counter < value.size())
				outputData.put(key, value);
		}
		// printMap(outputData);
		return outputData;
	}

	private static Map<String, List<Double>> stats(Map<String, List<Double>> inputedMap) {

		DescriptiveStatistics stats = new DescriptiveStatistics();
		Double min;
		Double max;
		Double mean;
		Double std;
		Map<String, List<Double>> result = new HashMap<>();
		List<Double> calcedValues;;
		
		for (Map.Entry<String, List<Double>> entry : inputedMap.entrySet()) {
			String key = entry.getKey();
			List<Double> value = entry.getValue();
//			System.out.println("------------------" + key + "-------------------");
//			System.out.println("values size: " + value.size());
			for (int i = 0; i < value.size(); i++) {
				if (value.get(i) < 10000) {
					//System.out.print(value.get(i) + " ");
					stats.addValue(value.get(i));
				}
			}
				
			calcedValues = new ArrayList<Double>();
			min = stats.getMin();
			max = stats.getMax();
			mean = stats.getMean();
			std = stats.getStandardDeviation();
			calcedValues.add(min);
			calcedValues.add(max);
			calcedValues.add(mean);
			calcedValues.add(std);
//			System.out.println("\n");
//			System.out.println("Min: " + min);
//			System.out.println("Max: " + max);
//			System.out.println("Mean: " + mean);
//			System.out.println("Standard Deviation: " + std);
//			System.out.println("calcedValues: "+calcedValues);
			result.put(key, calcedValues);
		}
		return result;
	}
		
	private static List<Double> statsImproved(Map<String, List<Double>> inputedMap) {

		DescriptiveStatistics stats = new DescriptiveStatistics();
		//List<Double> output;
		Map<String, List<Double>> output2;
		Stream<List<Double>> result = inputedMap.entrySet().stream().map(x -> {
			String key = x.getKey();
			List<Double> value = x.getValue();
			List<Double> filteredStatsStream = value.stream().filter(val -> val < 10000).collect(Collectors.toList());
			filteredStatsStream.forEach(val -> stats.addValue(val));
			return Arrays.asList(stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation());
		});	

		List<Double> output = result.flatMap(List::stream).collect(Collectors.toList());
		//output2.put(key, output);
		return output;
	}
	
	private static Map<String, List<Double>> statsImproved2(Map<String, List<Double>> inputedMap) {

		DescriptiveStatistics stats = new DescriptiveStatistics();
		//List<Double> output;
		Map<String, List<Double>> output2 =new HashMap<>();;
//		Stream<Map<String, List<Double>>> result = inputedMap.entrySet().stream().map(x -> {
//			String key = x.getKey();
//			List<Double> value = x.getValue();
//			List<Double> filteredStatsStream = value.stream().filter(val -> val < 10000).collect(Collectors.toList());
//			filteredStatsStream.forEach(val -> stats.addValue(val));
//			//return Arrays.asList(key, Double.toString(stats.getMin()), Double.toString(stats.getMax()), Double.toString(stats.getMean()), Double.toString(stats.getStandardDeviation()));
//			return Collectors.toMap(key,Arrays.asList(stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation()));
//		});	
//		
		
		Map<String, List<Double>> result = inputedMap.entrySet().stream().collect(
				Collectors.toMap( x -> x.getKey(), x -> {
					List<Double> value = x.getValue();
					List<Double> filteredStatsStream = value.stream().filter(val -> val < 10000).collect(Collectors.toList());
					filteredStatsStream.forEach(val -> stats.addValue(val));
					return Arrays.asList(stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation());
				}
				));	
		
		//List<Double> output = result.flatMap(List::stream).collect(Collectors.toList());
		
		
		
//		List<Double> output = result
//				.skip(1)
//				.forEach(val->Double.parseDouble(val.));
//
//		result
//		.skip(1)
//		.forEach(val->output2.put(result.findFirst().get().toString(),val.forEach(val2->Double.parseDouble(val2))));
		
		return result;
	}

	private static Map<String, ArrayList<String>> getUrlParameters(String fileName) {
		Properties prop = new Properties();
		Map<String, ArrayList<String>> outputData = new HashMap<>();
		InputStream input = null;
		String scheme;
		String domain;
		String port;
		String path;
		String url;
		ArrayList<String> finalTargets = new ArrayList<String>();
		String target;
		String from;
		String until;
		String format;
		int amoutOfTargets;
		try {
			input = new FileInputStream("config.properties.txt");
			prop.load(input);
			scheme = prop.getProperty("scheme");
			domain = prop.getProperty("domain");
			port = prop.getProperty("port");
			path = prop.getProperty("path");
			url = scheme + "://" + domain + ":" + port + "/" + path;

			from = prop.getProperty("from");
			until = prop.getProperty("until");
			format = prop.getProperty("format");
			amoutOfTargets=Integer.parseInt(prop.getProperty("amoutOfTargets"));
			for (int i = 1; i < amoutOfTargets+1; i++) {
				target = prop.getProperty("target." + i);
				target = URLEncoder.encode(target, "UTF-8");
				finalTargets.add("target=" + target + "&from=" + from + "&until=" + until + "&format=" + format);
			}
			outputData.put(url, finalTargets);

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return outputData;
	}

	public static void main(String[] args) throws Exception {
//		1. responseData = http.sendPost(key, value.get(i));
//		2. performanceData2 = parsePerSite(responseData);
//		3. stats(performanceData2);
		MyConnection http = new MyConnection();
		Map<String, ArrayList<String>> urlAndTargets;
		
		urlAndTargets = getUrlParameters("config.properties");
		System.out.println("urlAndTargets\n"+urlAndTargets);
		
		urlAndTargets.entrySet().stream().forEach(x -> {
			String key = x.getKey();
			List<String> value = x.getValue();
			List<String> filteredStatsStream = value.stream().collect(Collectors.toList());
			filteredStatsStream.forEach(val -> {
				try {
					//stats(parsePerSite(http.sendPost(key, val)));
					//printMap(stats(parsePerSite(http.sendPost(key, val))));
					printMap(statsImproved2(parsePerSite(http.sendPost(key, val))));
					//saveToTxt(stats(parsePerSite(http.sendPost(key, val))),"Dane");
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		});			
	}
}
