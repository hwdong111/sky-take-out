package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id查询套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdByDishId(List<Long> dishIds);

    /**
     * 插入套餐菜品关联数据
     * @param setmealDish
     */
    void save(SetmealDish setmealDish);
}
