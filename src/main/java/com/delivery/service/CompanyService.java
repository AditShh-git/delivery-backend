package com.delivery.service;

import com.delivery.dto.request.CompanyOnboardRequest;
import com.delivery.dto.request.CreateCompanyRequest;
import com.delivery.dto.response.CompanyDashboardResponse;
import com.delivery.dto.response.CompanyResponse;
import com.delivery.entity.CompanyStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CompanyService {

    CompanyResponse createEnterpriseCompany(CreateCompanyRequest request);

    CompanyResponse updateCompanyStatus(Long id, CompanyStatus status);

    Page<CompanyResponse> getCompanies(int page,
                                       int size,
                                       CompanyStatus status,
                                       String search);

    CompanyResponse getCompany(Long id);

    CompanyResponse onboardCompany(Long companyId, CompanyOnboardRequest request);

    CompanyDashboardResponse getDashboard(Long companyId);

    CompanyDashboardResponse getDashboardByEmail(String email);
}
