package org.musi.AI4Education.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.musi.AI4Education.domain.AIAnswerDTO;
import org.musi.AI4Education.domain.CharacterPoint;
import org.musi.AI4Education.domain.ConcreteQuestion;
import org.musi.AI4Education.domain.StudentProfile;
import org.musi.AI4Education.service.impl.StudentProfileServiceImpl;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface StudentProfileService extends IService<StudentProfile> {
    public StudentProfile getStudentProfileBySid(String sid);
    public StudentProfile createStudentProfile(StudentProfile studentProfile);
    public StudentProfile updateStudentProfile(StudentProfile studentProfile);
    public Map<String, Long> countQuestionPerDay();
    public Flux<AIAnswerDTO> updateCharacterPointByQidAndSid(StudentProfile studentProfile);
    public void useWenxinUpdateCharacterPointByQidAndSid(StudentProfile studentProfile) throws IOException;
    public List<CharacterPoint> getCharacterPointByQidAndSid(StudentProfile studentProfile);
    public Map<String, Object> getStudentProfileInformation(String sid);
}
