package com.offcn.seckill.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.entity.PageResult;
import com.offcn.mapper.TbSeckillGoodsMapper;
import com.offcn.mapper.TbSeckillOrderMapper;
import com.offcn.pojo.TbSeckillGoods;
import com.offcn.pojo.TbSeckillOrder;
import com.offcn.pojo.TbSeckillOrderExample;
import com.offcn.pojo.TbSeckillOrderExample.Criteria;
import com.offcn.seckill.service.SeckillOrderService;
import com.offcn.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

import java.util.Date;
import java.util.List;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private TbSeckillOrderMapper seckillOrderMapper;

    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    /**
     * 查询全部
     */
    @Override
    public List<TbSeckillOrder> findAll() {
        return seckillOrderMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbSeckillOrder> page = (Page<TbSeckillOrder>) seckillOrderMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(TbSeckillOrder seckillOrder) {
        seckillOrderMapper.insert(seckillOrder);
    }


    /**
     * 修改
     */
    @Override
    public void update(TbSeckillOrder seckillOrder) {
        seckillOrderMapper.updateByPrimaryKey(seckillOrder);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbSeckillOrder findOne(Long id) {
        return seckillOrderMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            seckillOrderMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbSeckillOrder seckillOrder, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbSeckillOrderExample example = new TbSeckillOrderExample();
        Criteria criteria = example.createCriteria();

        if (seckillOrder != null) {
            if (seckillOrder.getUserId() != null && seckillOrder.getUserId().length() > 0) {
                criteria.andUserIdLike("%" + seckillOrder.getUserId() + "%");
            }
            if (seckillOrder.getSellerId() != null && seckillOrder.getSellerId().length() > 0) {
                criteria.andSellerIdLike("%" + seckillOrder.getSellerId() + "%");
            }
            if (seckillOrder.getStatus() != null && seckillOrder.getStatus().length() > 0) {
                criteria.andStatusLike("%" + seckillOrder.getStatus() + "%");
            }
            if (seckillOrder.getReceiverAddress() != null && seckillOrder.getReceiverAddress().length() > 0) {
                criteria.andReceiverAddressLike("%" + seckillOrder.getReceiverAddress() + "%");
            }
            if (seckillOrder.getReceiverMobile() != null && seckillOrder.getReceiverMobile().length() > 0) {
                criteria.andReceiverMobileLike("%" + seckillOrder.getReceiverMobile() + "%");
            }
            if (seckillOrder.getReceiver() != null && seckillOrder.getReceiver().length() > 0) {
                criteria.andReceiverLike("%" + seckillOrder.getReceiver() + "%");
            }
            if (seckillOrder.getTransactionId() != null && seckillOrder.getTransactionId().length() > 0) {
                criteria.andTransactionIdLike("%" + seckillOrder.getTransactionId() + "%");
            }
        }

        Page<TbSeckillOrder> page = (Page<TbSeckillOrder>) seckillOrderMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void submitOrder(Long seckillId, String userId) {
        //允许使用redis事务
        redisTemplate.setEnableTransactionSupport(true);
        Object result = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations redisOperations) throws DataAccessException {
                redisOperations.watch("seckillGoods");
                //在开启事务前执行查询，获取秒杀商品对象
                TbSeckillGoods seckillGoods = (TbSeckillGoods) redisOperations.boundHashOps("seckillGoods").get(seckillId);
                //开启事务
                redisOperations.multi();
                //必要的空查询
                redisOperations.boundHashOps("seckillGoods").get(seckillId);
                //判断是否为空
                if (seckillGoods == null) {
                    redisOperations.exec();
                    throw new RuntimeException("秒杀商品不存在");
                }
                //获取商品库存，判断库存数量是否大于0
                if (seckillGoods.getStockCount() <= 0) {
                    redisOperations.exec();
                    throw new RuntimeException("商品被抢购一空");
                }
                //该用户已经抢购到商品，扣减库存（Redis）
                seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
                //更新最新库存到redis
                redisOperations.boundHashOps("seckillGoods").put(seckillId, seckillGoods);
                //判断库存等于0，清除秒杀商品信息，从redis，保存数据到数据库
                if (seckillGoods.getStockCount() == 0) {
                    //保存数据到数据库
                    seckillGoodsMapper.updateByPrimaryKey(seckillGoods);
                    //清除redis缓存里面该商品秒杀数据
                    redisOperations.boundHashOps("seckillGoods").delete(seckillId);
                }
                //保存订单（redis）
                TbSeckillOrder seckillOrder = new TbSeckillOrder();
                seckillOrder.setId(idWorker.nextId());
                seckillOrder.setSeckillId(seckillGoods.getId());
                //订单创建时间
                seckillOrder.setCreateTime(new Date());
                //秒杀价格
                seckillOrder.setMoney(seckillGoods.getCostPrice());
                //卖家
                seckillOrder.setSellerId(seckillGoods.getSellerId());
                //买家
                seckillOrder.setUserId(userId);
                //订单状态
                seckillOrder.setStatus("0");
                //保存秒杀订单到redis缓存
                redisOperations.boundHashOps("seckillOrder").put(userId, seckillOrder);
                //提交事务
                return redisOperations.exec();
            }
        });
    }

    @Override
    public TbSeckillOrder searchOrderFromRedisByUserId(String userId) {
        return (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
    }

    @Override
    public void saveOrderFromRedisToDB(String userId, Long orderId, String transactionId) {
        System.out.println("saveOrderFromRedisToDb:" + userId);
        //根据用户ID查询日志
        TbSeckillOrder seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
        if (seckillOrder == null) {
            throw new RuntimeException("订单不存在");
        }
        //如果与传递过来的订单号不符
        if (seckillOrder.getId().longValue() != orderId.longValue()) {
            throw new RuntimeException("订单不相符");
        }
        seckillOrder.setTransactionId(transactionId);//交易流水号
        seckillOrder.setPayTime(new Date());//支付之间
        seckillOrder.setStatus("1");//状态
        seckillOrderMapper.insert(seckillOrder);//保存到数据库
        redisTemplate.boundHashOps("seckillOrder").delete(userId);//从redis中清除
    }

    @Override
    public void deleteOrderFromRedis(String userId, Long orderId) {
        //根据用户ID查询日志
        TbSeckillOrder seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
        if (seckillOrder != null && seckillOrder.getId().longValue() == orderId.longValue()) {
            redisTemplate.boundHashOps("seckillOrder").delete(userId);//删除缓存中的订单
            //恢复库存
            //1.从缓存中提取秒杀商品
            TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps("seckillGoods").get(userId);
            if (seckillGoods != null) {
                seckillGoods.setStockCount(seckillGoods.getStockCount() + 1);
                redisTemplate.boundHashOps("seckillGoods").put(seckillOrder.getSeckillId(), seckillGoods);//存入缓存
            }
        }
    }

}
