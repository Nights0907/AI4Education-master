package org.musi.AI4Education.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.musi.AI4Education.service.GraphService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


@Service
public class GraphServiceImpl implements GraphService {
    @Override
    public String getRecord() {
        JSONArray result = getRecords();

        return result.toString();
    }

    //处理传入的数据
    @Override
    public Map<String, Object> getParseJson(String json) {
        try {
            String response_data = json;

            //对字符串进行处理
            response_data = response_data.replace("'", "\"");
            JSONObject jsonObject = JSON.parseObject(response_data);
            String jsonPart = jsonObject.getString("json");

            //传入并输出
            Map<String, Object> result = parseJson(jsonPart);
            System.out.println(result);
            return result;
        }catch (Exception e){
            return null;
        }
    }

    //获取已存的Json
    public JSONArray getRecords() {
        JSONArray jsonArray = new JSONArray();
        String jsonFilePath = "D:\\json_generate\\output0.json";

        try (FileReader fileReader = new FileReader(jsonFilePath)) {
            // 读取 JSON 文件内容
            StringBuilder stringBuilder = new StringBuilder();
            int character;
            while ((character = fileReader.read()) != -1) {
                stringBuilder.append((char) character);
            }

            // 将文件内容转换为 JSON 数组
            jsonArray = new JSONArray(stringBuilder.toString());

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return jsonArray;
    }

    //处理Json
    public Map<String, Object> parseJson(String response_data) throws Exception {
        List<Map<String, Object>> json_data = (List<Map<String, Object>>) JSON.parse(response_data);

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        List<Integer> node_set = new ArrayList<>();
        Set<String> label_set = new HashSet<>();
        Set<String> link_type_set = new HashSet<>();

        for (Map<String, Object> item : json_data) {
            Map<String, Object> start_node = (Map<String, Object>) item.get("start");
            Map<String, Object> end_node = (Map<String, Object>) item.get("end");
            String relation = (String) item.get("relation");

            // 处理起始节点
            int start_id = (int) start_node.get("id");
            String start_label = ((List<String>) start_node.get("labels")).get(0);
            if (!node_set.contains(start_id)) {
                node_set.add(start_id);
                Map<String, Object> startNodeMap = new HashMap<>();
                startNodeMap.put("id", start_id);
                startNodeMap.put("label", start_label);
                startNodeMap.put("properties", start_node.get("properties"));
                nodes.add(startNodeMap);
                label_set.add(start_label);
            }

            // 处理结束节点
            int end_id = (int) end_node.get("id");
            String end_label = ((List<String>) end_node.get("labels")).get(0);
            if (!node_set.contains(end_id)) {
                node_set.add(end_id);
                Map<String, Object> endNodeMap = new HashMap<>();
                endNodeMap.put("id", end_id);
                endNodeMap.put("label", end_label);
                endNodeMap.put("properties", end_node.get("properties"));
                nodes.add(endNodeMap);
                label_set.add(end_label);
            }

            // 处理关系
            Map<String, Object> link = new HashMap<>();
            link.put("source", start_id);
            link.put("target", end_id);
            link.put("type", relation);
            link.put("properties", new HashMap<>());  // 如果有关系的属性，可以从原始数据中获取
            links.add(link);
            link_type_set.add(relation);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("nodes", nodes);
        data.put("links", links);
        data.put("labels", new ArrayList<>(label_set));
        data.put("linkTypes", new ArrayList<>(link_type_set));

        return data;
    }

    //调用python接口
    private static String usePythonFunction(String pythonFunPath, String... args){
        String answer = "";
        try {
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add(pythonFunPath);
            command.addAll(Arrays.asList(args));

            ProcessBuilder pb;
            pb = new ProcessBuilder(command);
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(),"gb2312"));
            String line;
            while ((line = in.readLine()) != null) {
                answer += line;
            }
            in.close();
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return answer;
    }
}
