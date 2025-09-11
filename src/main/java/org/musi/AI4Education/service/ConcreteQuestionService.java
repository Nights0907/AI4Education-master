package org.musi.AI4Education.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.IService;
import org.json.JSONException;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.domain.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ConcreteQuestionService extends IService<ConcreteQuestion> {

    public String test(String content) throws IOException;
    public void sendDataToClient(Long clientId, String data);

    public String constructRequestJson(Integer userId,
                                       Double temperature,
                                       Double topP,
                                       Double penaltyScore,
                                       boolean stream,
                                       List<Map<String, String>> messages);
    public String useWenxinStreamTransformToGetAnswerAndExplanationAndKnowledge(String question) throws IOException;

    public Map<String, String> useWenxinStreamTransformToAnalyseAbilityByKnowledge(List<String> knowledge) throws IOException;

    public String useWenxinStreamTransformToGetSteps(String question) throws IOException;

    public List<String> useWenxinStreamTransformToAnalyseWrongType(String question,String content) throws IOException, JSONException;

    public Flux<String> useWenxinStreamTransformToCommunicateWithUser(String sid,String qid, String content) throws IOException, JSONException;

    public Flux<String> useWenxinStreamTransformToCommunicateWithUserWithWrongAnswer(String sid,String qid, String wrongText, String wrongReason,String content) throws IOException, JSONException;

    public String getQuestionStepByQuestionNumber(String qid,int number);

    public List<String> getQuestionKnowledgesByQid(String qid);

    public String uploadQuestionNotesByQid(String qid,String note);

    public ConcreteQuestion getQuestionNotesByQid(String qid);

    public String modifyQuestionNotesByQid(String qid,String note);

    public String deleteQuestionNotesByQid(String qid);

    public JSON connectWithBigModel(String content) throws IOException;

    public String connectWithBigModelStreamTransition(String question) throws IOException;


    public ChatHistory getChatHistoryByQid(String qid) throws IOException;

    public WrongReasonChatHistory getWrongAnswerChatHistoryByQid(String qid) throws IOException;

    public CommonResponse<String> createConcreteQuestion(ConcreteQuestion concreteQuestion);

    public ArrayList<QuestionStep> createQuestionSteps(String steps);

    public List<String> splitAnswerAndExplanation(String steps);

    public List<String> splitKnowledges(String knowledges);

    //查询单个错题的详细信息
    public ConcreteQuestion getConcreteQuestionByQid(ConcreteQuestion concreteQuestion);

    public Map<String, String> parseStringToObject(String data);
}
