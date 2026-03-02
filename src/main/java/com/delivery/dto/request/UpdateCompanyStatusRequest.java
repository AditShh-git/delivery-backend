package com.delivery.dto.request;

import com.delivery.entity.CompanyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCompanyStatusRequest(
        @NotNull
        CompanyStatus status
) {}
