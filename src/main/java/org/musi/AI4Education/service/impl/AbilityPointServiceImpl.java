package org.musi.AI4Education.service.impl;

import org.musi.AI4Education.domain.entity.AbilityPoint;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.musi.AI4Education.mapper.AbilityPointMapper;
import org.musi.AI4Education.service.AbilityPointService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AbilityPointServiceImpl extends ServiceImpl<AbilityPointMapper, AbilityPoint> implements AbilityPointService {
    @Override
    public List<String> parseStringToList(String input) {
        List<String> resultList = new ArrayList<>();

        // 使用逗号分割内容
        String[] items = input.split(",");
        for (String item : items) {
            // 去掉空格并添加到列表中
            resultList.add(item.trim());
        }

        return resultList;
    }
}
