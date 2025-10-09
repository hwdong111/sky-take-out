package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j

public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内营业额数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end的每一天的日期
        List<LocalDate> dateList = new ArrayList<>();

        while(!begin.equals(end)) {
            //添加日期
            dateList.add(begin);
            //日期加1天
            begin = begin.plusDays(1);
        }
        //添加最后一天
        dateList.add(end);
        //将日期列表转换为字符串，用逗号分隔
        String dateListStr = StringUtils.join(dateList, ",");

        //存放每一天的营业额数据
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date日期对应的营业额数据
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        //将营业额列表转换为字符串，用逗号分隔
        String turnoverListStr = StringUtils.join(turnoverList, ",");

        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }

    /**
     * 统计指定时间区间内用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserReportStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end的每一天的日期
        List<LocalDate> dateList = new ArrayList<>();

        Map map = new HashMap();
        map.put("end", LocalDateTime.of(begin, LocalTime.MIN));
        Integer totalUser = userMapper.countByMap(map);

        while(!begin.equals(end)) {
            //添加日期
            dateList.add(begin);
            //日期加1天
            begin = begin.plusDays(1);
        }
        //添加最后一天
        dateList.add(end);
        //将日期列表转换为字符串，用逗号分隔
        String dateListStr = StringUtils.join(dateList, ",");

        //存放每天的新增用户数量
        List<Integer> newUserList = new ArrayList<>();
        //存放每天的用户总数
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            //查询date对应的新增用户数量
            map.put("begin", LocalDateTime.of(date, LocalTime.MIN));
            map.put("end", LocalDateTime.of(date, LocalTime.MAX));

            Integer newUser = userMapper.countByMap(map);

            //存放新增用户数和总数
            newUserList.add(newUser);
            totalUser += newUser;
            totalUserList.add(totalUser);
        }

        //将新增用户数和总数列表转换为字符串，用逗号分隔
        String newUserListStr = StringUtils.join(newUserList, ",");
        String totalUserListStr = StringUtils.join(totalUserList, ",");

        return UserReportVO.builder()
                .dateList(dateListStr)
                .newUserList(newUserListStr)
                .totalUserList(totalUserListStr)
                .build();
    }

    /**
     * 统计指定时间区间内订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end的每一天的日期
        List<LocalDate> dateList = new ArrayList<>();

        while(!begin.equals(end)) {
            //添加日期
            dateList.add(begin);
            //日期加1天
            begin = begin.plusDays(1);
        }
        //添加最后一天
        dateList.add(end);
        //将日期列表转换为字符串，用逗号分隔
        String dateListStr = StringUtils.join(dateList, ",");

        //存放每天的有效订单数量和订单总数
        List<Integer> validOrderList = new ArrayList<>();//存放每天的有效订单
        List<Integer> orderCountList = new ArrayList<>();//存放每天的订单总数

        Integer totalOrder = 0;//存放订单总数
        Integer totalValidOrder = 0;//存放有效订单总数

        for (LocalDate date : dateList) {
            //查询date对应的订单数量和有效订单总数
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            totalOrder += orderCount;
            totalValidOrder += validOrderCount;

            orderCountList.add(orderCount);
            validOrderList.add(validOrderCount);
        }
        //将订单数量列表转换为字符串，用逗号分隔
        String orderCountListStr = StringUtils.join(orderCountList, ",");
        String validOrderListStr = StringUtils.join(validOrderList, ",");

        //计算订单完成率
        Double orderCompletionRate = (totalOrder == 0) ? 0.0 : (double) totalValidOrder / totalOrder;

        return OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(orderCountListStr)
                .validOrderCountList(validOrderListStr)
                .totalOrderCount(totalOrder)
                .validOrderCount(totalValidOrder)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间内订单数量
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }
}
