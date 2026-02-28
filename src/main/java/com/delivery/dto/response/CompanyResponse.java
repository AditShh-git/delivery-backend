package com.delivery.dto.response;

import java.util.List;

public record CompanyResponse(
        Long id,
        String name,
        String contact,
        List<PolicyResponse> policies
) {}