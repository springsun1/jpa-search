package com.dbapp.ejpa.repository;

import com.dbapp.ejpa.entity.BaseSearchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Description:项目自定义的一些常用的扩展
 *
 * @author dbdu
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseSearchEntity, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
}
