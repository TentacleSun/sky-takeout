package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    //菜品id查询套餐
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);
}
