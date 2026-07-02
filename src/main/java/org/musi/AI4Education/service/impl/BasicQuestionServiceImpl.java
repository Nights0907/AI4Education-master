package org.musi.AI4Education.service.impl;

import org.musi.AI4Education.domain.entity.BasicQuestion;
import org.musi.AI4Education.domain.entity.ConcreteQuestion;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONException;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.mapper.BasicQuestionMapper;
import org.musi.AI4Education.service.BasicQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.sql.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BasicQuestionServiceImpl extends ServiceImpl<BasicQuestionMapper, BasicQuestion> implements BasicQuestionService {

    @Autowired
    private BasicQuestionMapper basicQuestionMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BasicQuestionService basicQuestionService;


    @Override
    public CommonResponse<String> createBasicQuestion(BasicQuestion basicQuestion) {
        basicQuestionMapper.insert(basicQuestion);
        return CommonResponse.creatForSuccess("添加成功");
    }

    @Override
    public BasicQuestion modifyBasicQuestion(BasicQuestion basicQuestion) {
        UpdateWrapper<BasicQuestion> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("qid", basicQuestion.getQid());
        updateWrapper.set("wrongType",basicQuestion.getWrongType());
        updateWrapper.set("wrongDetails",basicQuestion.getWrongDetails());
        updateWrapper.set("wrongText",basicQuestion.getWrongText());

        update(basicQuestion,updateWrapper);

        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("qid", basicQuestion.getQid());
        BasicQuestion basicQuestion1=basicQuestionMapper.selectOne(wrapper);
        return basicQuestion1;
    }

    @Override
    public List<BasicQuestion> getBasicQuestionList() {
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("sid",StpUtil.getLoginIdAsString());
        return basicQuestionMapper.selectList(wrapper);
    }

    @Override
    public List<BasicQuestion> getBasicQuestionListInOrder() {
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("sid",StpUtil.getLoginIdAsString());
        List<BasicQuestion> basicQuestionList = basicQuestionMapper.selectList(wrapper);
        Collections.sort(basicQuestionList, Comparator.comparing(BasicQuestion::getQid).reversed());
        return basicQuestionList;
    }

    @Override
    public List<BasicQuestion> getMarkedBasicQuestionList() {
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("sid",StpUtil.getLoginIdAsString());
        wrapper.eq("mark",1);
        return basicQuestionMapper.selectList(wrapper);
    }

    @Override
    public BasicQuestion getBasicQuestionByQid(BasicQuestion basicQuestion) {
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("qid",basicQuestion.getQid());
        return basicQuestionMapper.selectOne(wrapper);
    }


    @Override
    public BasicQuestion addQuestionMark(BasicQuestion basicQuestion) {
        UpdateWrapper<BasicQuestion> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("qid", basicQuestion.getQid());
        updateWrapper.set("mark",1);
        update(basicQuestion,updateWrapper);

        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("qid", basicQuestion.getQid());
        BasicQuestion basicQuestion1=basicQuestionMapper.selectOne(wrapper);
        return basicQuestion1;
    }

    @Override
    public BasicQuestion deleteQuestionMark(BasicQuestion basicQuestion) {
        UpdateWrapper<BasicQuestion> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("qid", basicQuestion.getQid());
        updateWrapper.set("mark",0);
        update(basicQuestion,updateWrapper);

        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("qid", basicQuestion.getQid());
        BasicQuestion basicQuestion1=basicQuestionMapper.selectOne(wrapper);
        return basicQuestion1;
    }

    @Override
    public BasicQuestion addQuestionPosition(BasicQuestion basicQuestion) {
        UpdateWrapper<BasicQuestion> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("qid", basicQuestion.getQid());
        updateWrapper.set("position",basicQuestion.getPosition());
        update(basicQuestion,updateWrapper);

        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("qid", basicQuestion.getQid());
        BasicQuestion basicQuestion1=basicQuestionMapper.selectOne(wrapper);
        return basicQuestion1;
    }

    @Override
    public BasicQuestion modifyQuestionPosition(BasicQuestion basicQuestion) {
        UpdateWrapper<BasicQuestion> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("qid", basicQuestion.getQid());
        updateWrapper.set("position",basicQuestion.getPosition());
        update(basicQuestion,updateWrapper);

        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("qid", basicQuestion.getQid());
        BasicQuestion basicQuestion1=basicQuestionMapper.selectOne(wrapper);
        return basicQuestion1;
    }

    @Override
    public BasicQuestion getQuestionPosition(BasicQuestion basicQuestion) {
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("qid", basicQuestion.getQid());
        BasicQuestion basicQuestion1=basicQuestionMapper.selectOne(wrapper);
        return basicQuestion1;
    }

    @Override
    public String getQuestionTextByQid(BasicQuestion basicQuestion) {
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("qid", basicQuestion.getQid());
        System.out.println(basicQuestion.getQid());
        BasicQuestion basicQuestion1=basicQuestionMapper.selectOne(wrapper);
        if (basicQuestion1 == null) {
            return "";
        }
        return cleanQuestionText(basicQuestion1.getQuestionText());
    }

    @Override
    public String getQuestionWrongTextByQid(BasicQuestion basicQuestion) {
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("qid", basicQuestion.getQid());
        System.out.println(basicQuestion.getQid());
        BasicQuestion basicQuestion1=basicQuestionMapper.selectOne(wrapper);
        if (basicQuestion1 == null) {
            return "";
        }
        return cleanQuestionText(basicQuestion1.getWrongText());
    }

    private String cleanQuestionText(String text) {
        if (text == null) {
            return "";
        }
        String regex = "[\\[\\]{}]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll("");
    }

    @Override
    public String getFollowingClassification(String front,String back) {
        if (back.startsWith(front)) {
            // 去掉前缀并获取剩下的字符串
            String remainingString = back.substring(front.length());
            String[] parts = remainingString.split("/");
            if (parts.length > 0) {
                return parts[1];
            }else{
                return "";
            }
        } else{
            return "传入数据有误";
        }
    }

    @Override
    public List<HashMap<String, Object>> getQuestionInfoByPosition(String position) {
        String sid = StpUtil.getLoginIdAsString();
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("sid",sid);
        wrapper.eq("position",position);
        List<BasicQuestion> basicQuestionList = basicQuestionMapper.selectList(wrapper);

        if(!basicQuestionList.isEmpty()){
            //该位置有题
            List<ConcreteQuestion> concreteQuestionList = new ArrayList<>();
            for(BasicQuestion basicQuestion:basicQuestionList){
                Query query = new Query();
                query.addCriteria(Criteria.where("qid").is(basicQuestion.getQid()));
                ConcreteQuestion concreteQuestion = mongoTemplate.findOne(query, ConcreteQuestion.class);
                concreteQuestionList.add(concreteQuestion);
            }
            HashMap<String,Object> tempResult = new HashMap<>();
            tempResult.put("basicQuestionList",basicQuestionList);
            tempResult.put("concreteQuestionList",concreteQuestionList);
            List<HashMap<String,Object>> finalResult = new ArrayList<>();
            finalResult.add(tempResult);
            return finalResult;
        }else{
            //该位置没题，要返回下一级目录
            QueryWrapper<BasicQuestion> queryWrapper = new QueryWrapper<>();
            queryWrapper.like("position", position); // 使用 like 来模糊匹配包含 keyword 的字段值
            List<BasicQuestion> resultList = basicQuestionService.list(queryWrapper);

            System.out.println(resultList);

            List<String> tempResult = new ArrayList<>();
            for(BasicQuestion basicQuestion:resultList){
                System.out.println(basicQuestion.getPosition());
                String temp = getFollowingClassification(position,basicQuestion.getPosition());
                if(!temp.equals(""))
                {
                    boolean contains = false;
                    for (String str : tempResult) {
                        if (str.equals(temp)) {
                            contains = true;
                            break; // 结束循环，无需继续检查
                        }
                    }
                    if(!contains){
                        tempResult.add(temp);
                    }
                }
            }
            HashMap<String,Object> finalResult = new HashMap<>();
            finalResult.put("classification",tempResult);
            List<HashMap<String,Object>> result = new ArrayList<>();
            result.add(finalResult);
            return result;
        }
    }

    @Override
    public JsonNode getPositionsByUid() throws JSONException {
        String sid = StpUtil.getLoginIdAsString();
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("sid",sid);
        List<BasicQuestion> tempResult = basicQuestionMapper.selectList(wrapper);
        List<String> result = new ArrayList<>();

        for(BasicQuestion basicQuestion:tempResult){
            boolean contains = false;
            String position = basicQuestion.getPosition();
            for (String str : result) {
                if (position.equals(str)){
                    contains = true;
                    break; // 结束循环，无需继续检查
                }
            }
            if(!contains&&!position.equals("")){
                result.add(position);
            }
        }
        return convertToJSON(result);
    }

    @Override
    public List<String> getBasicPositionsByUid() {
        String sid = StpUtil.getLoginIdAsString();
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("sid",sid);
        List<BasicQuestion> tempResult = basicQuestionMapper.selectList(wrapper);
        List<String> result = new ArrayList<>();

        for(BasicQuestion basicQuestion:tempResult){
            boolean contains = false;
            String position = basicQuestion.getPosition();
            String[] parts = position.split("/");
            String first = parts.length > 1 ? parts[0] : position;
            for (String str : result) {
                if (first.equals(str)){
                    contains = true;
                    break; // 结束循环，无需继续检查
                }
            }
            if(!contains&&!first.equals("")){
                result.add(first);
            }
        }
        return result;
    }

    @Override
    public void deleteQuestion_PositionsByPosition(String position) {

        String sid = StpUtil.getLoginIdAsString();

        UpdateWrapper<BasicQuestion> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("sid", sid);
        updateWrapper.eq("position", position);

        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setPosition("");

        basicQuestionMapper.update(basicQuestion, updateWrapper);

    }

    @Override
    public JsonNode convertToJSON(List<String> inputList) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        for (String input : inputList) {
            String[] categories = input.split("/");
            ObjectNode currentNode = rootNode;

            for (String category : categories) {
                if (!currentNode.has(category)) {
                    currentNode.putObject(category);
                }
                currentNode = (ObjectNode) currentNode.get(category);
            }
        }

        ObjectNode finalResult = mapper.createObjectNode();
        finalResult.set("folderList", rootNode);

        return finalResult;
    }

    @Override
    public List<String> getQidListBySid(String sid) {
        QueryWrapper<BasicQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("sid",sid);
        List<BasicQuestion> basicQuestionList = basicQuestionMapper.selectList(wrapper);
        List<String> qidList = new ArrayList<>();
        for(BasicQuestion basicQuestion : basicQuestionList){
            qidList.add(basicQuestion.getQid());
        }
        return qidList;
    }


}
