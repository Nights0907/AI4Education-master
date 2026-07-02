package org.musi.AI4Education.service;

import org.musi.AI4Education.domain.entity.AbilityPoint;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AbilityPointService extends IService<AbilityPoint> {
    public List<String> parseStringToList(String input);
}
