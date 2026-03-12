package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.AddStudentManualRequest;
import com.sep490.ecoverse_be.dto.response.AccountListResponse;
import com.sep490.ecoverse_be.dto.response.ImportResultResponse;
import com.sep490.ecoverse_be.dto.response.SendCredentialSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IStudentImportService {

    ImportResultResponse importStudentsFromExcel(MultipartFile file);

    // Them hoc sinh va phu huynh thu cong (khong qua Excel)
    void addStudentManually(AddStudentManualRequest request);

    AccountListResponse getImportedAccounts();

    // Gui email chi cho phu huynh chua nhan (credentialEmailSent = false)
    SendCredentialSummaryResponse sendCredentialEmails();

    // Gui lai email cho mot phu huynh cu the (reset password + gui lai)
    void resendCredentialEmail(UUID parentId);
}
