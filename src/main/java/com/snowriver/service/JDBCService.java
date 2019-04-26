package com.snowriver.service;

import com.snowriver.entity.Member;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JDBCService {

    private static List<?> select(Object condition) {

        List<Object> result = new ArrayList<Object>();
        Class<?> entityClass = condition.getClass();

        Connection con = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;

        try {
            // 加载驱动
            Class.forName("com.mysql.jdbc.Driver");
            // 获取数据库连接
            con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/carlt?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Hongkong&autoReconnect=true", "root", "gfh261022");
            // 根据类名找属性名
            Map<String,String> columnMapper = new HashMap<String,String>();
            // 根据属性名找类命
            Map<String,String> fieldMapper = new HashMap<String,String>();
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    String name = column.name();
                    columnMapper.put(name, fieldName);
                    fieldMapper.put(fieldName, name);
                } else {
                    // 默认字段属性名一致
                    columnMapper.put(fieldName, fieldName);
                    fieldMapper.put(fieldName, fieldName);
                }
            }

            // 3. 创建语句集
            Table table = entityClass.getAnnotation(Table.class);
            String sql = "select * from " + table.name();

            StringBuffer where = new StringBuffer(" where 1=1 ");
            for (Field field : fields) {
                Object value =field.get(condition);
                if(null != value){
                    if(String.class == field.getType()) {
                        where.append(" and " + fieldMapper.get(field.getName()) + " = '" + value + "'");
                    }else{
                        where.append(" and " + fieldMapper.get(field.getName()) + " = " + value + "");
                    }
                }
            }

            System.out.println(sql + where.toString());
            pstm = con.prepareStatement(sql + where.toString());

            // 执行语句集
            rs = pstm.executeQuery();

            // 保存处理真正数值以外的所有的附加信息
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object instance = entityClass.newInstance();
                for (int i = 1; i <= columnCount; i++) {
                    // 从rs中获取当前这个游标下的类名
                    String columnName = rs.getMetaData().getColumnName(i);
                    // 通过属性名获取类名
                    Field field = entityClass.getDeclaredField(columnMapper.get(columnName));
                    field.setAccessible(true);
                    field.set(instance, rs.getObject(columnName));
                }
                result.add(instance);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        //6、 关闭结果集、 关闭语句集、 关闭连接
        finally {
            try {
                rs.close();
                pstm.close();
                con.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return result;
    }

    public static void main(String[] args) {
        Member member = new Member();
        //member.setAddr("cece");
        List<?> select = select(member);
        System.out.println(select);
    }

}