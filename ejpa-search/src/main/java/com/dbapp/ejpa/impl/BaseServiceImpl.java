package com.dbapp.ejpa.impl;

import com.dbapp.ejpa.entity.BaseSearchEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import vip.efactory.common.base.entity.BaseSearchField;
import vip.efactory.common.base.enums.ConditionRelationEnum;
import vip.efactory.common.base.enums.SearchTypeEnum;
import vip.efactory.common.base.utils.DateTimeUtil;
import vip.efactory.common.base.utils.MapUtil;
import vip.efactory.common.base.utils.SQLFilter;

import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class BaseServiceImpl<T extends BaseSearchEntity>  {

    // 默认组的名称
    private final String DEFAULT_GROUP_NAME = "DEFAULT_NO_GROUP";

    // 常见的数字类型
    private static List<String> numberTypeList;
    // 常见日期时间类型
    private static List<String> dateTypeList;

    static { // 静态初始化以便提高性能
        // 保存常见的数字类型，以便避免逐个枚举类型处理
        numberTypeList = new ArrayList<>();
        numberTypeList.add("byte");
        numberTypeList.add("Byte");
        numberTypeList.add("short");
        numberTypeList.add("Short");
        numberTypeList.add("int");
        numberTypeList.add("Integer");
        numberTypeList.add("Long");
        numberTypeList.add("long");
        numberTypeList.add("float");
        numberTypeList.add("Float");
        numberTypeList.add("double");
        numberTypeList.add("Double");
        numberTypeList.add("BigInteger");
        numberTypeList.add("BigDecimal");
        // numberTypeList.add("AtomicInteger"); // 注释掉就说明JPA目前还不支持这些类型，类型来源于JDK
        // numberTypeList.add("AtomicLong");
        // numberTypeList.add("DoubleAccumulator");
        // numberTypeList.add("DoubleAdder");
        // numberTypeList.add("LongAccumulator");
        // numberTypeList.add("LongAdder");

        // 保存常见的日期时间类型
        dateTypeList = new ArrayList<>();
        dateTypeList.add("Date");
        dateTypeList.add("LocalDateTime");
        dateTypeList.add("LocalTime");
        dateTypeList.add("LocalDate");
    }


    /**
     * Description: 根据条件集合构建查询的表达式
     *
     * @return org.springframework.data.jpa.domain.Specification<T>
     */
    public Specification<T> getSpecification(T entity) {
        Set<BaseSearchField> conditions = entity.getConditions();
        // 检查条件是否合法,移除非法的条件
        checkPropertyAndValueValidity(entity);
        // 将条件按照各自所在的组进行分组
        Map<String, List<BaseSearchField>> groups = checkHasGroup(conditions);
        // 判断条件是否只有一个默认组，若是一个组，则说明没有组
        if (groups.size() == 1) {
            return handleSingleGroupCondition(groups.get(DEFAULT_GROUP_NAME), entity);
        } else {
            // 有多个组
            return handleGroupsCondition(groups, entity);
        }
    }

    /**
     * 处理多个分组条件的，条件查询构造
     */
    private Specification<T> handleGroupsCondition(Map<String, List<BaseSearchField>> groups, T entity) {
        return new Specification<T>() {
            @Override
            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                // 先处理默认组
                Predicate defaultGroupP = genPredicate4SingleGroup(groups.get(DEFAULT_GROUP_NAME), root, cb, entity);
                // 处理其他组
                for (Map.Entry<String, List<BaseSearchField>> entry : groups.entrySet()) {
                    if (DEFAULT_GROUP_NAME.equalsIgnoreCase(entry.getKey())) {
                        continue;
                    }

                    Predicate tmpGroupP = genPredicate4SingleGroup(entry.getValue(), root, cb, entity);
                    if (tmpGroupP == null) { // 若也为空则没有必要继续进行了！
                        continue;
                    }

                    // 从组内的一个条件里找到组的逻辑关系
                    if (defaultGroupP == null) { // 当默认组条件为空时，defaultGroupP为null，不处理会导致空指针异常！
                        defaultGroupP = tmpGroupP;
                    } else {
                        Integer logicalTypeGroup = entry.getValue().get(0).getLogicalTypeGroup();
                        if (logicalTypeGroup == ConditionRelationEnum.AND.getValue()) {
                            defaultGroupP = cb.and(defaultGroupP, tmpGroupP);
                        } else {
                            defaultGroupP = cb.or(defaultGroupP, tmpGroupP);
                        }
                    }
                }

                return defaultGroupP;
            }
        };
    }

    /**
     * Description:检查属性名和属性值的合法性,不合法的属性和值都会被移除
     *
     * @return void
     * @author dbdu
     * @date 19-7-5 下午12:31
     */
    private void checkPropertyAndValueValidity(T entity) {
        Set<BaseSearchField> conditions = entity.getConditions();
        if (conditions == null || conditions.size() == 0) {
            return;
        }

        // 检查属性名是否合法 非法
        Set<BaseSearchField> illegalConditions = new HashSet<>();        //存放非法的查询条件
        Map<String, String> properties = (Map<String, String>) MapUtil.objectToMap1(entity);
        Set<String> keys = properties.keySet();
        // 如果条件的字段名称与属性名不符，则移除，不作为选择条件；
        conditions.forEach(condition -> {
            if (!keys.contains(condition.getName())) {
                illegalConditions.add(condition);
            }
        });
        // 移除非法的条件
        conditions.removeAll(illegalConditions);

        //继续检查条件的值是否有非法敏感的关键字
        conditions.forEach(condition -> {
            String value1 = condition.getVal();
            if (SQLFilter.sqlInject(value1)) {
                illegalConditions.add(condition);
            }

            // 如果是范围需要检查两个值是否合法
            int searchType = condition.getSearchType() == null ? 0 : condition.getSearchType(); // searchType 用户可以不写,不写默认为0,模糊查询
            if (SearchTypeEnum.RANGE.getValue() == searchType) {
                String value2 = condition.getVal2();
                if (SQLFilter.sqlInject(value2)) {
                    illegalConditions.add(condition);
                }
            }
        });

        // 移除非法条件
        conditions.removeAll(illegalConditions);
    }



    /**
     * 检测条件中是否含有分组信息，例如：类似这样的条件：（A=3 || B=4） && （ C= 5 || D=6）
     */
    private Map<String, List<BaseSearchField>> checkHasGroup(Set<BaseSearchField> conditions) {
        Map<String, List<BaseSearchField>> groups = new HashMap<>();
        groups.put(DEFAULT_GROUP_NAME, new ArrayList<BaseSearchField>()); //存放没有明确分组的条件

        // 遍历所有的条件进行分组
        for (BaseSearchField searchField : conditions) {
            String groupName = searchField.getBracketsGroup();

            if (StringUtils.isEmpty(groupName)) { // 条件没有分组信息
                groups.get(DEFAULT_GROUP_NAME).add(searchField);
            } else { // 条件有分组信息
                // 检查groups是否有此分组，有则用，没有则创建
                if (groups.get(groupName) == null) {
                    groups.put(groupName, new ArrayList<BaseSearchField>()); //创建新的分组，
                }
                groups.get(groupName).add(searchField);    // 再将条件放进去
            }
        }

        // 对所有的分组按照 order排序
        for (Map.Entry<String, List<BaseSearchField>> entry : groups.entrySet()) {
            entry.getValue().sort(Comparator.comparingInt(BaseSearchField::getOrder));  // 条件排序,排序后默认是升序
        }

        return groups;
    }

    /**
     * 处理同一个组内查询条件的查询条件转换
     */
    private Specification<T> handleSingleGroupCondition(List<BaseSearchField> fields, T entity) {

        return new Specification<T>() {
            @Override
            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return genPredicate4SingleGroup(fields, root, cb, entity);
            }
        };

    }


    /**
     * 处理单个组内的条件生成
     */
    private Predicate genPredicate4SingleGroup(List<BaseSearchField> fields, Root<T> root, CriteriaBuilder cb, T entity) {
        Predicate finalPredicat = null;
        int count = fields.size();
        Predicate fieldP = null;
        for (int i = 0; i < count; i++) {
            BaseSearchField field = fields.get(i);
            // 得到条件的搜索类型，若为空默认模糊搜索
            int searchType = field.getSearchType() == null ? 0 : field.getSearchType();
            String key = field.getName();
            String startVal = field.getVal();      // 开始值
            String endVal = field.getVal2();    // 结束值
            // 处理查询值的类型转换
            String fieldType = getPropType(key, entity); //直接通过当前实体或者父类来获取属性的类型
            switch (searchType) {                   // cb支持更多的方法,此处仅使用常用的!
                case 1:     //  EQ(1, "等于查询"),
                    if (numberTypeList.contains(fieldType)) {
                        fieldP = cb.equal(root.get(key), convertType4PropertyValue(fieldType, startVal));
                    } else if (dateTypeList.contains(fieldType)) {
                        if (fieldType.equalsIgnoreCase("Date")) {
                            fieldP = cb.equal(root.<Date>get(key), DateTimeUtil.getDateFromString(startVal));
                        } else if (fieldType.equalsIgnoreCase("LocalDateTime")) {
                            fieldP = cb.equal(root.<LocalDateTime>get(key), DateTimeUtil.getLocalDateTimeFromString(startVal));
                        } else if (fieldType.equalsIgnoreCase("LocalDate")) {
                            fieldP = cb.equal(root.<LocalDate>get(key), DateTimeUtil.getLocalDateFromString(startVal));
                        } else if (fieldType.equalsIgnoreCase("LocalTime")) {
                            fieldP = cb.equal(root.<LocalTime>get(key), DateTimeUtil.getLocalTimeFromString(startVal));
                        }
                    } else {
                        fieldP = cb.equal(root.get(key).as(String.class), startVal);
                    }
                    break;
                case 2:     //  RANGE(2, "范围查询"),  如果结束值大于开始值，则交换位置避免查不到数据
                    if (numberTypeList.contains(fieldType)) {
                        fieldP = getPredicate4NumberBetweenConditiong(root, cb, key, fieldType, startVal, endVal);
                    } else if (dateTypeList.contains(fieldType)) {
                        if (fieldType.equalsIgnoreCase("Date")) {
                            Date start = DateTimeUtil.getDateFromString(startVal);
                            Date end = DateTimeUtil.getDateFromString(endVal);
                            fieldP = end.compareTo(start) > 0 ? cb.between(root.<Date>get(key), start, end) : cb.between(root.<Date>get(key), end, start);
                        } else if (fieldType.equalsIgnoreCase("LocalDateTime")) {
                            LocalDateTime start = DateTimeUtil.getLocalDateTimeFromString(startVal);
                            LocalDateTime end = DateTimeUtil.getLocalDateTimeFromString(endVal);
                            fieldP = end.compareTo(start) > 0 ? cb.between(root.<LocalDateTime>get(key), start, end) : cb.between(root.<LocalDateTime>get(key), end, start);
                        } else if (fieldType.equalsIgnoreCase("LocalDate")) {
                            LocalDate start = DateTimeUtil.getLocalDateFromString(startVal);
                            LocalDate end = DateTimeUtil.getLocalDateFromString(endVal);
                            fieldP = end.compareTo(start) > 0 ? cb.between(root.<LocalDate>get(key), start, end) : cb.between(root.<LocalDate>get(key), end, start);
                        } else if (fieldType.equalsIgnoreCase("LocalTime")) {
                            LocalTime start = DateTimeUtil.getLocalTimeFromString(startVal);
                            LocalTime end = DateTimeUtil.getLocalTimeFromString(endVal);
                            fieldP = end.compareTo(start) > 0 ? cb.between(root.<LocalTime>get(key), start, end) : cb.between(root.<LocalTime>get(key), end, start);
                        }
                    } else {
                        fieldP = cb.between(root.get(key).as(String.class), startVal, endVal);
                    }
                    break;
                case 3:     //  NE(3, "不等于查询"),
                    if (numberTypeList.contains(fieldType)) {
                        fieldP = cb.notEqual(root.get(key), convertType4PropertyValue(fieldType, startVal));
                    } else if (dateTypeList.contains(fieldType)) {
                        if (fieldType.equalsIgnoreCase("Date")) {
                            Date start = DateTimeUtil.getDateFromString(startVal);
                            fieldP = cb.notEqual(root.<Date>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDateTime")) {
                            LocalDateTime start = DateTimeUtil.getLocalDateTimeFromString(startVal);
                            fieldP = cb.notEqual(root.<LocalDateTime>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDate")) {
                            LocalDate start = DateTimeUtil.getLocalDateFromString(startVal);
                            fieldP = cb.notEqual(root.<LocalDate>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalTime")) {
                            LocalTime start = DateTimeUtil.getLocalTimeFromString(startVal);
                            fieldP = cb.notEqual(root.<LocalTime>get(key), start);
                        }
                    } else {
                        fieldP = cb.notEqual(root.get(key), startVal);
                    }
                    break;
                case 4:     //  LT(4, "小于查询"),
                    if (numberTypeList.contains(fieldType)) {
                        // fieldP = cb.lessThan(root.get(key), convertType4PropertyValue(fieldType, startVal));
                        fieldP = cb.lt(root.get(key), convertType4PropertyValue(fieldType, startVal));
                    } else if (dateTypeList.contains(fieldType)) {
                        if (fieldType.equalsIgnoreCase("Date")) {
                            Date start = DateTimeUtil.getDateFromString(startVal);
                            fieldP = cb.lessThan(root.<Date>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDateTime")) {
                            LocalDateTime start = DateTimeUtil.getLocalDateTimeFromString(startVal);
                            fieldP = cb.lessThan(root.<LocalDateTime>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDate")) {
                            LocalDate start = DateTimeUtil.getLocalDateFromString(startVal);
                            fieldP = cb.lessThan(root.<LocalDate>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalTime")) {
                            LocalTime start = DateTimeUtil.getLocalTimeFromString(startVal);
                            fieldP = cb.lessThan(root.<LocalTime>get(key), start);
                        }
                    } else {
                        fieldP = cb.lessThan(root.get(key).as(String.class), startVal);
                    }
                    break;
                case 5:     //  LE(5, "小于等于查询"),
                    if (numberTypeList.contains(fieldType)) {
                        // fieldP = cb.lessThanOrEqualTo(root.get(key), convertType4PropertyValue(fieldType, startVal));
                        fieldP = cb.le(root.get(key), convertType4PropertyValue(fieldType, startVal));
                    } else if (dateTypeList.contains(fieldType)) {
                        if (fieldType.equalsIgnoreCase("Date")) {
                            Date start = DateTimeUtil.getDateFromString(startVal);
                            fieldP = cb.lessThanOrEqualTo(root.<Date>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDateTime")) {
                            LocalDateTime start = DateTimeUtil.getLocalDateTimeFromString(startVal);
                            fieldP = cb.lessThanOrEqualTo(root.<LocalDateTime>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDate")) {
                            LocalDate start = DateTimeUtil.getLocalDateFromString(startVal);
                            fieldP = cb.lessThanOrEqualTo(root.<LocalDate>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalTime")) {
                            LocalTime start = DateTimeUtil.getLocalTimeFromString(startVal);
                            fieldP = cb.lessThanOrEqualTo(root.<LocalTime>get(key), start);
                        }
                    } else {
                        fieldP = cb.lessThanOrEqualTo(root.get(key).as(String.class), startVal);
                    }
                    break;
                case 6:     //  GT(6, "大于查询"),
                    if (numberTypeList.contains(fieldType)) {
                        // fieldP = cb.greaterThan(root.get(key), convertType4PropertyValue(fieldType, startVal));
                        fieldP = cb.gt(root.get(key), convertType4PropertyValue(fieldType, startVal));
                    } else if (dateTypeList.contains(fieldType)) {
                        if (fieldType.equalsIgnoreCase("Date")) {
                            Date start = DateTimeUtil.getDateFromString(startVal);
                            fieldP = cb.greaterThan(root.<Date>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDateTime")) {
                            LocalDateTime start = DateTimeUtil.getLocalDateTimeFromString(startVal);
                            fieldP = cb.greaterThan(root.<LocalDateTime>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDate")) {
                            LocalDate start = DateTimeUtil.getLocalDateFromString(startVal);
                            fieldP = cb.greaterThan(root.<LocalDate>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalTime")) {
                            LocalTime start = DateTimeUtil.getLocalTimeFromString(startVal);
                            fieldP = cb.greaterThan(root.<LocalTime>get(key), start);
                        }
                    } else {
                        fieldP = cb.greaterThan(root.get(key).as(String.class), startVal);
                    }
                    break;
                case 7:     //  GE(7, "大于等于查询");
                    if (numberTypeList.contains(fieldType)) {
                        // fieldP = cb.greaterThanOrEqualTo(root.get(key), convertType4PropertyValue(fieldType, startVal));
                        fieldP = cb.ge(root.get(key), convertType4PropertyValue(fieldType, startVal));
                    } else if (dateTypeList.contains(fieldType)) {
                        if (fieldType.equalsIgnoreCase("Date")) {
                            Date start = DateTimeUtil.getDateFromString(startVal);
                            fieldP = cb.greaterThanOrEqualTo(root.<Date>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDateTime")) {
                            LocalDateTime start = DateTimeUtil.getLocalDateTimeFromString(startVal);
                            fieldP = cb.greaterThanOrEqualTo(root.<LocalDateTime>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalDate")) {
                            LocalDate start = DateTimeUtil.getLocalDateFromString(startVal);
                            fieldP = cb.greaterThanOrEqualTo(root.<LocalDate>get(key), start);
                        } else if (fieldType.equalsIgnoreCase("LocalTime")) {
                            LocalTime start = DateTimeUtil.getLocalTimeFromString(startVal);
                            fieldP = cb.greaterThanOrEqualTo(root.<LocalTime>get(key), start);
                        }
                    } else {
                        fieldP = cb.greaterThanOrEqualTo(root.get(key).as(String.class), startVal);
                    }
                    break;
                case 8:     // IS_NULL(8, "Null值查询"),
                    fieldP = cb.isNull(root.get(key));
                    break;
                case 9:     // NOT_NULL(9, "非Null值查询")
                    fieldP = cb.isNotNull(root.get(key));
                    break;
                case 10:     // LEFT_LIKE(10, "左模糊查询"),
                    fieldP = cb.like(root.get(key).as(String.class), "%" + startVal);
                    break;
                case 11:     // RIGHT_LIKE(11, "右模糊查询")
                    fieldP = cb.like(root.get(key).as(String.class), startVal + "%");
                    break;
                case 12:     // IN(12, "包含查询"),   // 3.4+
                    // 切分属性值为集合
                    String[] values = startVal.split(",|;|、|，|；"); // 支持的分隔符：中英文的逗号分号，和中文的顿号！
                    List<String> valueList = Arrays.asList(values);
                    // 日期类型特殊处理
                    if (dateTypeList.contains(fieldType)) {
                        if (fieldType.equalsIgnoreCase("Date")) {
                            List<Date> valueDateList = new ArrayList<>();
                            valueList.forEach(v -> {
                                valueDateList.add(DateTimeUtil.getDateFromString(v));
                            });
                            Expression<Date> exp = root.<Date>get(key);
                            fieldP = exp.in(valueDateList);
                        } else if (fieldType.equalsIgnoreCase("LocalDateTime")) {
                            List<LocalDateTime> valueDateList = new ArrayList<>();
                            valueList.forEach(v -> {
                                valueDateList.add(DateTimeUtil.getLocalDateTimeFromString(v));
                            });
                            Expression<LocalDateTime> exp = root.<LocalDateTime>get(key);
                            fieldP = exp.in(valueDateList);
                        } else if (fieldType.equalsIgnoreCase("LocalDate")) {
                            List<LocalDate> valueDateList = new ArrayList<>();
                            valueList.forEach(v -> {
                                valueDateList.add(DateTimeUtil.getLocalDateFromString(v));
                            });
                            Expression<LocalDate> exp = root.<LocalDate>get(key);
                            fieldP = exp.in(valueDateList);
                        } else if (fieldType.equalsIgnoreCase("LocalTime")) {
                            List<LocalTime> valueDateList = new ArrayList<>();
                            valueList.forEach(v -> {
                                valueDateList.add(DateTimeUtil.getLocalTimeFromString(v));
                            });
                            Expression<LocalTime> exp = root.<LocalTime>get(key);
                            fieldP = exp.in(valueDateList);
                        }
                    } else {
                        Expression exp = root.get(key);
                        fieldP = exp.in(valueList);
                    }
                    break;
                case 13:     // NOT_IN(13, "不包含查询"),   // 不支持，啥也不做
                    break;
                case 14:     // IS_EMPTY_STRING(14, "空串查询"),
                    fieldP = cb.equal(root.get(key), "");
                    break;
                case 15:     // NOT_EMPTY_STRING(15, "非空串查询")
                    fieldP = cb.notEqual(root.get(key), "");
                    break;
                default:
                    // 0 或其他情况,则为模糊查询,FUZZY(0, "模糊查询"),
                    fieldP = cb.like(root.get(key).as(String.class), "%" + startVal + "%");
            }

            if (i == 0) { // 第一个直接赋值
                finalPredicat = fieldP;
            } else {
                // 获取当前条件的逻辑类型,即和上一个条件之间的关系，是或还是与
                Integer logicalType = field.getLogicalType();
                if (logicalType == ConditionRelationEnum.AND.getValue()) {
                    finalPredicat = cb.and(finalPredicat, fieldP);
                } else { // 其他为 logicalType == ConditionRelationEnum.OR.getValue()
                    finalPredicat = cb.or(finalPredicat, fieldP);
                }
            }
        }

        return finalPredicat;
    }



    /**
     * Description:利用反射获取属性的类型
     *
     * @return java.lang.String
     */
    private String getPropType(String key, T entity) {
        Class clazz = entity.getClass();
        List<Field> fieldList = new ArrayList<>();  //存
        while (clazz != null) {
            fieldList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
            clazz = clazz.getSuperclass();
        }
        for (Field field : fieldList) {
            if (field.getName().equals(key)) {
                return field.getType().getSimpleName();
            }
        }

        return "";
    }


    // 将查询条件的值转换为对应类型的值
    private Number convertType4PropertyValue(String type, String value) {
        if ("Byte".equalsIgnoreCase(type)) {
            return Byte.valueOf(value);
        } else if ("Short".equalsIgnoreCase(type)) {
            return Short.valueOf(value);
        } else if ("int".equals(type) || "Integer".equals(type)) {
            return Integer.valueOf(value);
        } else if ("Long".equalsIgnoreCase(type)) {
                return Long.valueOf(value);
        } else if ("Float".equalsIgnoreCase(type)) {
            return Float.valueOf(value);
        } else if ("Double".equalsIgnoreCase(type)) {
            return Double.valueOf(value);
        } else if ("BigInteger".equalsIgnoreCase(type)) {
            return new BigInteger(value);
        } else if ("BigDecimal".equalsIgnoreCase(type)) {
            return new BigDecimal(value);
        } else {
            return null;
        }
    }

    /**
     * 专门用于处理Number子类的区间查询条件的生成，此处之所以枚举类型，是因为内置的不支持这种的泛型！
     *
     * @param root
     * @param cb
     * @param key
     * @param fieldType
     * @param startVal
     * @param endVal
     * @return
     */
    private Predicate getPredicate4NumberBetweenConditiong(Root<T> root, CriteriaBuilder cb, String key, String fieldType, String startVal, String endVal) {
        if ("Byte".equalsIgnoreCase(fieldType)) {
            Byte start = Byte.valueOf(startVal);
            Byte end = Byte.valueOf(endVal);
            return end >= start ? cb.between(root.<Byte>get(key), start, end) : cb.between(root.<Byte>get(key), end, start);
        } else if ("Short".equalsIgnoreCase(fieldType)) {
            Short start = Short.valueOf(startVal);
            Short end = Short.valueOf(endVal);
            return end >= start ? cb.between(root.<Short>get(key), start, end) : cb.between(root.<Short>get(key), end, start);
        } else if ("int".equals(fieldType) || "Integer".equals(fieldType)) {
            Integer start = Integer.valueOf(startVal);
            Integer end = Integer.valueOf(endVal);
            return end >= start ? cb.between(root.<Integer>get(key), start, end) : cb.between(root.<Integer>get(key), end, start);
        } else if ("Long".equalsIgnoreCase(fieldType)) {
            Long start = Long.valueOf(startVal);
            Long end = Long.valueOf(endVal);
            return end >= start ? cb.between(root.<Long>get(key), start, end) : cb.between(root.<Long>get(key), end, start);
        } else if ("Float".equalsIgnoreCase(fieldType)) {
            Float start = Float.valueOf(startVal);
            Float end = Float.valueOf(endVal);
            return end >= start ? cb.between(root.<Float>get(key), start, end) : cb.between(root.<Float>get(key), end, start);
        } else if ("Double".equalsIgnoreCase(fieldType)) {
            Double start = Double.valueOf(startVal);
            Double end = Double.valueOf(endVal);
            return end >= start ? cb.between(root.<Double>get(key), start, end) : cb.between(root.<Double>get(key), end, start);
        } else if ("BigInteger".equalsIgnoreCase(fieldType)) {
            BigInteger start = new BigInteger(startVal);
            BigInteger end = new BigInteger(endVal);
            return end.compareTo(start) > 0 ? cb.between(root.<BigInteger>get(key), start, end) : cb.between(root.<BigInteger>get(key), end, start);
        } else if ("BigDecimal".equalsIgnoreCase(fieldType)) {
            BigDecimal start = new BigDecimal(startVal);
            BigDecimal end = new BigDecimal(endVal);
            return end.compareTo(start) > 0 ? cb.between(root.<BigDecimal>get(key), start, end) : cb.between(root.<BigDecimal>get(key), end, start);
        } else {
            return null;
        }
    }
}
