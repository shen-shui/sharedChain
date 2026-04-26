package com.atguigu.spzx.manager.test;

import com.alibaba.excel.EasyExcel;
import com.atguigu.spzx.model.vo.product.CategoryExcelVo;

import java.util.ArrayList;
import java.util.List;

public class EasyExcelTest {
    public static void main(String[] args) {
//        read();

        write();
    }

    public static void read() {
        String fileName = "/Users/shuiheng/IdeaProjects/01.xlsx";

        ExcelListener<CategoryExcelVo> excelListener = new ExcelListener<>();
        EasyExcel.read(fileName, CategoryExcelVo.class, excelListener).sheet().doRead();
        List<CategoryExcelVo> data = excelListener.getData();
        data.forEach(System.out::println);
    }

    public static void write() {
        List<CategoryExcelVo> list = new ArrayList<>();
        list.add(new CategoryExcelVo(1L , "数码办公" , "",0L, 1, 1)) ;
        list.add(new CategoryExcelVo(11L , "华为手机" , "",1L, 1, 2)) ;
        EasyExcel.write("/Users/shuiheng/IdeaProjects/02.xlsx", CategoryExcelVo.class)
                .sheet("分类数据").doWrite(list);
    }
}
