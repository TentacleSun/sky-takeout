package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.beans.Transient;


@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO){
        Dish dish= new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //向菜表插入1条，口味插入n条
        dishMapper.insert(dish);
        //insert完成后，在sql中主键回显返回id
        Long dishId = dish.getId();

        List<DishFlavor> flavorList = dishDTO.getFlavors();
        if(flavorList != null && flavorList.size()>0){
            flavorList.forEach(dishFlavor -> {dishFlavor.setDishId(dishId);});
            //加入口味数据
            dishFlavorMapper.insertBatch(flavorList);
        }
    }
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO){
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page= dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids){
        // 判断菜品能否删除（起售中不能删除）
        //被关联的菜品不能删除
        for(Long id : ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE)
                //起售中
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size()>0){
            //有套餐关联
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除口味数据与菜品数据
        for (Long id : ids) {
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteByDishId(id);
        }
    }

    /**
     * @param id 根据id查询口味与菜品
     * @return
     */
    public DishVO getByIdWithFlavor(Long id){
        //根据id查询口味与菜品
        Dish dish=dishMapper.getById(id);
        List <DishFlavor> dishFlavors =dishFlavorMapper.getByDishId(id);
        //封装VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }
    public void updateWithFlavor(DishDTO dishDTO){
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        dishMapper.update(dish);
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //List<DishFlavor> flavors =
    }
}
