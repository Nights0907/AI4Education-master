package org.musi.AI4Education.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Date;

@Data
@TableName(value = "question_basic_info")
public class BasicQuestion{
    @TableId
    private String sid;     //学生ID
    private String qid;      //题目ID
    private String questionText;     //题干
    private String questionType;      //题型
    private Date date;      //拍题时间
    private String subject;     //科目
    private String wrongText;     //错解题干
    private String wrongType;       //错误类型
    private String wrongDetails;      //错误详情
    private int mark;        //标记
    private String path;       //题目照片存储路径
    private String position;    //题目存储文件夹的名称
}
