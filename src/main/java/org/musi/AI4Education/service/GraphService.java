package org.musi.AI4Education.service;

import java.util.Map;

public interface GraphService {
    //获取生成知识图谱所需要的JSON数据
    public String getRecord();
    //对数据进行处理
    public Map<String, Object> getParseJson(String json);
}
