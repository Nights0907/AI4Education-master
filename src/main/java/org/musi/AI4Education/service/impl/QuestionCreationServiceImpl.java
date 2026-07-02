package org.musi.AI4Education.service.impl;

import org.musi.AI4Education.domain.entity.AbilityPoint;
import org.musi.AI4Education.domain.entity.KnowledgePoint;
import org.musi.AI4Education.domain.entity.QuestionStep;
import org.musi.AI4Education.domain.entity.BasicQuestion;
import org.musi.AI4Education.domain.entity.ConcreteQuestion;
import org.musi.AI4Education.domain.entity.History;
import org.musi.AI4Education.domain.entity.StudentProfile;
import org.json.JSONObject;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.service.AbilityPointService;
import org.musi.AI4Education.service.BasicQuestionService;
import org.musi.AI4Education.service.ConcreteQuestionService;
import org.musi.AI4Education.service.HistoryService;
import org.musi.AI4Education.service.OSSService;
import org.musi.AI4Education.service.QuestionCreationService;
import org.musi.AI4Education.service.StudentProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.musi.AI4Education.config.OCRConfig.latexOcr;

@Service
public class QuestionCreationServiceImpl implements QuestionCreationService {

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

    @Override
    public CommonResponse<Map<String, Object>> createQuestion(String sid, MultipartFile question) throws Exception {
        String url = ossService.uploadFile(question);
        String content = latexOcr(question);

        JSONObject ocrObject = new JSONObject(content);
        JSONObject res = ocrObject.getJSONObject("res");
        String questionText = res.getString("latex");

        String answerAndExplanationAndKnowledges = concreteQuestionService.useWenxinStreamTransformToGetAnswerAndExplanationAndKnowledge(questionText);
        String steps = concreteQuestionService.useWenxinStreamTransformToGetSteps(questionText);

        List<String> result = concreteQuestionService.splitAnswerAndExplanation(answerAndExplanationAndKnowledges);
        if (result.size() < 3) {
            throw new IllegalStateException("大模型返回格式错误：" + answerAndExplanationAndKnowledges);
        }
        String answer = result.get(0);
        String explanation = result.get(1);
        List<String> knowledges = concreteQuestionService.splitKnowledges(result.get(2));

        String qid = String.valueOf(System.currentTimeMillis());
        Date currentDate = new Date(System.currentTimeMillis());

        BasicQuestion basicQuestion = createBasicQuestion(sid, qid, currentDate, questionText, url);
        ConcreteQuestion concreteQuestion = createConcreteQuestion(qid, questionText, answer, explanation, knowledges, steps);
        createHistory(sid, qid, currentDate);
        updateStudentProfile(sid, qid, currentDate, knowledges);

        Map<String, Object> data = new HashMap<>();
        data.put("basicQuestion", basicQuestion);
        data.put("concreteQuestion", concreteQuestion);
        return CommonResponse.creatForSuccess(data);
    }

    private BasicQuestion createBasicQuestion(String sid, String qid, Date currentDate, String questionText, String url) {
        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setSid(sid);
        basicQuestion.setQid(qid);
        basicQuestion.setDate(currentDate);
        basicQuestion.setSubject("数学");
        basicQuestion.setQuestionText(questionText);
        basicQuestion.setQuestionType("选择题");
        basicQuestion.setMark(0);
        basicQuestion.setPath(url);
        basicQuestionService.createBasicQuestion(basicQuestion);
        return basicQuestion;
    }

    private ConcreteQuestion createConcreteQuestion(String qid, String questionText, String answer, String explanation, List<String> knowledges, String steps) {
        ConcreteQuestion concreteQuestion = new ConcreteQuestion();
        concreteQuestion.setQid(qid);
        concreteQuestion.setInspiration("负负得正");
        concreteQuestion.setQuestionText(questionText);
        concreteQuestion.setQuestionAnswer(answer);
        concreteQuestion.setQuestionAnalysis(explanation);
        concreteQuestion.setKnowledges(knowledges);
        concreteQuestion.setNote("");
        ArrayList<QuestionStep> questionStepList = concreteQuestionService.createQuestionSteps(steps);
        concreteQuestion.setQuestionSteps(questionStepList);
        concreteQuestionService.createConcreteQuestion(concreteQuestion);
        return concreteQuestion;
    }

    private void createHistory(String sid, String qid, Date currentDate) {
        History history = new History();
        history.setSid(sid);
        history.setHid(String.valueOf(System.currentTimeMillis()));
        history.setQid(qid);
        history.setTime(currentDate);
        history.setType("计算错误");
        history.setDetails("正负值错误");
        historyService.createHistory(history);
    }

