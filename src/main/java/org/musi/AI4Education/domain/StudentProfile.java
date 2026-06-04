package org.musi.AI4Education.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@TableName(value = "student_profile")
@Document(collection = "studentProfile")
public class StudentProfile {

    @TableId
    private String stid;     //学生档案ID

    private String sid;      //学生ID

    private String subject;     //学科

    private List<String> qidList;      // 搜过的题目集合

    private List<KnowledgePoint> knowledgePointList;        //知识点档案

    private List<AbilityPoint> abilityPointList;        //能力档案

    private List<CharacterPoint> characterPointList;        //个性偏好档案

}
