package org.musi.AI4Education.domain;

import lombok.Data;

import java.util.Date;

@Data
public class CharacterPoint {
//    private String description;
//    private int times;
//    private Date latestDate;
//
    private String type;    //沟通表达、创新思维、问题解决能力、好奇心。
    private String description;     //描述
    private Date latestDate;        //最新日期
}
