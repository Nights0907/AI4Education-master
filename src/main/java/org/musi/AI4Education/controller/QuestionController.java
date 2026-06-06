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
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.*;
import java.sql.Date;

import static org.musi.AI4Education.config.OCRConfig.latexOcr;

@RestController
@RequestMapping("/student")
public class QuestionController {

    @Autowired
    private QuestionCreationService questionCreationService;
    @Autowired
    private BasicQuestionService basicQuestionService;
    @Autowired
    private ConcreteQuestionService concreteQuestionService;
    @Autowired
    private StudentProfileService studentProfileService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private SpeechRecognitionService speechRecognitionService;


    // ignore_security_alert
    @PostMapping("/bigModel")
    public CommonResponse<Map<String, Object>> createQuestion(MultipartFile question) throws Exception {
        if (!StpUtil.isLogin()) {
            return CommonResponse.creatForError("请先登录");
        }
        return questionCreationService.createQuestion(StpUtil.getLoginIdAsString(), question);
    }
    // ignore_security_alert
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
        return concreteQuestionService.useWenxinStreamTransformToCommunicateWithUser(resolveSid(sid),qid,content);
    }

    @PostMapping(value = "/speech/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<String> recognizeSpeech(@RequestParam("file") MultipartFile file) {
        return CommonResponse.creatForSuccess(speechRecognitionService.recognize(file));
    }

    // ignore_security_alert
    @PostMapping(value = "/question/communicationWithUser/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> communicateWithWenxinByAudio(@RequestParam("file") MultipartFile file, @RequestParam String qid, @RequestParam String sid) throws IOException, JSONException {
        String effectiveSid = resolveSid(sid);
        return Mono.fromCallable(() -> speechRecognitionService.recognize(file))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(content -> {
                    try {
                        System.out.println("语音识别内容：" + content);
                        return Flux.concat(
                                Flux.just(ServerSentEvent.<String>builder().event("recognized").data(content).build()),
                                concreteQuestionService.useWenxinStreamTransformToCommunicateWithUser(effectiveSid, qid, content)
                                        .map(answer -> ServerSentEvent.<String>builder().data(answer).build())
                        );
                    } catch (IOException | JSONException e) {
                        throw new IllegalStateException(e);
                    }
                });

    }


    @GetMapping("/question/communicationWithUser/wrongAnswer")
    public Flux<String> communicateWithWenxinWithWrongAnswer(@RequestParam String sid,@RequestParam String content,@RequestParam String qid) throws IOException, JSONException {
        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setQid(qid);
        BasicQuestion basicQuestion2 = basicQuestionService.getBasicQuestionByQid(basicQuestion);
        String wrong_text = basicQuestion2.getWrongText();
        String wrongReason = basicQuestion2.getWrongType()+"中的"+basicQuestion2.getWrongDetails();
        return concreteQuestionService.useWenxinStreamTransformToCommunicateWithUserWithWrongAnswer(resolveSid(sid),qid, String.valueOf(wrong_text),wrongReason,content);
    }

    // ignore_security_alert
    @PostMapping(value = "/question/communicationWithUser/wrongAnswer/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> communicateWithWenxinByAudioWithWrongAnswer(@RequestParam("file") MultipartFile file, @RequestParam String qid, @RequestParam String sid) throws IOException, JSONException {

        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setQid(qid);
        BasicQuestion basicQuestion2 = basicQuestionService.getBasicQuestionByQid(basicQuestion);
        String wrong_text = basicQuestion2.getWrongText();
        String wrongReason = basicQuestion2.getWrongType()+"中的"+basicQuestion2.getWrongDetails();
        String effectiveSid = resolveSid(sid);

        return Mono.fromCallable(() -> speechRecognitionService.recognize(file))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(content -> {
                    try {
                        System.out.println("语音识别内容：" + content);
                        return Flux.concat(
                                Flux.just(ServerSentEvent.<String>builder().event("recognized").data(content).build()),
                                concreteQuestionService.useWenxinStreamTransformToCommunicateWithUserWithWrongAnswer(effectiveSid, qid, String.valueOf(wrong_text), wrongReason, content)
                                        .map(answer -> ServerSentEvent.<String>builder().data(answer).build())
                        );
                    } catch (IOException | JSONException e) {
                        throw new IllegalStateException(e);
                    }
                });

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

    private String resolveSid(String sid) {
        if (sid == null || sid.trim().isEmpty() || "undefined".equalsIgnoreCase(sid.trim())) {
            if (!StpUtil.isLogin()) {
                throw new IllegalArgumentException("请先登录，或在请求中传入有效 sid");
            }
            return StpUtil.getLoginIdAsString();
        }
        return sid.trim();
    }
}
