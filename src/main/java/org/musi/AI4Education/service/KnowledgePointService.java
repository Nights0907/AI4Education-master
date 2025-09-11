package org.musi.AI4Education.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.musi.AI4Education.domain.KnowledgePoint;

public interface KnowledgePointService extends IService<KnowledgePoint> {
    public KnowledgePoint init_update_knowledge_point(String qid, String type);
}
