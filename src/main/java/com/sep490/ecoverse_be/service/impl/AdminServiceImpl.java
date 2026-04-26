package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.UpdateApprovalRequest;
import com.sep490.ecoverse_be.dto.request.AdminGameLevelPresetUpsertRequest;
import com.sep490.ecoverse_be.dto.request.AdminGameLevelPresetItemUpsertRequest;
import com.sep490.ecoverse_be.dto.request.AdminGameTypeUpsertRequest;
import com.sep490.ecoverse_be.dto.request.AdminWasteItemUpsertRequest;
import com.sep490.ecoverse_be.dto.request.AdminWasteSubCategoryUpsertRequest;
import com.sep490.ecoverse_be.dto.response.AdminCampaignAnalyticsResponse;
import com.sep490.ecoverse_be.dto.response.AdminGameLevelPresetItemResponse;
import com.sep490.ecoverse_be.dto.response.AdminGameLevelPresetResponse;
import com.sep490.ecoverse_be.dto.response.AdminGameTypeResponse;
import com.sep490.ecoverse_be.dto.response.AdminUserListResponse;
import com.sep490.ecoverse_be.dto.response.AdminWasteItemResponse;
import com.sep490.ecoverse_be.dto.response.AdminWasteSubCategoryResponse;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.ParentAdminDetail;
import com.sep490.ecoverse_be.dto.response.PartnershipDetailResponse;
import com.sep490.ecoverse_be.dto.response.SchoolDetailResponse;
import com.sep490.ecoverse_be.dto.response.StudentAdminDetail;
import com.sep490.ecoverse_be.entity.Parent;
import com.sep490.ecoverse_be.entity.Partnership;
import com.sep490.ecoverse_be.entity.Payment;
import com.sep490.ecoverse_be.entity.Campaign;
import com.sep490.ecoverse_be.entity.GameLevelPreset;
import com.sep490.ecoverse_be.entity.GameLevelPresetItem;
import com.sep490.ecoverse_be.entity.GameType;
import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.Student;
import com.sep490.ecoverse_be.entity.StudentParentLink;
import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.entity.SubscriptionPlan;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.entity.WasteItem;
import com.sep490.ecoverse_be.entity.WasteSubCategory;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.ApprovalStatus;
import com.sep490.ecoverse_be.enums.PaymentMethod;
import com.sep490.ecoverse_be.enums.PaymentStatus;
import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import com.sep490.ecoverse_be.enums.PartnershipCampaignStatus;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.enums.SchoolCampaignStatus;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import com.sep490.ecoverse_be.enums.WasteCategory;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.ParentRepository;
import com.sep490.ecoverse_be.repository.PartnershipRepository;
import com.sep490.ecoverse_be.repository.PaymentRepository;
import com.sep490.ecoverse_be.repository.CampaignParticipantRepository;
import com.sep490.ecoverse_be.repository.CampaignRepository;
import com.sep490.ecoverse_be.repository.CampaignSchoolParticipateRepository;
import com.sep490.ecoverse_be.repository.GameTypeRepository;
import com.sep490.ecoverse_be.repository.GameLevelPresetRepository;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import com.sep490.ecoverse_be.repository.StudentRepository;
import com.sep490.ecoverse_be.repository.SubscriptionPlanRepository;
import com.sep490.ecoverse_be.repository.SubscriptionRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.repository.WasteItemRepository;
import com.sep490.ecoverse_be.repository.WasteSubCategoryRepository;
import com.sep490.ecoverse_be.service.IAdminService;
import com.sep490.ecoverse_be.service.IEmailService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements IAdminService {

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private GameTypeRepository gameTypeRepository;

    @Autowired
    private GameLevelPresetRepository gameLevelPresetRepository;

    @Autowired
    private WasteSubCategoryRepository wasteSubCategoryRepository;

    @Autowired
    private WasteItemRepository wasteItemRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignParticipantRepository campaignParticipantRepository;

    @Autowired
    private CampaignSchoolParticipateRepository campaignSchoolParticipateRepository;

    @Autowired
    private TripoApiService tripoApiService;

    private User getCurrentAdmin() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getUser();
    }

    @Override
    public PageResponse<SchoolDetailResponse> getPendingSchools(String keyword, Pageable pageable) {
        Specification<School> spec = buildSchoolSpec(ApprovalStatus.PENDING, keyword);
        Page<School> page = schoolRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToSchoolDetailResponse);
    }

    @Override
    public PageResponse<PartnershipDetailResponse> getPendingPartnerships(String keyword, Pageable pageable) {
        Specification<Partnership> spec = buildPartnershipSpec(ApprovalStatus.PENDING, keyword);
        Page<Partnership> page = partnershipRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToPartnershipDetailResponse);
    }

    @Override
    public PageResponse<SchoolDetailResponse> getApprovedSchools(String keyword, Pageable pageable) {
        Specification<School> spec = buildSchoolSpec(ApprovalStatus.APPROVED, keyword);
        Page<School> page = schoolRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToSchoolDetailResponse);
    }

    @Override
    public PageResponse<PartnershipDetailResponse> getApprovedPartnerships(String keyword, Pageable pageable) {
        Specification<Partnership> spec = buildPartnershipSpec(ApprovalStatus.APPROVED, keyword);
        Page<Partnership> page = partnershipRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToPartnershipDetailResponse);
    }

    @Override
    public PageResponse<SchoolDetailResponse> getAllSchools(String keyword, Pageable pageable) {
        Specification<School> spec = buildSchoolSpec(null, keyword);
        Page<School> page = schoolRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToSchoolDetailResponse);
    }

    @Override
    public PageResponse<PartnershipDetailResponse> getAllPartnerships(String keyword, Pageable pageable) {
        Specification<Partnership> spec = buildPartnershipSpec(null, keyword);
        Page<Partnership> page = partnershipRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::mapToPartnershipDetailResponse);
    }

    @Override
    @Transactional
    public AdminGameTypeResponse createGameType(AdminGameTypeUpsertRequest request) {
        if (gameTypeRepository.existsByTypeCodeAndIsDeleteFalse(request.getTypeCode())) {
            throw new BadRequestException("Game type code đã tồn tại");
        }
        if (gameTypeRepository.existsByNameIgnoreCaseAndIsDeleteFalse(request.getName())) {
            throw new BadRequestException("Tên game type đã tồn tại");
        }

        GameType gameType = new GameType();
        gameType.setTypeCode(request.getTypeCode());
        gameType.setName(request.getName());
        gameType.setShortDescription(request.getShortDescription());
        gameType.setFullDescription(request.getFullDescription());
        gameType.setHowToPlay(request.getHowToPlay());
        gameType.setThumbnailUrl(request.getThumbnailUrl());
        gameType.setIconUrl(request.getIconUrl());
        gameType.setFeatures(request.getFeatures() != null ? request.getFeatures() : new LinkedHashMap<>());
        gameType.setSupportsCoin(request.getSupportsCoin() == null || request.getSupportsCoin());
        gameType.setMaxLevels(request.getMaxLevels() != null ? request.getMaxLevels() : 1);
        gameType.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        gameType.setCreatedBy(getCurrentAdmin());
        gameType.setUpdatedBy(getCurrentAdmin());
        gameType = gameTypeRepository.save(gameType);
        return mapGameType(gameType);
    }

    @Override
    @Transactional
    public AdminGameTypeResponse updateGameType(UUID id, AdminGameTypeUpsertRequest request) {
        GameType gameType = getActiveGameTypeOrThrow(id);

        if (!gameType.getTypeCode().equals(request.getTypeCode())
                && gameTypeRepository.existsByTypeCodeAndIsDeleteFalse(request.getTypeCode())) {
            throw new BadRequestException("Game type code đã tồn tại");
        }
        if (!gameType.getName().equalsIgnoreCase(request.getName())
                && gameTypeRepository.existsByNameIgnoreCaseAndIsDeleteFalse(request.getName())) {
            throw new BadRequestException("Tên game type đã tồn tại");
        }

        gameType.setTypeCode(request.getTypeCode());
        gameType.setName(request.getName());
        gameType.setShortDescription(request.getShortDescription());
        gameType.setFullDescription(request.getFullDescription());
        gameType.setHowToPlay(request.getHowToPlay());
        gameType.setThumbnailUrl(request.getThumbnailUrl());
        gameType.setIconUrl(request.getIconUrl());
        if (request.getFeatures() != null) {
            gameType.setFeatures(request.getFeatures());
        }
        if (request.getSupportsCoin() != null) {
            gameType.setSupportsCoin(request.getSupportsCoin());
        }
        if (request.getMaxLevels() != null) {
            gameType.setMaxLevels(request.getMaxLevels());
        }
        if (request.getDisplayOrder() != null) {
            gameType.setDisplayOrder(request.getDisplayOrder());
        }
        gameType.setUpdatedBy(getCurrentAdmin());
        return mapGameType(gameTypeRepository.save(gameType));
    }

    @Override
    @Transactional
    public void deleteGameType(UUID id) {
        GameType gameType = getActiveGameTypeOrThrow(id);
        gameType.setDelete(true);
        gameType.setUpdatedBy(getCurrentAdmin());
        gameTypeRepository.save(gameType);
    }

    @Override
    public List<AdminGameTypeResponse> getGameTypes() {
        return gameTypeRepository.findByIsDeleteFalse().stream().map(this::mapGameType).toList();
    }

    @Override
    public AdminGameTypeResponse getGameTypeById(UUID id) {
        GameType gameType = getActiveGameTypeOrThrow(id);
        return mapGameType(gameType);
    }

    @Override
    @Transactional
    public AdminGameLevelPresetResponse createGameLevelPreset(UUID gameTypeId,
            AdminGameLevelPresetUpsertRequest request) {
        GameType gameType = getActiveGameTypeOrThrow(gameTypeId);
        if (gameLevelPresetRepository.existsByGameTypeIdAndDifficulty(gameTypeId, request.getDifficulty())) {
            throw new BadRequestException("Preset cho difficulty này đã tồn tại");
        }

        GameLevelPreset preset = new GameLevelPreset();
        preset.setGameType(gameType);
        applyPresetUpsert(preset, request);
        return mapGameLevelPreset(gameLevelPresetRepository.save(preset));
    }

    @Override
    @Transactional
    public AdminGameLevelPresetResponse updateGameLevelPreset(UUID gameTypeId, UUID presetId,
            AdminGameLevelPresetUpsertRequest request) {
        getActiveGameTypeOrThrow(gameTypeId);
        GameLevelPreset preset = getPresetOrThrow(gameTypeId, presetId);

        if (gameLevelPresetRepository.existsByGameTypeIdAndDifficultyAndIdNot(gameTypeId, request.getDifficulty(),
                presetId)) {
            throw new BadRequestException("Preset cho difficulty này đã tồn tại");
        }

        applyPresetUpsert(preset, request);
        return mapGameLevelPreset(gameLevelPresetRepository.save(preset));
    }

    @Override
    @Transactional
    public void deleteGameLevelPreset(UUID gameTypeId, UUID presetId) {
        getActiveGameTypeOrThrow(gameTypeId);
        GameLevelPreset preset = getPresetOrThrow(gameTypeId, presetId);
        gameLevelPresetRepository.delete(preset);
    }

    @Override
    public List<AdminGameLevelPresetResponse> getGameLevelPresets(UUID gameTypeId) {
        getActiveGameTypeOrThrow(gameTypeId);
        return gameLevelPresetRepository.findByGameTypeIdOrderByDifficultyAsc(gameTypeId)
                .stream()
                .map(this::mapGameLevelPreset)
                .toList();
    }

    @Override
    public AdminGameLevelPresetResponse getGameLevelPresetById(UUID gameTypeId, UUID presetId) {
        getActiveGameTypeOrThrow(gameTypeId);
        return mapGameLevelPreset(getPresetOrThrow(gameTypeId, presetId));
    }

    @Override
    @Transactional
    public AdminWasteSubCategoryResponse createWasteSubCategory(AdminWasteSubCategoryUpsertRequest request) {
        if (wasteSubCategoryRepository.existsByCategoryAndSubCategoryCodeAndIsDeleteFalse(request.getCategory(),
                request.getSubCategoryCode())) {
            throw new BadRequestException("Sub category code đã tồn tại trong category");
        }
        WasteSubCategory subCategory = new WasteSubCategory();
        subCategory.setCategory(request.getCategory());
        subCategory.setSubCategoryCode(request.getSubCategoryCode());
        subCategory.setDisplayName(request.getDisplayName());
        subCategory.setDescription(request.getDescription());
        subCategory.setIconUrl(request.getIconUrl());
        subCategory.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        subCategory.setActive(request.getIsActive() == null || request.getIsActive());
        subCategory.setCreatedBy(getCurrentAdmin());
        return mapWasteSubCategory(wasteSubCategoryRepository.save(subCategory));
    }

    @Override
    @Transactional
    public AdminWasteSubCategoryResponse updateWasteSubCategory(UUID id, AdminWasteSubCategoryUpsertRequest request) {
        WasteSubCategory subCategory = getActiveWasteSubCategoryOrThrow(id);
        if (wasteSubCategoryRepository.existsByCategoryAndSubCategoryCodeAndIdNotAndIsDeleteFalse(request.getCategory(),
                request.getSubCategoryCode(), id)) {
            throw new BadRequestException("Sub category code đã tồn tại trong category");
        }
        subCategory.setCategory(request.getCategory());
        subCategory.setSubCategoryCode(request.getSubCategoryCode());
        subCategory.setDisplayName(request.getDisplayName());
        subCategory.setDescription(request.getDescription());
        subCategory.setIconUrl(request.getIconUrl());
        if (request.getDisplayOrder() != null) {
            subCategory.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            subCategory.setActive(request.getIsActive());
        }
        return mapWasteSubCategory(wasteSubCategoryRepository.save(subCategory));
    }

    @Override
    @Transactional
    public void deleteWasteSubCategory(UUID id) {
        WasteSubCategory subCategory = getActiveWasteSubCategoryOrThrow(id);
        subCategory.setActive(false);
        wasteSubCategoryRepository.save(subCategory);
    }

    @Override
    public List<AdminWasteSubCategoryResponse> getWasteSubCategories() {
        return wasteSubCategoryRepository.findByIsDeleteFalse().stream().map(this::mapWasteSubCategory).toList();
    }

    @Override
    @Transactional
    public AdminWasteItemResponse createWasteItem(AdminWasteItemUpsertRequest request) {
        WasteSubCategory subCategory = getActiveWasteSubCategoryOrThrow(request.getSubCategoryId());
        if (wasteItemRepository.existsByItemNameIgnoreCaseAndSubCategoryIdAndIsDeleteFalse(request.getItemName(),
                request.getSubCategoryId())) {
            throw new BadRequestException("Waste item đã tồn tại trong sub-category");
        }
        WasteItem wasteItem = new WasteItem();
        wasteItem.setItemName(request.getItemName());
        wasteItem.setSubCategory(subCategory);
        wasteItem.setCategory(subCategory.getCategory());
        wasteItem.setDescription(request.getDescription());
        wasteItem.setFunFact(request.getFunFact());
        wasteItem.setImageUrl(request.getImageUrl());
        wasteItem.setDecompositionTime(request.getDecompositionTime());
        wasteItem.setRecyclingTips(request.getRecyclingTips());
        wasteItem.setActive(request.getIsActive() == null || request.getIsActive());
        wasteItem.setCreatedBy(getCurrentAdmin());

        if (request.getImageUrl() != null && request.getImageUrl().toLowerCase().endsWith(".glb")) {
            wasteItem.setModel3dUrl(null);
            wasteItem.setTripoTaskId(null);
            wasteItem.setTripoStatus(null);
        } else if (Boolean.TRUE.equals(request.getGenerate3dModel()) && request.getImageUrl() != null
                && !request.getImageUrl().isBlank()) {
            try {
                String presignedUrl = s3PresignedUrlService.generatePresignedUrl(request.getImageUrl());
                String taskId = tripoApiService.submitImageTo3dTask(presignedUrl);
                wasteItem.setTripoTaskId(taskId);
                wasteItem.setTripoStatus("PENDING");
            } catch (Exception e) {
                wasteItem.setTripoStatus("FAILED");
                // Log naturally handled in TripoApiService, but fail gracefully for WasteItem
                // setup
            }
        }

        return mapWasteItem(wasteItemRepository.save(wasteItem));
    }

    @Override
    @Transactional
    public AdminWasteItemResponse updateWasteItem(UUID id, AdminWasteItemUpsertRequest request) {
        WasteItem wasteItem = getActiveWasteItemOrThrow(id);
        WasteSubCategory subCategory = getActiveWasteSubCategoryOrThrow(request.getSubCategoryId());
        if (wasteItemRepository.existsByItemNameIgnoreCaseAndSubCategoryIdAndIdNotAndIsDeleteFalse(
                request.getItemName(), request.getSubCategoryId(), id)) {
            throw new BadRequestException("Waste item đã tồn tại trong sub-category");
        }
        wasteItem.setItemName(request.getItemName());
        wasteItem.setSubCategory(subCategory);
        wasteItem.setCategory(subCategory.getCategory());
        wasteItem.setDescription(request.getDescription());
        wasteItem.setFunFact(request.getFunFact());
        wasteItem.setImageUrl(request.getImageUrl());
        wasteItem.setDecompositionTime(request.getDecompositionTime());
        wasteItem.setRecyclingTips(request.getRecyclingTips());
        if (request.getIsActive() != null) {
            wasteItem.setActive(request.getIsActive());
        }

        if (request.getImageUrl() != null && request.getImageUrl().toLowerCase().endsWith(".glb")) {
            wasteItem.setModel3dUrl(null);
            wasteItem.setTripoTaskId(null);
            wasteItem.setTripoStatus(null);
        } else if (Boolean.TRUE.equals(request.getGenerate3dModel()) && request.getImageUrl() != null
                && !request.getImageUrl().isBlank()) {
            try {
                String presignedUrl = s3PresignedUrlService.generatePresignedUrl(request.getImageUrl());
                String taskId = tripoApiService.submitImageTo3dTask(presignedUrl);
                wasteItem.setTripoTaskId(taskId);
                wasteItem.setTripoStatus("PENDING");
            } catch (Exception e) {
                wasteItem.setTripoStatus("FAILED");
            }
        }

        return mapWasteItem(wasteItemRepository.save(wasteItem));
    }

    @Override
    @Transactional
    public void deleteWasteItem(UUID id) {
        WasteItem wasteItem = getActiveWasteItemOrThrow(id);
        wasteItem.setDelete(true);
        wasteItemRepository.save(wasteItem);
    }

    @Override
    public List<AdminWasteItemResponse> getWasteItems() {
        return wasteItemRepository.findByIsDeleteFalse().stream().map(this::mapWasteItem).toList();
    }

    @Override
    public AdminWasteItemResponse getWasteItemById(UUID id) {
        WasteItem wasteItem = getActiveWasteItemOrThrow(id);
        return mapWasteItem(wasteItem);
    }

    @Override
    public AdminCampaignAnalyticsResponse getCampaignAnalytics() {
        Map<String, Long> schoolStatus = new LinkedHashMap<>();
        for (SchoolCampaignStatus status : SchoolCampaignStatus.values()) {
            schoolStatus.put(status.name(), campaignRepository.countBySchoolStatus(status));
        }

        Map<String, Long> partnershipStatus = new LinkedHashMap<>();
        for (PartnershipCampaignStatus status : PartnershipCampaignStatus.values()) {
            partnershipStatus.put(status.name(), campaignRepository.countByPartnershipStatus(status));
        }

        return AdminCampaignAnalyticsResponse.builder()
                .totalCampaigns(campaignRepository.count())
                .totalSchoolCampaigns(
                        campaignRepository.countByCampaignType(CampaignType.SCHOOL_INTERNAL)
                                + campaignRepository.countByCampaignType(CampaignType.INTER_SCHOOL))
                .totalPartnershipCampaigns(campaignRepository.countByCampaignType(CampaignType.PARTNERSHIP_EVENT))
                .totalParticipants(campaignParticipantRepository.countByIsActiveTrue())
                .totalSchoolInvitations(campaignSchoolParticipateRepository.count())
                .approvedSchoolInvitations(
                        campaignSchoolParticipateRepository.countByStatus(ParticipationStatus.APPROVED))
                .schoolCampaignStatusCounts(schoolStatus)
                .partnershipCampaignStatusCounts(partnershipStatus)
                .build();
    }

    @Override
    public PageResponse<AdminUserListResponse> getAllUsers(Role role, UUID schoolId, String keyword,
            Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by role
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            } else {
                // Only return these 4 roles (exclude ADMINISTRATOR)
                predicates.add(root.get("role").in(
                        Role.PARTNERSHIP_SCHOOL,
                        Role.THIRD_PARTY_PARTNERSHIP,
                        Role.STUDENT,
                        Role.PARENT));
            }

            // Keyword search on email/username
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern);
                Predicate usernameLike = cb.like(cb.lower(root.get("username")), pattern);
                predicates.add(cb.or(emailLike, usernameLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> page = userRepository.findAll(spec, pageable);
        return PageResponse.from(page, user -> mapUserToAdminUserResponse(user, schoolId));
    }

    @Override
    @Transactional
    public SchoolDetailResponse updateSchoolApproval(
            UUID id,
            UpdateApprovalRequest request) {

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
        school.setApprovedAt(OffsetDateTime.now());

        if (request.getStatus() == ApprovalStatus.APPROVED) {

            User schoolUser = school.getUser();
            schoolUser.setStatus(AccountStatus.ACTIVE);

            assignFreeSubscription("SCHOOL_FREE", SubscriberType.SCHOOL, school, null, schoolUser);

            emailService.sendApprovalEmail(
                    school.getContactEmail(),
                    school.getSchoolName());

        } else {

            emailService.sendRejectionEmail(
                    school.getContactEmail(),
                    school.getSchoolName(),
                    request.getReason());
        }

        return mapToSchoolDetailResponse(school);
    }

    @Override
    @Transactional
    public PartnershipDetailResponse updatePartnershipApproval(
            UUID id,
            UpdateApprovalRequest request) {

        Partnership partnership = partnershipRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đối tác với id: " + id));

        if (partnership.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Đối tác không ở trạng thái chờ duyệt.");
        }

        if (request.getStatus() == ApprovalStatus.REJECTED &&
                (request.getReason() == null || request.getReason().isBlank())) {
            throw new BadRequestException("Phải cung cấp lý do khi từ chối.");
        }

        partnership.setApprovalStatus(request.getStatus());
        partnership.setApprovedBy(getCurrentAdmin());
        partnership.setApprovedAt(OffsetDateTime.now());

        if (request.getStatus() == ApprovalStatus.APPROVED) {

            User schoolUser = partnership.getUser();
            schoolUser.setStatus(AccountStatus.ACTIVE);

            assignFreeSubscription("PARTNERSHIP_FREE", SubscriberType.PARTNERSHIP, null, partnership, schoolUser);

            emailService.sendApprovalEmail(
                    partnership.getContactEmail(),
                    partnership.getOrganizationName());

        } else {

            emailService.sendRejectionEmail(
                    partnership.getContactEmail(),
                    partnership.getOrganizationName(),
                    request.getReason());
        }

        return mapToPartnershipDetailResponse(partnership);
    }

    @Override
    public void updateUserStatus(UUID userId, boolean isActive) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (isActive) {
            user.setStatus(AccountStatus.ACTIVE);
            user.setIsActive(true);
        } else {
            user.setStatus(AccountStatus.SUSPENDED);
            user.setIsActive(false);
        }
        userRepository.save(user);
    }

    private SchoolDetailResponse mapToSchoolDetailResponse(School school) {
        User user = school.getUser();
        return SchoolDetailResponse.builder()
                .id(school.getId())
                .userId(user.getId().toString())
                .schoolName(school.getSchoolName())
                .schoolType(school.getSchoolType())
                .taxCode(school.getTaxCode())
                .contactEmail(school.getContactEmail())
                .phoneNumber(school.getPhoneNumber())
                .address(school.getAddress())
                .ward(school.getWard())
                .province(school.getProvince())
                .country(school.getCountry())
                .principalName(school.getPrincipalName())
                .position(school.getPosition())
                .linkWeb(school.getLinkWeb())
                .description(school.getDescription())
                .logoUrl(s3PresignedUrlService.generatePresignedUrl(school.getLogoUrl()))
                .licenseUrl(s3PresignedUrlService.generatePresignedUrl(school.getLicenseUrl()))
                .approvalStatus(school.getApprovalStatus())
                .approvedAt(school.getApprovedAt())
                .accountStatus(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }

    private PartnershipDetailResponse mapToPartnershipDetailResponse(Partnership partnership) {
        User user = partnership.getUser();
        return PartnershipDetailResponse.builder()
                .id(partnership.getId())
                .userId(user.getId().toString())
                .organizationName(partnership.getOrganizationName())
                .partnershipType(partnership.getPartnershipType())
                .taxCode(partnership.getTaxCode())
                .contactEmail(partnership.getContactEmail())
                .phoneNumber(partnership.getPhoneNumber())
                .registeredAddress(partnership.getRegisteredAddress())
                .geographicScopeWard(partnership.getGeographicScopeWard())
                .geographicScopeProvince(partnership.getGeographicScopeProvince())
                .contactPerson(partnership.getContactPerson())
                .position(partnership.getPosition())
                .linkWeb(partnership.getLinkWeb())
                .description(partnership.getDescription())
                .logoUrl(s3PresignedUrlService.generatePresignedUrl(partnership.getLogoUrl()))
                .licenseUrl(s3PresignedUrlService.generatePresignedUrl(partnership.getLicenseUrl()))
                .approvalStatus(partnership.getApprovalStatus())
                .approvedAt(partnership.getApprovedAt())
                .accountStatus(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(partnership.getCreatedAt())
                .updatedAt(partnership.getUpdatedAt())
                .build();
    }

    @Override
    public AdminUserListResponse getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với id: " + userId));

        return switch (user.getRole()) {
            case PARTNERSHIP_SCHOOL -> {
                School school = schoolRepository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));
                yield mapSchoolToAdminUserResponse(school);
            }
            case THIRD_PARTY_PARTNERSHIP -> {
                Partnership partnership = partnershipRepository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin đối tác"));
                yield mapPartnershipToAdminUserResponse(partnership);
            }
            case STUDENT -> {
                Student student = studentRepository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin học sinh"));
                yield mapStudentToAdminUserResponse(student);
            }
            case PARENT -> {
                Parent parent = parentRepository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin phụ huynh"));
                yield mapParentToAdminUserResponse(parent);
            }
            default -> AdminUserListResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .isActive(user.getIsActive())
                    .createdAt(user.getCreatedAt())
                    .detail(null)
                    .build();
        };
    }

    @Override
    public SchoolDetailResponse getSchoolById(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học với id: " + schoolId));
        return mapToSchoolDetailResponse(school);
    }

    @Override
    public PartnershipDetailResponse getPartnershipById(UUID partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đối tác với id: " + partnershipId));
        return mapToPartnershipDetailResponse(partnership);
    }

    // --- Specification builders ---

    private Specification<School> buildSchoolSpec(ApprovalStatus approvalStatus, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (approvalStatus != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), approvalStatus));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("schoolName")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("contactEmail")), pattern);
                Predicate taxCodeLike = cb.like(cb.lower(root.get("taxCode")), pattern);
                predicates.add(cb.or(nameLike, emailLike, taxCodeLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Partnership> buildPartnershipSpec(ApprovalStatus approvalStatus, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (approvalStatus != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), approvalStatus));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("organizationName")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("contactEmail")), pattern);
                Predicate taxCodeLike = cb.like(cb.lower(root.get("taxCode")), pattern);
                predicates.add(cb.or(nameLike, emailLike, taxCodeLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // --- User mapping helper for paginated getAllUsers ---

    private AdminUserListResponse mapUserToAdminUserResponse(User user, UUID schoolIdFilter) {
        return switch (user.getRole()) {
            case PARTNERSHIP_SCHOOL -> {
                School school = schoolRepository.findByUserId(user.getId()).orElse(null);
                yield school != null ? mapSchoolToAdminUserResponse(school) : buildBasicResponse(user);
            }
            case THIRD_PARTY_PARTNERSHIP -> {
                Partnership partnership = partnershipRepository.findByUserId(user.getId()).orElse(null);
                yield partnership != null ? mapPartnershipToAdminUserResponse(partnership) : buildBasicResponse(user);
            }
            case STUDENT -> {
                Student student = studentRepository.findByUserId(user.getId()).orElse(null);
                if (student != null && schoolIdFilter != null && !student.getSchool().getId().equals(schoolIdFilter)) {
                    yield null; // filtered out by schoolId
                }
                yield student != null ? mapStudentToAdminUserResponse(student) : buildBasicResponse(user);
            }
            case PARENT -> {
                Parent parent = parentRepository.findByUserId(user.getId()).orElse(null);
                if (parent != null && schoolIdFilter != null) {
                    List<StudentParentLink> links = studentParentLinkRepository.findByParentId(parent.getId());
                    boolean belongsToSchool = links.stream()
                            .anyMatch(link -> link.getStudent().getSchool().getId().equals(schoolIdFilter));
                    if (!belongsToSchool) {
                        yield null; // filtered out by schoolId
                    }
                }
                yield parent != null ? mapParentToAdminUserResponse(parent) : buildBasicResponse(user);
            }
            default -> buildBasicResponse(user);
        };
    }

    private AdminUserListResponse buildBasicResponse(User user) {
        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(null)
                .build();
    }

    private AdminUserListResponse mapSchoolToAdminUserResponse(School school) {
        User user = school.getUser();
        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(mapToSchoolDetailResponse(school))
                .build();
    }

    private AdminUserListResponse mapPartnershipToAdminUserResponse(Partnership partnership) {
        User user = partnership.getUser();
        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(mapToPartnershipDetailResponse(partnership))
                .build();
    }

    private AdminUserListResponse mapStudentToAdminUserResponse(Student student) {
        User user = student.getUser();
        School school = student.getSchool();
        StudentAdminDetail studentDetail = StudentAdminDetail.builder()
                .studentId(student.getId())
                .fullName(student.getFullName())
                .studentCode(student.getStudentCode())
                .className(student.getClassName())
                .gradeLevel(student.getGradeLevel())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender() != null ? student.getGender().name() : null)
                .address(student.getAddress())
                .avatarUrl(s3PresignedUrlService.generatePresignedUrl(student.getAvatarUrl()))
                .accountStatus(user.getStatus())
                .isActive(user.getIsActive())
                .schoolName(school.getSchoolName())
                .schoolId(school.getId())
                .build();

        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(studentDetail)
                .build();
    }

    private AdminUserListResponse mapParentToAdminUserResponse(Parent parent) {
        User user = parent.getUser();
        List<StudentParentLink> links = studentParentLinkRepository.findByParentId(parent.getId());
        List<String> schoolNames = links.stream()
                .map(link -> link.getStudent().getSchool().getSchoolName())
                .distinct()
                .collect(Collectors.toList());

        ParentAdminDetail parentDetail = ParentAdminDetail.builder()
                .parentId(parent.getId())
                .fullName(parent.getFullName())
                .phoneNumber(parent.getPhoneNumber())
                .email(user.getEmail())
                .accountStatus(user.getStatus())
                .isActive(user.getIsActive())
                .schoolNames(schoolNames)
                .build();

        return AdminUserListResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .detail(parentDetail)
                .build();
    }

    private AdminGameTypeResponse mapGameType(GameType gameType) {
        List<WasteCategory> categories = gameType.getLevelPresets() == null
                ? List.of()
                : gameType.getLevelPresets().stream()
                        .filter(p -> p.getItems() != null)
                        .flatMap(p -> p.getItems().stream())
                        .filter(item -> item.getWasteCategories() != null)
                        .flatMap(item -> item.getWasteCategories().stream())
                        .distinct()
                        .toList();

        return AdminGameTypeResponse.builder()
                .id(gameType.getId())
                .typeCode(gameType.getTypeCode())
                .name(gameType.getName())
                .shortDescription(gameType.getShortDescription())
                .fullDescription(gameType.getFullDescription())
                .howToPlay(gameType.getHowToPlay())
                .thumbnailUrl(gameType.getThumbnailUrl())
                .thumbnailPresignedUrl(s3PresignedUrlService.generatePresignedUrl(gameType.getThumbnailUrl()))
                .iconUrl(gameType.getIconUrl())
                .iconPresignedUrl(s3PresignedUrlService.generatePresignedUrl(gameType.getIconUrl()))
                .features(gameType.getFeatures())
                .supportsCoin(gameType.isSupportsCoin())
                .maxLevels(gameType.getMaxLevels())
                .isActive(gameType.isActive())
                .displayOrder(gameType.getDisplayOrder())
                .mappedWasteCategories(categories)
                .build();
    }

    private void applyPresetUpsert(GameLevelPreset preset, AdminGameLevelPresetUpsertRequest request) {
        preset.setDifficulty(request.getDifficulty());

        Set<Integer> requestedLevels = new HashSet<>();
        for (AdminGameLevelPresetItemUpsertRequest itemRequest : request.getItems()) {
            if (!requestedLevels.add(itemRequest.getLevelNumber())) {
                throw new BadRequestException("levelNumber trong items không được trùng nhau");
            }
        }

        Map<Integer, GameLevelPresetItem> existingByLevel = new HashMap<>();
        if (preset.getItems() != null) {
            for (GameLevelPresetItem existingItem : preset.getItems()) {
                existingByLevel.putIfAbsent(existingItem.getLevelNumber(), existingItem);
            }
        }

        List<GameLevelPresetItem> items = request.getItems().stream()
                .sorted((a, b) -> Integer.compare(a.getLevelNumber(), b.getLevelNumber()))
                .map(itemRequest -> {
                    GameLevelPresetItem item = existingByLevel.get(itemRequest.getLevelNumber());
                    if (item == null) {
                        item = new GameLevelPresetItem();
                    }
                    applyPresetItemRequest(item, itemRequest, preset);
                    return item;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        if (items.stream().map(GameLevelPresetItem::getLevelNumber).distinct().count() != items.size()) {
            throw new BadRequestException("levelNumber trong items không được trùng nhau");
        }

        if (items.stream().anyMatch(item -> item.getWasteCategories() == null || item.getWasteCategories().isEmpty())) {
            throw new BadRequestException("Mỗi preset item phải có ít nhất 1 wasteCategory");
        }

        if (preset.getItems() == null) {
            preset.setItems(new ArrayList<>());
        } else {
            preset.getItems().clear();
        }
        preset.getItems().addAll(items);
    }

    private void applyPresetItemRequest(GameLevelPresetItem item,
            AdminGameLevelPresetItemUpsertRequest request,
            GameLevelPreset preset) {
        item.setPreset(preset);
        item.setLevelNumber(request.getLevelNumber());
        item.setItemCount(request.getItemCount());
        item.setTimeLimitSeconds(request.getTimeLimitSeconds());
        item.setScorePerCorrect(request.getScorePerCorrect());
        item.setLives(request.getLives());
        item.setWasteCategories(request.getWasteCategories() == null
                ? Set.of()
                : new HashSet<>(request.getWasteCategories()));
        item.setConfigJson(request.getConfigJson());
    }

    private AdminGameLevelPresetResponse mapGameLevelPreset(GameLevelPreset preset) {
        List<AdminGameLevelPresetItemResponse> items = preset.getItems() == null
                ? List.of()
                : preset.getItems().stream()
                        .sorted((a, b) -> Integer.compare(a.getLevelNumber(), b.getLevelNumber()))
                        .map(item -> AdminGameLevelPresetItemResponse.builder()
                                .id(item.getId())
                                .levelNumber(item.getLevelNumber())
                                .itemCount(item.getItemCount())
                                .timeLimitSeconds(item.getTimeLimitSeconds())
                                .scorePerCorrect(item.getScorePerCorrect())
                                .lives(item.getLives())
                                .wasteCategories(item.getWasteCategories())
                                .configJson(item.getConfigJson())
                                .build())
                        .toList();

        return AdminGameLevelPresetResponse.builder()
                .id(preset.getId())
                .gameTypeId(preset.getGameType().getId())
                .difficulty(preset.getDifficulty())
                .items(items)
                .build();
    }

    private AdminWasteSubCategoryResponse mapWasteSubCategory(WasteSubCategory subCategory) {
        return AdminWasteSubCategoryResponse.builder()
                .id(subCategory.getId())
                .category(subCategory.getCategory())
                .subCategoryCode(subCategory.getSubCategoryCode())
                .displayName(subCategory.getDisplayName())
                .description(subCategory.getDescription())
                .iconUrl(s3PresignedUrlService.generatePresignedUrl(subCategory.getIconUrl()))
                .displayOrder(subCategory.getDisplayOrder())
                .isActive(subCategory.isActive())
                .build();
    }

    private AdminWasteItemResponse mapWasteItem(WasteItem wasteItem) {
        WasteSubCategory subCategory = wasteItem.getSubCategory();
        return AdminWasteItemResponse.builder()
                .id(wasteItem.getId())
                .itemName(wasteItem.getItemName())
                .category(wasteItem.getCategory())
                .subCategoryId(subCategory != null ? subCategory.getId() : null)
                .subCategoryCode(subCategory != null ? subCategory.getSubCategoryCode() : null)
                .subCategoryDisplayName(subCategory != null ? subCategory.getDisplayName() : null)
                .description(wasteItem.getDescription())
                .funFact(wasteItem.getFunFact())
                .imageUrl(wasteItem.getImageUrl())
                .imagePresignedUrl(s3PresignedUrlService.generatePresignedUrl(wasteItem.getImageUrl()))
                .decompositionTime(wasteItem.getDecompositionTime())
                .recyclingTips(wasteItem.getRecyclingTips())
                .model3dUrl(wasteItem.getModel3dUrl())
                .model3dPresignedUrl(s3PresignedUrlService.generatePresignedUrl(wasteItem.getModel3dUrl()))
                .tripoTaskId(wasteItem.getTripoTaskId())
                .tripoStatus(wasteItem.getTripoStatus())
                .isActive(wasteItem.isActive())
                .build();
    }

    private GameType getActiveGameTypeOrThrow(UUID id) {
        return gameTypeRepository.findByIdAndIsDeleteFalse(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy game type"));
    }

    private GameLevelPreset getPresetOrThrow(UUID gameTypeId, UUID presetId) {
        return gameLevelPresetRepository.findById(presetId)
                .filter(preset -> preset.getGameType() != null && preset.getGameType().getId().equals(gameTypeId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy preset"));
    }

    private WasteSubCategory getActiveWasteSubCategoryOrThrow(UUID id) {
        return wasteSubCategoryRepository.findByIdAndIsDeleteFalse(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy waste sub-category"));
    }

    private WasteItem getActiveWasteItemOrThrow(UUID id) {
        return wasteItemRepository.findByIdAndIsDeleteFalse(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy waste item"));
    }

    private void assignFreeSubscription(String planCode, SubscriberType subscriberType,
            School school, Partnership partnership, User user) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanCode(planCode).orElse(null);
        if (plan == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        Subscription subscription = new Subscription();
        subscription.setSubscriptionCode("SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        subscription.setSubscriberType(subscriberType);
        subscription.setSchool(school);
        subscription.setPartnership(partnership);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(now);
        subscription.setEndDate(now.plusDays(plan.getDurationDays()));
        subscriptionRepository.save(subscription);

        Payment payment = new Payment();
        payment.setPaymentCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setSubscriberType(subscriberType);
        payment.setSchool(school);
        payment.setPartnership(partnership);
        payment.setSubscription(subscription);
        payment.setAmount(BigDecimal.ZERO);
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.OTHER);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(now);
        payment.setPayerName(user.getEmail());
        payment.setPayerEmail(user.getEmail());
        payment.setCreatedBy(user);
        payment.setNotes("Free plan - auto-assigned on approval");
        paymentRepository.save(payment);
    }
}
