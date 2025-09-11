package org.musi.AI4Education.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "history")
public class History {

    @TableId
    private String hid;    //历史记录ID

    private String sid;    //学生ID

    private String qid;    //错题ID

    private Date time;     //时间

    private String type;    //错误类型

    private String details;   //错误详情
}


