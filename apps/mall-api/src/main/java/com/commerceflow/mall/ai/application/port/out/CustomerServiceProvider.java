package com.commerceflow.mall.ai.application.port.out;

import com.commerceflow.mall.ai.AiModels;

/**
 * Outbound boundary for a product customer-service provider.
 *
 * <p>The AI application service owns facts, validation, evidence and trace
 * persistence. Provider adapters only translate the typed request to an
 * external protocol and return a typed suggestion.</p>
 */
public interface CustomerServiceProvider {
    AiModels.PythonCustomerServiceResponse answer(AiModels.PythonCustomerServiceRequest request);
}
