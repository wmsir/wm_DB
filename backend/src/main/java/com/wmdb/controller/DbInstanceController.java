package com.wmdb.controller;

import com.wmdb.common.Result;
import com.wmdb.model.DbInstance;
import com.wmdb.service.DbInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据库实例控制器
 *
 * @author wm
 * @date 2023-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/instance")
@RequiredArgsConstructor
public class DbInstanceController {

    private final DbInstanceService dbInstanceService;

    @GetMapping("/list")
    public Result<List<DbInstance>> listInstances() {
        return Result.success(dbInstanceService.listInstances());
    }

    @PostMapping("/save")
    public Result<Void> saveInstance(@RequestBody DbInstance instance) {
        dbInstanceService.saveInstance(instance);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteInstance(@PathVariable("id") Long id) {
        dbInstanceService.deleteInstance(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approveInstance(@PathVariable("id") Long id) {
        dbInstanceService.approveInstance(id);
        return Result.success(null);
    }
}
