package org.musi.AI4Education.service.impl;

import org.musi.AI4Education.domain.entity.KnowledgePoint;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.musi.AI4Education.mapper.KnowledgePointMapper;
import org.musi.AI4Education.service.KnowledgePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class KnowledgePointServiceImpl extends ServiceImpl<KnowledgePointMapper, KnowledgePoint> implements KnowledgePointService {

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public KnowledgePoint init_update_knowledge_point(String qid, String type) {
        return null;
    }
}
