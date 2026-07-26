package com.commerceflow.mall.order;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderEvidenceMapper {
    OrderExecutionEvidenceDto findByOrderNo(@Param("orderNo") String orderNo);
}
