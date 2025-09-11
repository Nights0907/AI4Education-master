package org.musi.AI4Education.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName(value = "student_info")
public class Student {

    //学生基本信息
    @TableId
    private String sid;     //学生ID

    private String username;        //用户名

    private String password;        //密码

    private String phone;        //电话

    private String email;        //邮箱

    private String gender;      //性别

    private String description;      //描述

    //学生学业信息
    private String grade;      //年级

    private String major;      //选科

    private String ranking;      //排名

    //登录注册信息
    private int isLogin;

}
