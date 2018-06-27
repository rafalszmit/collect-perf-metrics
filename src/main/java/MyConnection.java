import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyConnection {

    static String CONFIG_FILE = "config.properties";

    public static void main(String[] args) throws Exception {

        MyConnection http = new MyConnection();

        String url = getUrl(CONFIG_FILE);
        List<String> postParameters = getUrlParameters(CONFIG_FILE);

        postParameters.forEach(postParameter -> {

            try {
                String metricData = http.sendPost(url, postParameter);
                Map<String, List<Double>> measurementsPerSite = parseMeasurementsPerSite(metricData);
                Map<String, List<Double>> statsPerSite = calculateStatsPerSite(measurementsPerSite);
                String output = prepareOutput(postParameter, statsPerSite);
                saveToFile(output, "Dane.txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

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

    private static Map<String, List<Double>> parseMeasurementsPerSite(String grafData) {

        JSONArray response = new JSONArray(grafData);
        Map<String, List<Double>> outputList = new HashMap<>();
        List<Double> valuePerSite = new ArrayList<Double>();

        for (Object obj : response) {
            String targetName = ((JSONObject) obj).getString("target");
            JSONArray datapoints = ((JSONObject) obj).getJSONArray("datapoints");
            Stream<Double> pointValuesStream = datapoints.toList().stream()
                    .filter(el -> ((ArrayList) el).get(0) != null)
                    .map(el -> (Double.parseDouble(((ArrayList) el).get(0).toString())));
            valuePerSite = pointValuesStream.collect(Collectors.toList());
            outputList.put(targetName, valuePerSite);
        }

        return outputList;
    }

    private static String prepareOutput(String header, Map<String, List<Double>> sitesWithStats) {
        String LF = "\n";
        StringBuffer output = new StringBuffer();
        output.append(header + LF);
        output.append("site,min,max,mean,stdev" + LF);
        for (Map.Entry<String, List<Double>> entry : sitesWithStats.entrySet()) {
            String site = entry.getKey();
            List<Double> stats = entry.getValue();
            Stream<Double> statsStream = stats.stream();
            output.append(site + "," + statsStream.map(x -> ((Double) x).toString()).collect(Collectors.joining(",")) + LF);
        }
        output.append(LF);
        return output.toString();
    }

    private static void saveToFile(String output, String fileName) {

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
            dataWriter.println(output);
            dataWriter.close();

        } catch (Exception e) {
            System.out.println("File Error");
        }
    }

    private static Map<String, List<Double>> calculateStatsPerSite(Map<String, List<Double>> inputedMap) {

        DescriptiveStatistics stats = new DescriptiveStatistics();

        Map<String, List<Double>> result = inputedMap.entrySet().stream().collect(
                Collectors.toMap(x -> x.getKey(), x -> {
                            List<Double> value = x.getValue();
                            List<Double> filteredStatsStream = value.stream().filter(val -> val < 10000).collect(Collectors.toList());
                            filteredStatsStream.forEach(val -> stats.addValue(val));
                            return Arrays.asList(stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation());
                        }
                ));

        return result;
    }

    private static String getUrl(String fileName) {

        InputStream input = null;
        String url = null;
        try {
            input = new FileInputStream(fileName);
            Properties prop = new Properties();
            prop.load(input);
            String scheme = prop.getProperty("scheme");
            String domain = prop.getProperty("domain");
            String port = prop.getProperty("port");
            String path = prop.getProperty("path");
            url = scheme + "://" + domain + ":" + port + "/" + path;
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

        return url;
    }

    private static List<String> getUrlParameters(String fileName) {

        InputStream input = null;
        List<String> postParameters = new LinkedList<String>();

        try {
            input = new FileInputStream(fileName);
            Properties prop = new Properties();
            prop.load(input);
            String from = prop.getProperty("from");
            String until = prop.getProperty("until");
            String format = prop.getProperty("format");

            int amoutOfTargets = Integer.parseInt(prop.getProperty("amoutOfTargets"));
            for (int i = 1; i < amoutOfTargets + 1; i++) {
                String target = prop.getProperty("target." + i);
                target = URLEncoder.encode(target, "UTF-8");
                postParameters.add("target=" + target + "&from=" + from + "&until=" + until + "&format=" + format);
            }
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

        return postParameters;
    }

}
