package org.musi.AI4Education.domain.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class KnowledgePoint {

    private String kid;      //知识点ID

    private List<String> qid;      //推导出知识点的题目ID

    private String type;     //知识点类型

    private int times;      //该知识点犯错次数

    private Date latestDate;        //知识点最新犯错时间

}
