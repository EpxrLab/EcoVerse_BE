package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.AccountListResponse;
import com.sep490.ecoverse_be.dto.response.ImportResultResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IStudentImportService {

    ImportResultResponse importStudentsFromExcel(MultipartFile file);

    AccountListResponse getImportedAccounts();

    void sendCredentialEmails();
}
