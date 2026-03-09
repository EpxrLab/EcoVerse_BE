package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;
import com.sep490.ecoverse_be.entity.Partnership;
import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.ApprovalStatus;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.PartnershipRepository;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.IAdminService;
import com.sep490.ecoverse_be.service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements IAdminService {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IEmailService emailService;

    private User getCurrentAdmin() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getUser();
    }

    @Override
    public List<SchoolDetailResponse> getPendingSchools() {
        return schoolRepository.findByApprovalStatus(ApprovalStatus.PENDING)
                .stream()
                .map(this::mapToSchoolDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartnershipDetailResponse> getPendingPartnerships() {
        return partnershipRepository.findByApprovalStatus(ApprovalStatus.PENDING)
                .stream()
                .map(this::mapToPartnershipDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SchoolDetailResponse updateSchoolApproval(
            UUID id,
            UpdateApprovalRequest request
    ) {

        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học với id: " + id));

        if (school.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Trường học không ở trạng thái chờ duyệt.");
        }

        if (request.getStatus() == ApprovalStatus.REJECTED &&
                (request.getReason() == null || request.getReason().isBlank())) {
            throw new BadRequestException("Phải cung cấp lý do khi từ chối.");
        }

        school.setApprovalStatus(request.getStatus());
        school.setApprovedBy(getCurrentAdmin());
        school.setApprovedAt(LocalDateTime.now());

        if (request.getStatus() == ApprovalStatus.APPROVED) {

            User schoolUser = school.getUser();
            schoolUser.setStatus(AccountStatus.ACTIVE);

            emailService.sendApprovalEmail(
                    school.getContactEmail(),
                    school.getSchoolName()
            );

        } else {

            emailService.sendRejectionEmail(
                    school.getContactEmail(),
                    school.getSchoolName(),
                    request.getReason()
            );
        }

        return mapToSchoolDetailResponse(school);
    }

    @Override
    @Transactional
    public PartnershipDetailResponse updatePartnershipApproval(
            UUID id,
            UpdateApprovalRequest request
    ) {

        Partnership partnership = partnershipRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đối tác với id: " + id));

        if (partnership.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Đối tác không ở trạng thái chờ duyệt.");
        }

        if (request.getStatus() == ApprovalStatus.REJECTED &&
                (request.getReason() == null || request.getReason().isBlank())) {
            throw new BadRequestException("Phải cung cấp lý do khi từ chối.");
        }

        partnership.setApprovalStatus(request.getStatus());
        partnership.setApprovedBy(getCurrentAdmin());
        partnership.setApprovedAt(LocalDateTime.now());

        if (request.getStatus() == ApprovalStatus.APPROVED) {

            User schoolUser = partnership.getUser();
            schoolUser.setStatus(AccountStatus.ACTIVE);

            emailService.sendApprovalEmail(
                    partnership.getContactEmail(),
                    partnership.getOrganizationName()
            );

        } else {

            emailService.sendRejectionEmail(
                    partnership.getContactEmail(),
                    partnership.getOrganizationName(),
                    request.getReason()
            );
        }

        return mapToPartnershipDetailResponse(partnership);
    }

    @Override
    public List<SchoolDetailResponse> getApprovedSchools() {
        return schoolRepository.findByApprovalStatus(ApprovalStatus.APPROVED)
                .stream()
                .map(this::mapToSchoolDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartnershipDetailResponse> getApprovedPartnerships() {
        return partnershipRepository.findByApprovalStatus(ApprovalStatus.APPROVED)
                .stream()
                .map(this::mapToPartnershipDetailResponse)
                .collect(Collectors.toList());
    }


    @Override
    public void updateUserStatus(UUID userId, boolean isActive) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if(isActive){
            user.setStatus(AccountStatus.ACTIVE);
            user.setIsActive(true);
        }else{
            user.setStatus(AccountStatus.INACTIVE);
            user.setIsActive(false);
        }
        userRepository.save(user);
    }


    private SchoolDetailResponse mapToSchoolDetailResponse(School school) {
        return SchoolDetailResponse.builder()
                .id(school.getId())
                .userId(school.getUser().getId().toString())
                .schoolName(school.getSchoolName())
                .schoolType(school.getSchoolType())
                .taxCode(school.getTaxCode())
                .contactEmail(school.getContactEmail())
                .phoneNumber(school.getPhoneNumber())
                .address(school.getAddress())
                .district(school.getDistrict())
                .province(school.getProvince())
                .principalName(school.getPrincipalName())
                .position(school.getPosition())
                .logoUrl(school.getLogoUrl())
                .licenseUrl(school.getLicenseUrl())
                .approvalStatus(school.getApprovalStatus())
                .createdAt(school.getCreatedAt())
                .build();
    }

    private PartnershipDetailResponse mapToPartnershipDetailResponse(Partnership partnership) {
        return PartnershipDetailResponse.builder()
                .id(partnership.getId())
                .userId(partnership.getUser().getId().toString())
                .organizationName(partnership.getOrganizationName())
                .partnershipType(partnership.getPartnershipType())
                .taxCode(partnership.getTaxCode())
                .contactEmail(partnership.getContactEmail())
                .phoneNumber(partnership.getPhoneNumber())
                .registeredAddress(partnership.getRegisteredAddress())
                .geographicScopeDistrict(partnership.getGeographicScopeDistrict())
                .geographicScopeProvince(partnership.getGeographicScopeProvince())
                .contactPerson(partnership.getContactPerson())
                .position(partnership.getPosition())
                .logoUrl(partnership.getLogoUrl())
                .licenseUrl(partnership.getLicenseUrl())
                .approvalStatus(partnership.getApprovalStatus())
                .createdAt(partnership.getCreatedAt())
                .build();
    }
}
