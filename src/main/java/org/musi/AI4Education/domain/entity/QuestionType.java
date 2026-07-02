package org.musi.AI4Education.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName(value = "question_type")
public class QuestionType {

    @TableId
    private String tid;    //题目类型ID
    private String firstType;    // 大类型
    private String secondType;    //小类型

}
