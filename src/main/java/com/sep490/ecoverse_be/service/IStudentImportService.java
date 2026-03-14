package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.AddStudentManualRequest;
import com.sep490.ecoverse_be.dto.response.AccountListResponse;
import com.sep490.ecoverse_be.dto.response.ImportResultResponse;
import com.sep490.ecoverse_be.dto.response.SendCredentialSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IStudentImportService {

    ImportResultResponse importStudentsFromExcel(MultipartFile file);

    void addStudentManually(AddStudentManualRequest request);

        AccountListResponse getImportedAccounts();

        SendCredentialSummaryResponse sendCredentialEmails();

        void resendCredentialEmail(UUID parentId);
}
