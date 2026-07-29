package com.commerceflow.mall.order;

import javax.sql.DataSource;
import com.commerceflow.mall.operations.OperationsOverviewMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackageClasses = {OrderEvidenceMapper.class, OperationsOverviewMapper.class})
public class OrderEvidenceMybatisConfig {
    @Bean
    SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mappers/*.xml"));
        return factory.getObject();
    }
}
