package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

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

    /**
     * @param id
     * 根据id查询口味与菜品
     */
    DishVO getByIdWithFlavor(Long id);

    void updateWithFlavor(DishDTO dishDTO);
    List<DishVO> listWithFlavor(Dish dish);


    void startOrStop(Integer status, Long id);
}
