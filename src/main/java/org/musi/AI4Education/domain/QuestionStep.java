package org.musi.AI4Education.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
public class QuestionStep {
    private int number;  // 步骤标号
    private String content;  //  详细信息
}
