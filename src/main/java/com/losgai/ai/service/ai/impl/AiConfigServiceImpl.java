package com.losgai.ai.service.ai.impl;

import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.enums.ResultCodeEnum;
import com.losgai.ai.mapper.AiConfigMapper;
import com.losgai.ai.service.ai.AiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiConfigServiceImpl implements AiConfigService {

    private final AiConfigMapper aiConfigMapper;

    @Override
    @Transactional
    @CacheEvict(value = "models",allEntries = true)
    public ResultCodeEnum add(AiConfig aiConfig) {
        aiConfig.setCreateTime(Date.from(Instant.now()));
        aiConfig.setUpdateTime(Date.from(Instant.now()));
        aiConfigMapper.insert(aiConfig);
        if(aiConfig.getIsDefault()==1){
            // 排除其他默认
            aiConfigMapper.updateOtherIsNotDefault(aiConfig.getId());
        }
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    public List<AiConfig> queryByKeyWord(String keyWord) {
        return aiConfigMapper.selectByKeyWord(keyWord);
    }

    @Override
    @CacheEvict(value = "models",allEntries = true)
    public ResultCodeEnum deleteById(Long id) {
        aiConfigMapper.deleteByPrimaryKey(id);
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Transactional
    @CacheEvict(value = "models",allEntries = true)
    public ResultCodeEnum update(AiConfig aiConfig) {
        aiConfigMapper.updateByPrimaryKeySelective(aiConfig);
        if(aiConfig.getIsDefault()==1){
            // 排除其他默认
            aiConfigMapper.updateOtherIsNotDefault(aiConfig.getId());
        }
        return ResultCodeEnum.SUCCESS;
    }

    @Override
    @Cacheable("models")
    public List<AiConfig> getModels() {
        return aiConfigMapper.selectModelList();
    }
}
