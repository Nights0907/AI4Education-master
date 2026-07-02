package org.musi.AI4Education.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName(value = "question_concrete_info")
public class ConcreteQuestion {

    @TableId
    private String qid;      //题目ID
    private String questionAnswer;   //题目答案
    private String inspiration;     //易错点
    private String questionText;      //题目文本内容
    private String reason;      //错误原因
    private List<QuestionStep> questionSteps;       //解题步骤
    private String questionAnalysis;      //错题分析
    private List<String> knowledges;
    private String note;

}
