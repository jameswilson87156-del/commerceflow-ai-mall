package com.commerceflow.mall.order.application;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.order.OrderEvidenceMapper;
import com.commerceflow.mall.order.OrderExecutionEvidenceDto;
import com.commerceflow.mall.order.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Shared read application service for customer-owned and operator-wide order views. */
@Service
public class OrderReadService {
    private final OrderRepository repository;
    private final OrderEvidenceMapper evidenceMapper;

    public OrderReadService(OrderRepository repository, OrderEvidenceMapper evidenceMapper) {
        this.repository = repository;
        this.evidenceMapper = evidenceMapper;
    }

    public List<ApiModels.OrderSummary> listForUser(long userId) {
        return repository.findAll(userId);
    }

    public List<ApiModels.OrderSummary> listForOperator() {
        return repository.findAllForOperator();
    }

    public ApiModels.OrderSummary detailForUser(long userId, String orderNo) {
        return repository.findForUser(userId, orderNo).orElseThrow(this::notFound);
    }

    public ApiModels.OrderSummary detailForOperator(String orderNo) {
        return repository.find(orderNo).orElseThrow(this::notFound);
    }

    public OrderExecutionEvidenceDto evidenceForUser(long userId, String orderNo) {
        detailForUser(userId, orderNo);
        return evidenceForOrder(orderNo);
    }

    public OrderExecutionEvidenceDto evidenceForOperator(String orderNo) {
        detailForOperator(orderNo);
        return evidenceForOrder(orderNo);
    }

    private OrderExecutionEvidenceDto evidenceForOrder(String orderNo) {
        OrderExecutionEvidenceDto evidence = evidenceMapper.findByOrderNo(orderNo);
        if (evidence == null) throw notFound();
        return evidence;
    }

    private CommerceException notFound() {
        return new CommerceException("ORDER_NOT_FOUND", "Order not found");
    }
}
