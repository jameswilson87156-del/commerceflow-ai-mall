package com.commerceflow.mall.ai;

public interface CustomerServiceProviderClient {
    AiModels.PythonCustomerServiceResponse answer(AiModels.PythonCustomerServiceRequest request);
}
