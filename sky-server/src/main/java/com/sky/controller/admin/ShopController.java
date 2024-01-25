package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@Slf4j
@Api(tags = "店铺相关接口")
@RequestMapping("/admin/shop")
public class ShopController {
    static final private String KEY = "SHOP_STATUS";
    @Autowired
    private RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status){
        log.info("更改营业状态至{}",status);
        redisTemplate.opsForValue().set(KEY,status);

        return Result.success();
    }

    @GetMapping("status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> setStatus(){

        Integer status = (Integer)redisTemplate.opsForValue().get(KEY);
        log.info("查询营业状态{}",status);
        return Result.success(status);
    }
}
