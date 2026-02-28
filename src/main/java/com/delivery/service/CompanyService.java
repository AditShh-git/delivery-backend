package com.delivery.service;

import com.delivery.dto.request.CreateCompanyRequest;
import com.delivery.dto.response.CompanyResponse;

import java.util.List;

public interface CompanyService {

    CompanyResponse createCompany(CreateCompanyRequest request);

    List<CompanyResponse> getAllCompanies();

    CompanyResponse getCompany(Long id);
}
