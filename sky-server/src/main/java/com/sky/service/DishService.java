package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

import java.util.List;

public interface DishService {
    /**
     * @param dishDTO
     * 新增菜品与口味
     */
    public void saveWithFlavor(DishDTO dishDTO);

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * @param ids
     * 批量删除
     */
    void deleteBatch(List<Long> ids);
}
