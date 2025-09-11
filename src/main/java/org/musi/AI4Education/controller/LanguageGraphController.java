package org.musi.AI4Education.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.service.GraphService;
import org.musi.AI4Education.service.impl.GraphServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/graph")
public class LanguageGraphController {

    @Autowired
    private GraphService graphService;

    //获取知识图谱的JSON数据
    @GetMapping("/record")
    public CommonResponse<String> record(){
        System.out.println("调用record...");
        //获取相关的JSON数据
        String generateData = graphService.getRecord();
        System.out.println("generateData:"+generateData);
        if (generateData.equals(""))
            return CommonResponse.creatForError(0,"获取信息错误！");
        else
            return CommonResponse.creatForSuccess(generateData);
    }
    @PostMapping("/parseJson")
    public CommonResponse<Object> parseJson(@RequestBody String json){
        System.out.println("调用parseJson..."+json);
        System.out.println(json);
        Map<String, Object> jsonData = graphService.getParseJson(json);

        //生成jsonObject
        JSONObject jsonObject = new JSONObject(jsonData);
        System.out.println(jsonObject);
        if (jsonData != null) {
            return CommonResponse.creatForSuccess(jsonObject);
        } else {
            return CommonResponse.creatForSuccess("ERROR");
        }
    }
}