    private void updateStudentProfile(String sid, String qid, Date currentDate, List<String> knowledges) throws Exception {
        Map<String, String> knowledgeAbility = concreteQuestionService.useWenxinStreamTransformToAnalyseAbilityByKnowledge(knowledges);
        StudentProfile studentProfile = studentProfileService.getStudentProfileBySid(sid);
        if (studentProfile == null) {
            createStudentProfile(sid, qid, currentDate, knowledgeAbility);
            return;
        }
        updateExistingStudentProfile(studentProfile, qid, currentDate, knowledgeAbility);
    }

    private void createStudentProfile(String sid, String qid, Date currentDate, Map<String, String> knowledgeAbility) throws Exception {
        StudentProfile studentProfile = new StudentProfile();
        studentProfile.setStid(String.valueOf(System.currentTimeMillis()));
        studentProfile.setSid(sid);
        studentProfile.setSubject("数学");

        List<KnowledgePoint> knowledgePointList = new ArrayList<>();
        List<AbilityPoint> abilityPointList = new ArrayList<>();
        for (Map.Entry<String, String> entry : knowledgeAbility.entrySet()) {
            String kid = String.valueOf(System.currentTimeMillis());
            knowledgePointList.add(newKnowledgePoint(kid, entry.getKey(), qid, currentDate));
            for (String ability : abilityPointService.parseStringToList(entry.getValue())) {
                abilityPointList.add(newAbilityPoint(kid, ability, qid, currentDate));
            }
        }

        studentProfile.setKnowledgePointList(knowledgePointList);
        studentProfile.setAbilityPointList(abilityPointList);
        studentProfileService.createStudentProfile(studentProfile);
        studentProfileService.useWenxinUpdateCharacterPointByQidAndSid(studentProfile);
    }

    private void updateExistingStudentProfile(StudentProfile studentProfile, String qid, Date currentDate, Map<String, String> knowledgeAbility) throws Exception {
        List<KnowledgePoint> knowledgePointList = studentProfile.getKnowledgePointList();
        List<AbilityPoint> abilityPointList = studentProfile.getAbilityPointList();
        for (Map.Entry<String, String> entry : knowledgeAbility.entrySet()) {
            String kid = upsertKnowledgePoint(knowledgePointList, entry.getKey(), qid, currentDate);
            for (String ability : abilityPointService.parseStringToList(entry.getValue())) {
                upsertAbilityPoint(abilityPointList, kid, ability, qid, currentDate);
            }
        }
        studentProfile.setKnowledgePointList(knowledgePointList);
        studentProfile.setAbilityPointList(abilityPointList);
        studentProfileService.updateStudentProfile(studentProfile);
        studentProfileService.useWenxinUpdateCharacterPointByQidAndSid(studentProfile);
    }

    private String upsertKnowledgePoint(List<KnowledgePoint> knowledgePointList, String knowledge, String qid, Date currentDate) {
        for (KnowledgePoint knowledgePoint : knowledgePointList) {
            if (knowledgePoint.getType().equals(knowledge)) {
                knowledgePoint.getQid().add(qid);
                knowledgePoint.setTimes(knowledgePoint.getTimes() + 1);
                knowledgePoint.setLatestDate(currentDate);
                return knowledgePoint.getKid();
            }
        }
        String kid = String.valueOf(System.currentTimeMillis());
        knowledgePointList.add(newKnowledgePoint(kid, knowledge, qid, currentDate));
        return kid;
    }

    private void upsertAbilityPoint(List<AbilityPoint> abilityPointList, String kid, String ability, String qid, Date currentDate) {
        for (AbilityPoint abilityPoint : abilityPointList) {
            if (abilityPoint.getType().equals(ability)) {
                abilityPoint.getQid().add(qid);
                abilityPoint.setTimes(abilityPoint.getTimes() + 1);
                abilityPoint.setLatestDate(currentDate);
                return;
            }
        }
        abilityPointList.add(newAbilityPoint(kid, ability, qid, currentDate));
    }

    private KnowledgePoint newKnowledgePoint(String kid, String knowledge, String qid, Date currentDate) {
        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setKid(kid);
        List<String> qidList = new ArrayList<>();
        qidList.add(qid);
        knowledgePoint.setQid(qidList);
        knowledgePoint.setType(knowledge);
        knowledgePoint.setTimes(1);
        knowledgePoint.setLatestDate(currentDate);
        return knowledgePoint;
    }

    private AbilityPoint newAbilityPoint(String kid, String ability, String qid, Date currentDate) {
        AbilityPoint abilityPoint = new AbilityPoint();
        abilityPoint.setAid(String.valueOf(System.currentTimeMillis()));
        abilityPoint.setType(ability);
        List<String> qidList = new ArrayList<>();
        qidList.add(qid);
        abilityPoint.setQid(qidList);
        abilityPoint.setKid(kid);
        abilityPoint.setTimes(1);
        abilityPoint.setLatestDate(currentDate);
        return abilityPoint;
    }
}
