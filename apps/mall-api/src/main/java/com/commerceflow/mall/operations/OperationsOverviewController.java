package com.commerceflow.mall.operations;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationsOverviewController {
    private final OperationsOverviewService service;
    public OperationsOverviewController(OperationsOverviewService service) { this.service = service; }
    @GetMapping("/overview")
    public OperationsOverviewDto overview() { return service.overview(); }
}
