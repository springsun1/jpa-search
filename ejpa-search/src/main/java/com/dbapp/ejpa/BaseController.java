package com.dbapp.ejpa;

import com.dbapp.ejpa.entity.BaseSearchEntity;
import com.dbapp.ejpa.impl.BaseServiceImpl;
import com.dbapp.ejpa.repository.BaseRepository;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import vip.efactory.common.base.entity.BaseSearchField;
import vip.efactory.common.base.enums.SearchTypeEnum;
import vip.efactory.common.base.page.EPage;
import vip.efactory.common.base.utils.CommUtil;
import vip.efactory.common.base.utils.R;
import vip.efactory.common.i18n.enums.CommDBEnum;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BaseController<T1 extends BaseSearchEntity, T2 extends BaseRepository, ID> {

    /**
     * T1实体对应的Service,在子类控制器则不需要再次注入了
     */
    @Autowired
    public T2 entityService;

    /**
     * Description:获取T的Class对象是关键，看构造方法
     */
    private Class<T1> clazz = null;

    /**
     * Description:无参构造函数，获得T1的clazz对象
     */
    public BaseController() {
        //为了得到T1的Class，采用如下方法
        //1得到该泛型类的子类对象的Class对象
        Class clz = this.getClass();
        //2得到子类对象的泛型父类类型（也就是BaseDaoImpl<T>）
        ParameterizedType type = (ParameterizedType) clz.getGenericSuperclass();
//        System.out.println(type);
        //
        Type[] types = type.getActualTypeArguments();
        clazz = (Class<T1>) types[0];
        //System.out.println(clazz.getSimpleName());
    }

    /**
     * Description:实体的分页查询，包括排序等,使用SpringData自己的对象接收分页参数
     *
     * @param page 分页参数对象
     * @return R
     */
    public R getByPage(Pageable page) {
        Page<T1> entities = entityService.findAll(page);
        EPage ePage = new EPage(entities);
        return R.ok().setData(ePage);
    }

    /**
     * Description: 高级查询加分页功能
     *
     * @param page   分页参数对象
     * @param entity 包含高级查询条件的实体
     * @return R
     */
    public R advancedQueryByPage(Pageable page, T1 entity) {
        Page<T1> entities;
        BaseServiceImpl<T1> accountBaseService = new BaseServiceImpl<T1>();
        if (entity.getConditions() != null && entity.getConditions().size() > 0) {
            //构造动态查询的条件
            Specification<T1> specification = accountBaseService.getSpecification(entity);
            entities = entityService.findAll(specification, page);
        } else {
            entities = entityService.findAll(page);
        }
        EPage ePage = new EPage(entities);
        return R.ok().setData(ePage);
    }

    /**
     * Description: 高级查询不分页
     *
     * @param entity 包含高级查询条件的实体
     * @return R
     */
    public R advancedQuery(T1 entity) {
        BaseServiceImpl<T1> accountBaseService = new BaseServiceImpl<T1>();

        if (entity.getConditions() != null && entity.getConditions().size() > 0) {
            Specification<T1> specification = accountBaseService.getSpecification(entity);
            List<T1> entities = entityService.findAll(specification);
            return R.ok().setData(entities);
        }else{
            return R.ok().setData(new ArrayList<>());
        }


    }

    /**
     * Description: 同一个值,在多个字段中模糊查询,不分页
     *
     * @param q      模糊查询的值
     * @param fields 例如:"name,address,desc",对这三个字段进行模糊匹配
     * @return R
     */
    public R queryMultiField(String q, String fields) {
        if (StringUtils.isEmpty(fields)){
            return R.error(CommDBEnum.SELECT_PROPERTY_NAME_NOT_EMPTY);
        }
        // 构造高级查询条件
        T1 be = buildQueryConditions(q, fields);

        if (be.getConditions() != null && be.getConditions().size() > 0) {
            BaseServiceImpl<T1> accountBaseService = new BaseServiceImpl<T1>();
            Specification<T1> specification = accountBaseService.getSpecification(be);
            List<T1> entities = entityService.findAll(specification);
            return R.ok().setData(entities);
        } else {
            //返回前25条数据！
//            return br.findTop25ByOrderByIdDesc();
            return R.ok().setData(new ArrayList<>());
        }
    }




    /**
     * Description:同一个值,在多个字段中模糊查询,分页
     *
     * @param q      模糊查询的值
     * @param fields 例如:"name,address,desc",对这三个字段进行模糊匹配
     * @param page   分页参数对象
     * @return R
     */
    public R queryMultiField(String q, String fields, Pageable page) {
        if (StringUtils.isEmpty(fields)){
            return R.error(CommDBEnum.SELECT_PROPERTY_NAME_NOT_EMPTY);
        }
        // 构造高级查询条件
        T1 be = buildQueryConditions(q, fields);
        if (be.getConditions() != null && be.getConditions().size() > 0) {
            BaseServiceImpl<T1> accountBaseService = new BaseServiceImpl<T1>();
            Specification<T1> specification = accountBaseService.getSpecification(be);
            Page<T1> entities = entityService.findAll(specification,page);
            EPage ePage = new EPage(entities);
            return R.ok().setData(ePage);
        } else {
            //返回前25条数据！
//            return br.findTop25ByOrderByIdDesc();
            Page<T1> entities = entityService.findAll(page);
            EPage ePage = new EPage(entities);
            return R.ok().setData(ePage);
        }
    }

    /**
     * Description:根据查询值及多字段,来构建高级查询条件
     *
     * @param q      查询额值
     * @param fields 需要模糊匹配的字段，支持的分隔符：中英文的逗号分号，和中文的顿号！
     * @return T1 当前的泛型实体, 包含高级查询参数
     */
    @SneakyThrows
    private T1 buildQueryConditions(String q, String fields) {
        // 如果q不为空,则构造高级查询条件
        T1 entity = clazz.newInstance();
        if (!CommUtil.isMutiHasNull(q, fields)) {
            Set<BaseSearchField> conditions = new HashSet<>();
            // 判断filds是一个字段还是多个字段,若是多个字段则进行切分
            // 切分属性值为集合，支持的分隔符：中英文的逗号分号，和中文的顿号！
            String[] rawFields = fields.split(",|;|、|，|；");
            for (String c : rawFields) {
                // 构建默认OR的多字段模糊查询
                BaseSearchField condition = new BaseSearchField();
                condition.setName(c);
                condition.setSearchType(SearchTypeEnum.FUZZY.getValue());
                condition.setVal(q);
                conditions.add(condition);
            }
            entity.setConditions(conditions);
            return entity;
        }
        return null;
    }


}
