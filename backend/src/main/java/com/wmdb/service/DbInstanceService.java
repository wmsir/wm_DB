package com.wmdb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.DbInstanceMapper;
import com.wmdb.model.DbInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据库实例服务
 *
 * @author wm
 * @date 2023-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbInstanceService {

    private final DbInstanceMapper dbInstanceMapper;

    public List<DbInstance> listInstances() {
        return dbInstanceMapper.selectList(new QueryWrapper<>());
    }

    public void saveInstance(DbInstance instance) {
        instance.setStatus("AUDITING");
        if (instance.getId() == null) {
            dbInstanceMapper.insert(instance);
        } else {
            dbInstanceMapper.updateById(instance);
        }
    }

    public void deleteInstance(Long id) {
        dbInstanceMapper.deleteById(id);
    }

    public void approveInstance(Long id) {
        DbInstance instance = dbInstanceMapper.selectById(id);
        if (instance != null) {
            instance.setStatus("APPROVED");
            dbInstanceMapper.updateById(instance);
        }
    }
}
