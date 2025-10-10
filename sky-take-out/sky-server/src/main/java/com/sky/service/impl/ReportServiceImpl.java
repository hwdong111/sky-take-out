package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j

public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    WorkspaceService workspaceService;

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

    /**
     * 统计指定时间区间内的销量排名top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String namesStr = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numbersStr = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(namesStr)
                .numberList(numbersStr)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1 查询数据库，获取营业数据(最近30天)
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(dateBegin, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX)
        );


        //2 通过POI将数据写入到Excel文件中
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //获取Sheet页
            XSSFSheet sheet = excel.getSheetAt(0);
            //填充概览数据
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);//填充数据(时间）

            //获取第4行
            XSSFRow row4 = sheet.getRow(3);
            row4.getCell(2).setCellValue(businessDataVO.getTurnover());//填充营业额
            row4.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());//填充订单完成率
            row4.getCell(6).setCellValue(businessDataVO.getNewUsers());//填充新增用户数

            //获取第5行
            XSSFRow row5 = sheet.getRow(4);
            row5.getCell(2).setCellValue(businessDataVO.getValidOrderCount());//填充有效订单数
            row5.getCell(4).setCellValue(businessDataVO.getUnitPrice());//填充平均客单价

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(
                                date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX)
                );

                XSSFRow row = sheet.getRow(i + 7);
                row.getCell(1).setCellValue(date.toString());//填充日期
                row.getCell(2).setCellValue(businessDataVO.getTurnover());//填充营业额
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());//填充有效订单数
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());//填充订单完成率
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());//填充平均客单价
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());//填充新增用户数
            }


            //3 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);

            //关闭资源
            outputStream.close();
            excel.close();
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
