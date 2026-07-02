package org.musi.AI4Education.service;

import org.musi.AI4Education.domain.dto.AIAnswerDTO;
import org.musi.AI4Education.domain.entity.CharacterPoint;
import org.musi.AI4Education.domain.entity.StudentProfile;
import com.baomidou.mybatisplus.extension.service.IService;
import reactor.core.publisher.Flux;

import java.io.IOException;
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
