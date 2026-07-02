package org.musi.AI4Education.domain.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AbilityPoint {
    private String aid;      //能力 ID
    private String type;     //能力类型
    private List<String> qid;      //推导出题目ID
    private String kid;      //由哪个知识点推导出的，知识点ID
    private int times;      //该能力点的犯错次数
    private Date latestDate;        //该能力点的最新犯错时间
}
