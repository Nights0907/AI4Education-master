package org.musi.AI4Education.service;

import org.musi.AI4Education.domain.entity.KnowledgePoint;
import com.baomidou.mybatisplus.extension.service.IService;

public interface KnowledgePointService extends IService<KnowledgePoint> {
    public KnowledgePoint init_update_knowledge_point(String qid, String type);
}
