package org.musi.AI4Education.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.config.Wen_XinConfig;
import org.musi.AI4Education.domain.*;
import org.musi.AI4Education.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.sql.Date;

import static org.musi.AI4Education.config.OCRConfig.latexOcr;

@RestController
@RequestMapping("/student")
public class QuestionController {

    @Autowired
    private AbilityPointService abilityPointService;
    @Autowired
    private BasicQuestionService basicQuestionService;
    @Autowired
    private ConcreteQuestionService concreteQuestionService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private OSSService ossService;
    @Autowired
    private StudentProfileService studentProfileService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private ChatGPTService chatGPTservice;


    @PostMapping("/bigModel")
    public CommonResponse<Map<String, Object>> createQuestion(MultipartFile question) throws Exception {

            if (StpUtil.isLogin()) {

            //将图片传输到阿里云OSS，并返回存储的URL
            String url = ossService.uploadFile(question);

            //调用图像识别OCR，返回包含题干信息的Latex字符串，获得并打印题干文本信息
            String content = latexOcr(question);

            JSONObject ocrObject = new JSONObject(content);
            JSONObject res = ocrObject.getJSONObject("res");
            String question_text = res.getString("latex");
            System.out.println("题干文本信息: "+question_text);

            String answerAndExplanationAndKnoeledges=concreteQuestionService.useWenxinStreamTransformToGetAnswerAndExplanationAndKnowledge(question_text);
            String steps=concreteQuestionService.useWenxinStreamTransformToGetSteps(question_text);

            System.out.println(answerAndExplanationAndKnoeledges);
            System.out.println(steps);

            List<String> result = concreteQuestionService.splitAnswerAndExplanation(answerAndExplanationAndKnoeledges);
            String answer = result.get(0);
            String explanation = result.get(1);
            String knowledgesList = result.get(2);
            List<String> knowledges= concreteQuestionService.splitKnowledges(knowledgesList);


            //存储错题概要信息
            BasicQuestion basicQuestion = new BasicQuestion();

            String sid =StpUtil.getLoginIdAsString();
            basicQuestion.setSid(sid);

            String qid = String.valueOf(System.currentTimeMillis());
            basicQuestion.setQid(qid);

            Date currentDate = new Date(System.currentTimeMillis());
            basicQuestion.setDate(currentDate);

            //这一部分要融入大模型
            basicQuestion.setSubject("数学");
            basicQuestion.setQuestionText(question_text);
            basicQuestion.setQuestionType("选择题");
            basicQuestion.setMark(0);
            basicQuestion.setPath(url);
            basicQuestionService.createBasicQuestion(basicQuestion);


            //存储错题详细信息
            ConcreteQuestion concreteQuestion = new ConcreteQuestion();

            concreteQuestion.setQid(qid);
            //易错点
            concreteQuestion.setInspiration("负负得正");
            //题目文本
            concreteQuestion.setQuestionText(question_text);
            //存储题目答案
            concreteQuestion.setQuestionAnswer(answer);
            //存储题目解析
            concreteQuestion.setQuestionAnalysis(explanation);
            //存储题目知识点
            concreteQuestion.setKnowledges(knowledges);
            //存储题目笔记：暂时为空
            concreteQuestion.setNote("");


            //存储解题步骤
            ArrayList<QuestionStep> questionStepList = concreteQuestionService.createQuestionSteps(steps);
            concreteQuestion.setQuestionSteps(questionStepList);
            concreteQuestionService.createConcreteQuestion(concreteQuestion);

            //存储历史记录信息
            History history = new History();
            String hid = String.valueOf(System.currentTimeMillis());
            history.setSid(sid);
            history.setHid(hid);
            history.setQid(qid);
            history.setTime(currentDate);
            history.setType("计算错误");
            history.setDetails("正负值错误");

            historyService.createHistory(history);

            //存储学生档案

            Map<String, String> knowledge_ability = concreteQuestionService.useWenxinStreamTransformToAnalyseAbilityByKnowledge(knowledges);

            StudentProfile studentProfile = studentProfileService.getStudentProfileBySid(sid);

            if(studentProfile == null){

                studentProfile = new StudentProfile();

                studentProfile.setStid(String.valueOf(System.currentTimeMillis()));
                studentProfile.setSid(sid);
                studentProfile.setSubject("数学");

                List<KnowledgePoint> knowledgePointList = new ArrayList<>();
                List<AbilityPoint> abilityPointList = new ArrayList<>();

                for (Map.Entry<String, String> entry : knowledge_ability.entrySet()) {
                    String knowledge = entry.getKey();

                    //设置知识点
                    KnowledgePoint knowledgePoint = new KnowledgePoint();
                    String kid = String.valueOf(System.currentTimeMillis());
                    knowledgePoint.setKid(kid);
                    List<String> qid_list = new ArrayList<>();
                    qid_list.add(qid);
                    knowledgePoint.setQid(qid_list);
                    knowledgePoint.setType(knowledge);
                    knowledgePoint.setTimes(1);
                    knowledgePoint.setLatestDate(currentDate);
                    knowledgePointList.add(knowledgePoint);
                    List<String> abilitys = abilityPointService.parseStringToList(entry.getValue());
                    for(String ability:abilitys){
                        AbilityPoint abilityPoint = new AbilityPoint();
                        abilityPoint.setAid(String.valueOf(System.currentTimeMillis()));
                        abilityPoint.setType(ability);
                        List<String> qid_list_1 = new ArrayList<>();
                        qid_list_1.add(qid);
                        abilityPoint.setQid(qid_list_1);
                        abilityPoint.setKid(kid);
                        abilityPoint.setTimes(1);
                        abilityPoint.setLatestDate(currentDate);
                        abilityPointList.add(abilityPoint);
                    }
                    System.out.println("knowledge: " + knowledge + ", abilitys: " + abilitys);
                }

                studentProfile.setKnowledgePointList(knowledgePointList);
                studentProfile.setAbilityPointList(abilityPointList);

                //新建学生档案
                studentProfileService.createStudentProfile(studentProfile);
                studentProfileService.useWenxinUpdateCharacterPointByQidAndSid(studentProfile);

            }else{

                List<KnowledgePoint> knowledgePointList = studentProfile.getKnowledgePointList();
                List<AbilityPoint> abilityPointList = studentProfile.getAbilityPointList();
                boolean knowledge_is_already_in_list;

                for (Map.Entry<String, String> entry : knowledge_ability.entrySet()) {
                    String knowledge = entry.getKey();
                    String kid = String.valueOf(System.currentTimeMillis());
                    knowledge_is_already_in_list = false;
                    for (KnowledgePoint knowledgePoint : knowledgePointList) {
                        if (knowledgePoint.getType().equals(knowledge)) {
                            knowledgePoint.getQid().add(qid);
                            knowledgePoint.setTimes(knowledgePoint.getTimes() + 1);
                            knowledgePoint.setLatestDate(currentDate);
                            knowledge_is_already_in_list = true;
                        }
                    }
                    if(!knowledge_is_already_in_list){
                        KnowledgePoint knowledgePoint = new KnowledgePoint();
                        knowledgePoint.setKid(kid);
                        List<String> qid_list = new ArrayList<>();
                        qid_list.add(qid);
                        knowledgePoint.setQid(qid_list);
                        knowledgePoint.setType(knowledge);
                        knowledgePoint.setTimes(1);
                        knowledgePoint.setLatestDate(currentDate);
                        knowledgePointList.add(knowledgePoint);
                    }

                    List<String> abilitys = abilityPointService.parseStringToList(entry.getValue());
                    boolean ability_is_already_in_list;

                    for(String ability:abilitys){
                        ability_is_already_in_list = false;
                        for (AbilityPoint abilityPoint : abilityPointList) {
                            if (abilityPoint.getType().equals(ability)) {
                                abilityPoint.getQid().add(qid);
                                abilityPoint.setTimes(abilityPoint.getTimes()+1);
                                abilityPoint.setLatestDate(currentDate);
                                ability_is_already_in_list = true;
                            }
                        }
                        if(!ability_is_already_in_list){
                            AbilityPoint abilityPoint = new AbilityPoint();
                            abilityPoint.setAid(String.valueOf(System.currentTimeMillis()));
                            abilityPoint.setType(ability);
                            List<String> qid_list_1 = new ArrayList<>();
                            qid_list_1.add(qid);
                            abilityPoint.setQid(qid_list_1);
                            abilityPoint.setKid(kid);
                            abilityPoint.setTimes(1);
                            abilityPoint.setLatestDate(currentDate);
                            abilityPointList.add(abilityPoint);
                        }
                    }
                    System.out.println("knowledge: " + knowledge + ", abilitys: " + abilitys);
                }

                studentProfile.setKnowledgePointList(knowledgePointList);
                studentProfile.setAbilityPointList(abilityPointList);

                //更新学生档案
                studentProfileService.updateStudentProfile(studentProfile);
                //更新学生档案中的 个性偏好 部分
                studentProfileService.useWenxinUpdateCharacterPointByQidAndSid(studentProfile);
            }

            //设置返回信息
            Map<String, Object> data = new HashMap<>();
            data.put("basicQuestion",basicQuestion);
            data.put("concreteQuestion",concreteQuestion);

            return CommonResponse.creatForSuccess(data);
        } else {
            // 令牌无效或解码错误
            return CommonResponse.creatForError("请先登录");
        }
    }
    @PostMapping("/bigModel/wrongAnswer")
    public CommonResponse<String> analyseWrongType(MultipartFile wrongAnswer,@RequestParam String qid)throws Exception{
        if (StpUtil.isLogin()) {

            System.out.println("this is qid:"+qid);
            //调用图像识别OCR，返回包含题干信息的Latex字符串，获得并打印题干文本信息
            String wrongAnswerInJSONForm = latexOcr(wrongAnswer);

            JSONObject ocrObject0 = new JSONObject(wrongAnswerInJSONForm);
            JSONObject res0 = ocrObject0.getJSONObject("res");
            String wrong_answer_text = res0.getString("latex");
            System.out.println("错解文本信息: "+wrong_answer_text);

            BasicQuestion basicQuestion = new BasicQuestion();
            basicQuestion.setQid(qid);
            String question_text = basicQuestionService.getQuestionTextByQid(basicQuestion);

            //根据文本信息，大模型生成答案(这部分后期要优化)
            List<String> wrongTypes = concreteQuestionService.useWenxinStreamTransformToAnalyseWrongType(String.valueOf(question_text),wrong_answer_text);
            System.out.println(wrongTypes);

            //获取题目犯错信息
            String wrongType = wrongTypes.get(0);
            String wrongDetails = wrongTypes.get(1);
            String teachMethod = wrongTypes.get(2);

            String sid = StpUtil.getLoginIdAsString();
            String description = studentService.getStudentBySid(sid).getDescription();

            System.out.println("*****************************教学方案******************************");
            System.out.println("学生的基础情况为："+description);
            System.out.println("学生的错解文本为："+question_text);
            System.out.println("属于："+wrongType+"中的"+wrongDetails);
            System.out.println("根据情况，设计的教学方案为："+teachMethod);
            System.out.println("*****************************教学方案******************************");

            basicQuestion.setWrongText(wrong_answer_text);
            basicQuestion.setWrongType(wrongType);
            basicQuestion.setWrongDetails(wrongDetails);

            basicQuestionService.modifyBasicQuestion(basicQuestion);

            return CommonResponse.creatForSuccess("错解分析成功");
        } else {
            // 令牌无效或解码错误
            return CommonResponse.creatForError("请先登录");
        }
    }
    @GetMapping("/question/base/basicQuestion")
    public CommonResponse<BasicQuestion> getBasicQuestionByQid(@RequestBody BasicQuestion basicQuestion){
        if (StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(basicQuestionService.getBasicQuestionByQid(basicQuestion));
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }
    @PostMapping("/question/concrete")
    public CommonResponse<ConcreteQuestion> getConcreteQuestionByQid(@RequestBody ConcreteQuestion concreteQuestion){
        if (StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(concreteQuestionService.getConcreteQuestionByQid(concreteQuestion));
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }
    @GetMapping("/question/base")
    public CommonResponse<List<BasicQuestion>> getBasicQuestionList(){
        if (StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(basicQuestionService.getBasicQuestionList());
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }
    @GetMapping("/question/base/order")
    public CommonResponse<List<BasicQuestion>> getBasicQuestionListInOrder(){
        if (StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(basicQuestionService.getBasicQuestionListInOrder());
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }
    @GetMapping("/question/base/mark")
    public CommonResponse<List<BasicQuestion>> getMarkedBasicQuestionList(){
        if (StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(basicQuestionService.getMarkedBasicQuestionList());
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }
    @PostMapping("/question/mark")
    public CommonResponse<BasicQuestion> addQuestionMark(@RequestBody BasicQuestion basicQuestion){
        if(StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(basicQuestionService.addQuestionMark(basicQuestion));
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }
    @DeleteMapping("/question/mark")
    public CommonResponse<BasicQuestion> deleteQuestionMark(@RequestBody BasicQuestion basicQuestion){
        if(StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(basicQuestionService.deleteQuestionMark(basicQuestion));
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }
    @GetMapping("/question/position")
    public CommonResponse<BasicQuestion> getQuestionPosition(@RequestBody BasicQuestion basicQuestion){
        if(StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(basicQuestionService.getQuestionPosition(basicQuestion));
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }
    @PostMapping("/question/position")
    public CommonResponse<BasicQuestion> addQuestionPosition(@RequestBody BasicQuestion basicQuestion){
        if(StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(basicQuestionService.addQuestionPosition(basicQuestion));
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }
    @PutMapping("/question/position")
    public CommonResponse<BasicQuestion> modifyQuestionPosition(@RequestBody BasicQuestion basicQuestion){
        if(StpUtil.isLogin()){
            return CommonResponse.creatForSuccess(basicQuestionService.modifyQuestionPosition(basicQuestion));
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping(value = "/question/communicationWithUser", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> communicateWithWenxin(@RequestParam String sid,@RequestParam String content,@RequestParam String qid) throws IOException, JSONException {
        return concreteQuestionService.useWenxinStreamTransformToCommunicateWithUser(sid,qid,content);
    }

    @GetMapping(value = "/question/communicationWithUser/audio", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> communicateWithWenxinByAudio(MultipartFile file,@RequestParam String qid,@RequestParam String sid) throws IOException, JSONException {

        String url = ossService.uploadPCMFileAndReturnName(file);
        System.out.println("图片存储路径："+url);
        String content = chatGPTservice.getTextByPcm(url);
        System.out.println("语音识别内容：" + content);
        return concreteQuestionService.useWenxinStreamTransformToCommunicateWithUser(sid,qid,content);

    }


    @GetMapping("/question/communicationWithUser/wrongAnswer")
    public Flux<String> communicateWithWenxinWithWrongAnswer(@RequestParam String sid,@RequestParam String content,@RequestParam String qid) throws IOException, JSONException {
        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setQid(qid);
        BasicQuestion basicQuestion2 = basicQuestionService.getBasicQuestionByQid(basicQuestion);
        String wrong_text = basicQuestion2.getWrongText();
        String wrongReason = basicQuestion2.getWrongType()+"中的"+basicQuestion2.getWrongDetails();
        return concreteQuestionService.useWenxinStreamTransformToCommunicateWithUserWithWrongAnswer(sid,qid, String.valueOf(wrong_text),wrongReason,content);
    }

    @GetMapping(value = "/question/communicationWithUser/wrongAnswer/audio", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> communicateWithWenxinByAudioWithWrongAnswer(MultipartFile file,@RequestParam String qid,@RequestParam String sid) throws IOException, JSONException {

        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setQid(qid);
        BasicQuestion basicQuestion2 = basicQuestionService.getBasicQuestionByQid(basicQuestion);
        String wrong_text = basicQuestion2.getWrongText();
        String wrongReason = basicQuestion2.getWrongType()+"中的"+basicQuestion2.getWrongDetails();

        String url = ossService.uploadPCMFileAndReturnName(file);
        System.out.println("图片存储路径："+url);
        String content = chatGPTservice.getTextByPcm(url);
        System.out.println("语音识别内容：" + content);
        return concreteQuestionService.useWenxinStreamTransformToCommunicateWithUserWithWrongAnswer(sid,qid, String.valueOf(wrong_text),wrongReason,content);

    }


    @GetMapping("/question/communication")
    public CommonResponse<ChatHistory> getChatHistroyByQid(@RequestParam String qid) throws IOException {
        if(StpUtil.isLogin()){
            ChatHistory chatHistory = concreteQuestionService.getChatHistoryByQid(qid);
            System.out.println(chatHistory);
            return CommonResponse.creatForSuccess(chatHistory);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping("/question/communication/wrongAnswer")
    public CommonResponse<WrongReasonChatHistory> getWrongAnswerChatHistroyByQid(@RequestParam String qid) throws IOException {
        if(StpUtil.isLogin()){
            WrongReasonChatHistory chatHistory = concreteQuestionService.getWrongAnswerChatHistoryByQid(qid);
            System.out.println(chatHistory);
            return CommonResponse.creatForSuccess(chatHistory);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping("/question/stepInfo")
    public CommonResponse<String> getQuestionAnalysisStepInfo(@RequestParam String qid,@RequestParam int number) throws IOException, JSONException {
        if(StpUtil.isLogin()){
            String result = concreteQuestionService.getQuestionStepByQuestionNumber(qid,number);
            return CommonResponse.creatForSuccess(result);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping("/question/knowledges")
    public CommonResponse<List<String>> getQuestionKnowledges(@RequestParam String qid) throws IOException, JSONException {
        if(StpUtil.isLogin()){
            List<String> resultTemp = concreteQuestionService.getQuestionKnowledgesByQid(qid);
            List<String> result = new ArrayList<>();
            result.add("请生成这个题的答案");
            result.add("请生成这个题的解析");
            result.add("这个问题与我们之前学过的哪些知识相关");
            result.add("我可以用哪些方法来解决这个问题");
            result.add("这个题目有什么实际应用吗");
            for(String temp : resultTemp){
                temp = "如何理解"+temp;
                result.add(temp);
            }
            return CommonResponse.creatForSuccess(result);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @PostMapping("/question/note")
    public CommonResponse<String> uploadQuestionNoteByQid(@RequestBody ConcreteQuestion concreteQuestion) throws IOException, JSONException {
        if(StpUtil.isLogin()){
            String result = concreteQuestionService.uploadQuestionNotesByQid(concreteQuestion.getQid(),concreteQuestion.getNote());
            return CommonResponse.creatForSuccess(result);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @PostMapping("/question/getnote")
    public CommonResponse<String> getQuestionNoteByQid(@RequestBody ConcreteQuestion concreteQuestion) throws IOException, JSONException {
        if(StpUtil.isLogin()){
            ConcreteQuestion result = concreteQuestionService.getQuestionNotesByQid(concreteQuestion.getQid());
            return CommonResponse.creatForSuccess(result.getNote());
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @PutMapping("/question/note")
    public CommonResponse<String> modifyQuestionNoteByQid(@RequestBody ConcreteQuestion concreteQuestion) throws IOException, JSONException {
        if(StpUtil.isLogin()){
            String result = concreteQuestionService.modifyQuestionNotesByQid(concreteQuestion.getQid(),concreteQuestion.getNote());
            return CommonResponse.creatForSuccess(result);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @DeleteMapping("/question/note")
    public CommonResponse<String> deleteQuestionNoteByQid(@RequestBody ConcreteQuestion concreteQuestion) throws IOException, JSONException {
        if(StpUtil.isLogin()){
            String result = concreteQuestionService.deleteQuestionNotesByQid(concreteQuestion.getQid());
            return CommonResponse.creatForSuccess(result);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping("/question/position/details")
    public CommonResponse<List<HashMap<String,Object>>> getQuestionInfoByPosition(@RequestParam String position){
        if(StpUtil.isLogin()){
            List<HashMap<String,Object>> result = basicQuestionService.getQuestionInfoByPosition(position);
            return CommonResponse.creatForSuccess(result);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping("/question/position/all")
    public CommonResponse<JsonNode> getPositionsByUid() throws JSONException {
        if(StpUtil.isLogin()){
            JsonNode result = basicQuestionService.getPositionsByUid();
            return CommonResponse.creatForSuccess(result);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping("/question/position/basicPosition")
    public CommonResponse<List<String>> getBasicPositionsByUid(){
        if(StpUtil.isLogin()){
            List<String> result = basicQuestionService.getBasicPositionsByUid();
            return CommonResponse.creatForSuccess(result);
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @DeleteMapping("/question/position/all")
    public CommonResponse<String> deleteQuestion_PositionsByPosition(@RequestParam String position){
        if(StpUtil.isLogin()){
            basicQuestionService.deleteQuestion_PositionsByPosition(position);
            return CommonResponse.creatForSuccess("删除成功");
        }else{
            return CommonResponse.creatForError("请先登录");
        }
    }

    @GetMapping("/studentProfile")
    public CommonResponse<Map<String, Object>> getStudentProfileBySid(@RequestParam String sid) throws IOException, JSONException {
        Map<String, Object> result = studentProfileService.getStudentProfileInformation(sid);
        return CommonResponse.creatForSuccess(result);
    }

    @GetMapping("/test")
    public CommonResponse<String> test(@RequestParam String sid) throws IOException, JSONException {
        Map<String, Object> result = studentProfileService.getStudentProfileInformation(sid);
        Object knowledgeList = result.get("knowledge_weight");
        Object abilityList = result.get("ability_weight");
        return CommonResponse.creatForSuccess(knowledgeList.toString()+abilityList.toString());
    }


}
