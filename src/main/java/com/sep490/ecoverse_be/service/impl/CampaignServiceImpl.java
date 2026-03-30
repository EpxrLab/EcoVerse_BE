package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.*;
import com.sep490.ecoverse_be.dto.response.*;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.*;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.ICampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CampaignServiceImpl implements ICampaignService {

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private CampaignRoundRepository campaignRoundRepository;
    @Autowired
    private CampaignParticipantRepository campaignParticipantRepository;
    @Autowired
    private CampaignRoundParticipantRepository campaignRoundParticipantRepository;
    @Autowired
    private CampaignSchoolParticipateRepository campaignSchoolParticipateRepository;
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
    private RoundGameConfigRepository roundGameConfigRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private RoundLeaderboardRepository roundLeaderboardRepository;
    @Autowired
    private SchoolLeaderboardRepository schoolLeaderboardRepository;
    @Autowired
    private GameTypeRepository gameTypeRepository;
    @Autowired
    private GameLevelPresetRepository gameLevelPresetRepository;
    @Autowired
    private WasteSubCategoryRepository wasteSubCategoryRepository;
    @Autowired
    private DefaultCoinConfigRepository defaultCoinConfigRepository;
    @Autowired
    private CampaignRoundQuizRepository campaignRoundQuizRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Không xác định được người dùng hiện tại");
        }
        return principal.getUser();
    }

    private School getCurrentSchool() {
        return schoolRepository.findByUserId(getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));
    }

    private Partnership getCurrentPartnership() {
        return partnershipRepository.findByUserId(getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin đối tác"));
    }

    private Student getCurrentStudent() {
        return studentRepository.findByUserId(getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin học sinh"));
    }

    private Parent getCurrentParent() {
        return parentRepository.findByUserId(getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin phụ huynh"));
    }

    private String statusOf(Campaign campaign) {
        if (campaign.getCampaignType() == CampaignType.PARTNERSHIP_EVENT) {
            return campaign.getPartnershipStatus().name();
        }
        return campaign.getSchoolStatus().name();
    }

    private String createCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BadRequestException("Thời gian bắt đầu/kết thúc không hợp lệ");
        }
    }

    private Campaign getSchoolCampaignOwned(UUID campaignId, UUID schoolId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy campaign"));
        if (campaign.getCreatorSchool() == null || !campaign.getCreatorSchool().getId().equals(schoolId)) {
            throw new BadRequestException("Bạn không có quyền truy cập campaign này");
        }
        return campaign;
    }

    private Campaign getPartnershipCampaignOwned(UUID campaignId, UUID partnershipId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy campaign"));
        if (campaign.getCreatorPartnership() == null || !campaign.getCreatorPartnership().getId().equals(partnershipId)) {
            throw new BadRequestException("Bạn không có quyền truy cập campaign này");
        }
        return campaign;
    }

    private CampaignDetailResponse mapCampaignDetail(Campaign campaign) {
        List<CampaignRoundInfoResponse> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId())
                .stream()
                .map(r -> {
                    Optional<RoundGameConfig> configOpt = roundGameConfigRepository.findFirstByCampaignRoundIdOrderByDisplayOrderAsc(r.getId());
                    List<UUID> selectedPresetIds = configOpt
                            .map(cfg -> cfg.getSelectedPresets() == null
                                    ? List.<UUID>of()
                                    : cfg.getSelectedPresets().stream().map(BaseEntity::getId).toList())
                            .orElse(List.of());
                    Map<String, List<UUID>> presetSubCategoryConfig = configOpt
                            .map(cfg -> cfg.getPresetSubCategoryConfig() == null ? Map.<String, List<UUID>>of() : cfg.getPresetSubCategoryConfig())
                            .orElse(Map.of());
                    List<UUID> quizIds = r.getSelectedQuizzes() == null
                            ? List.of()
                            : r.getSelectedQuizzes().stream().map(BaseEntity::getId).toList();

                    return CampaignRoundInfoResponse.builder()
                            .id(r.getId())
                            .roundNumber(r.getRoundNumber())
                            .roundName(r.getRoundName())
                            .status(r.getStatus())
                            .startTime(r.getStartTime())
                            .endTime(r.getEndTime())
                            .quizId(r.getQuiz() != null ? r.getQuiz().getId() : null)
                            .quizIds(quizIds)
                            .selectedPresetIds(selectedPresetIds)
                            .presetSubCategoryConfig(presetSubCategoryConfig)
                            .build();
                })
                .toList();

        return CampaignDetailResponse.builder()
                .id(campaign.getId())
                .campaignCode(campaign.getCampaignCode())
                .campaignName(campaign.getCampaignName())
                .campaignType(campaign.getCampaignType())
                .description(campaign.getDescription())
                .status(statusOf(campaign))
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .invitationDate(campaign.getInvitationDate())
                .invitationDeadline(campaign.getInvitationDeadline())
                .topRankingCount(campaign.getTopRankingCount())
                .totalRounds(campaign.getTotalRounds())
                .rounds(rounds)
                .build();
    }

    private CampaignSummaryResponse mapCampaignSummary(Campaign campaign) {
        return CampaignSummaryResponse.builder()
                .id(campaign.getId())
                .campaignCode(campaign.getCampaignCode())
                .campaignName(campaign.getCampaignName())
                .campaignType(campaign.getCampaignType())
                .status(statusOf(campaign))
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .invitationDeadline(campaign.getInvitationDeadline())
                .build();
    }

    private void ensureHasAtLeastOneGameAndQuiz(Campaign campaign) {
        List<CampaignRound> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());
        boolean hasAtLeastOneQuiz = rounds.stream().anyMatch(round -> round.getQuiz() != null
                || (round.getSelectedQuizzes() != null && !round.getSelectedQuizzes().isEmpty()));
        boolean hasAtLeastOneGame = rounds.stream().anyMatch(round -> roundGameConfigRepository
                .findFirstByCampaignRoundIdOrderByDisplayOrderAsc(round.getId())
                .map(cfg -> cfg.getSelectedPresets() != null && !cfg.getSelectedPresets().isEmpty())
                .orElse(false));

        if (!hasAtLeastOneGame || !hasAtLeastOneQuiz) {
            throw new BadRequestException("Campaign cần add ít nhất 1 game preset và 1 quiz trước khi chuyển khỏi DRAFT");
        }
    }

    private Map<UUID, List<UUID>> normalizePresetSubCategoryConfigs(UpdateRoundGameConfigRequest request) {
        Map<UUID, List<UUID>> normalized = new LinkedHashMap<>();
        for (RoundPresetSubCategoryConfigRequest cfg : request.getPresetSubCategoryConfigs()) {
            if (normalized.containsKey(cfg.getPresetId())) {
                throw new BadRequestException("presetSubCategoryConfigs chứa preset bị trùng");
            }
            LinkedHashSet<UUID> uniqueSubCategoryIds = new LinkedHashSet<>(cfg.getSelectedSubCategoryIds());
            if (uniqueSubCategoryIds.isEmpty()) {
                throw new BadRequestException("Mỗi preset phải chọn ít nhất 1 sub-category");
            }
            normalized.put(cfg.getPresetId(), new ArrayList<>(uniqueSubCategoryIds));
        }
        return normalized;
    }

    @Override
    @Transactional
    public CampaignDetailResponse createSchoolCampaign(SchoolCampaignUpsertRequest request) {
        School school = getCurrentSchool();
        validateDateRange(request.getStartDate(), request.getEndDate());

        Campaign campaign = new Campaign();
        campaign.setCampaignCode(createCode("SCH"));
        campaign.setCampaignName(request.getCampaignName());
        campaign.setCampaignType(CampaignType.SCHOOL_INTERNAL);
        campaign.setDescription(request.getDescription());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setInvitationDate(request.getInvitationDate());
        campaign.setInvitationDeadline(request.getInvitationDeadline());
        campaign.setTopRankingCount(request.getTopRankingCount() != null ? request.getTopRankingCount() : 10);
        campaign.setBannerImageUrl(request.getBannerImageUrl());
        campaign.setTotalRounds(1);
        campaign.setCreatorSchool(school);
        campaign.setCreatedBy(getCurrentUser());
        campaign.setSchoolStatus(SchoolCampaignStatus.DRAFT);
        campaign = campaignRepository.save(campaign);

        CampaignRound round = new CampaignRound();
        round.setCampaign(campaign);
        round.setRoundNumber(1);
        round.setRoundName("Play & Learn");
        round.setStartTime(campaign.getStartDate());
        round.setEndTime(campaign.getEndDate());
        campaignRoundRepository.save(round);

        return mapCampaignDetail(campaign);
    }

    @Override
    public List<CampaignSummaryResponse> getMySchoolCampaigns() {
        School school = getCurrentSchool();
        return campaignRepository.findByCreatorSchoolIdAndIsActiveTrueOrderByCreatedAtDesc(school.getId())
                .stream()
                .map(this::mapCampaignSummary)
                .toList();
    }

    @Override
    public CampaignDetailResponse getSchoolCampaignById(UUID campaignId) {
        return mapCampaignDetail(getSchoolCampaignOwned(campaignId, getCurrentSchool().getId()));
    }

    @Override
    @Transactional
    public CampaignDetailResponse updateSchoolCampaign(UUID campaignId, SchoolCampaignUpsertRequest request) {
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được sửa campaign ở trạng thái DRAFT");
        }
        validateDateRange(request.getStartDate(), request.getEndDate());
        campaign.setCampaignName(request.getCampaignName());
        campaign.setDescription(request.getDescription());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setInvitationDate(request.getInvitationDate());
        campaign.setInvitationDeadline(request.getInvitationDeadline());
        campaign.setTopRankingCount(request.getTopRankingCount() != null ? request.getTopRankingCount() : campaign.getTopRankingCount());
        campaign.setBannerImageUrl(request.getBannerImageUrl());
        campaignRepository.save(campaign);

        CampaignRound round = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId())
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));
        round.setStartTime(request.getStartDate());
        round.setEndTime(request.getEndDate());
        campaignRoundRepository.save(round);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse activateSchoolCampaign(UUID campaignId) {
        School school = getCurrentSchool();
        Campaign campaign = getSchoolCampaignOwned(campaignId, school.getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được kích hoạt campaign ở trạng thái DRAFT");
        }
        ensureHasAtLeastOneGameAndQuiz(campaign);
        campaign.setSchoolStatus(SchoolCampaignStatus.SCHEDULED);
        campaignRepository.save(campaign);

        // Tự động tạo bản ghi tham gia cho chính trường tạo campaign (school tự mời mình)
        boolean alreadyParticipating = campaignSchoolParticipateRepository
                .findByCampaignIdAndSchoolId(campaignId, school.getId()).isPresent();
        if (!alreadyParticipating) {
            CampaignSchoolParticipate selfParticipate = new CampaignSchoolParticipate();
            selfParticipate.setCampaign(campaign);
            selfParticipate.setSchool(school);
            selfParticipate.setStatus(ParticipationStatus.APPROVED);
            selfParticipate.setInvitationSentAt(LocalDateTime.now());
            selfParticipate.setParticipationConfirmedAt(LocalDateTime.now());
            campaignSchoolParticipateRepository.save(selfParticipate);
        }

        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse setSchoolCampaignDraft(UUID campaignId) {
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.SCHEDULED) {
            throw new BadRequestException("Chỉ được chuyển về DRAFT khi campaign ở trạng thái SCHEDULED");
        }
        campaign.setSchoolStatus(SchoolCampaignStatus.DRAFT);
        campaignRepository.save(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse extendInviting(UUID campaignId, ExtendInvitingRequest request) {
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.INVITING) {
            throw new BadRequestException("Chỉ được gia hạn khi campaign đang INVITING");
        }
        if (!request.getNewInviteEndAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Thời gian gia hạn phải lớn hơn hiện tại");
        }
        campaign.setInvitationDeadline(request.getNewInviteEndAt());
        campaign.setSchoolStatus(SchoolCampaignStatus.EXTENDED);
        campaignRepository.save(campaign);

        if (request.getAdditionalStudentIds() != null && !request.getAdditionalStudentIds().isEmpty()) {
            School school = getCurrentSchool();
            List<Student> students = studentRepository.findAllById(request.getAdditionalStudentIds());
            for (Student student : students) {
                if (!student.getSchool().getId().equals(school.getId())) {
                    continue;
                }
                if (campaignParticipantRepository.existsByCampaignIdAndStudentId(campaignId, student.getId())) {
                    continue;
                }
                CampaignParticipant participant = new CampaignParticipant();
                participant.setCampaign(campaign);
                participant.setSchool(school);
                participant.setStudent(student);
                participant.setEnrollmentDate(LocalDateTime.now());
                participant.setParentApprovalStatus(ParticipationStatus.PENDING_PARENT_APPROVAL);
                campaignParticipantRepository.save(participant);
            }
        }
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse cancelSchoolCampaign(UUID campaignId) {
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.EXTENDED) {
            throw new BadRequestException("Chỉ được hủy campaign ở trạng thái EXTENDED");
        }
        campaign.setSchoolStatus(SchoolCampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public void inviteStudentsToSchoolCampaign(UUID campaignId, AssignStudentsRequest request) {
        School school = getCurrentSchool();
        Campaign campaign = getSchoolCampaignOwned(campaignId, school.getId());

        // Cho phép chọn học sinh ở trạng thái DRAFT hoặc SCHEDULED (trước khi scheduler chuyển sang INVITING)
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.DRAFT
                && campaign.getSchoolStatus() != SchoolCampaignStatus.SCHEDULED) {
            throw new BadRequestException("Chỉ được mời học sinh khi campaign đang ở trạng thái DRAFT hoặc SCHEDULED");
        }

        List<Student> students = studentRepository.findAllById(request.getStudentIds());
        int added = 0;
        for (Student student : students) {
            if (!student.getSchool().getId().equals(school.getId())) {
                continue;
            }
            if (campaignParticipantRepository.existsByCampaignIdAndStudentId(campaignId, student.getId())) {
                continue;
            }
            CampaignParticipant participant = new CampaignParticipant();
            participant.setCampaign(campaign);
            participant.setStudent(student);
            participant.setSchool(school);
            participant.setEnrollmentDate(LocalDateTime.now());
            participant.setParentApprovalStatus(ParticipationStatus.PENDING_PARENT_APPROVAL);
            campaignParticipantRepository.save(participant);
            added++;
        }

        // Cập nhật studentsEnrolled trên CampaignSchoolParticipate nếu đã tồn tại
        // Dùng final variable để dùng được trong lambda
        final int totalAdded = added;
        campaignSchoolParticipateRepository.findByCampaignIdAndSchoolId(campaignId, school.getId())
                .ifPresent(sp -> {
                    sp.setStudentsEnrolled(sp.getStudentsEnrolled() + totalAdded);
                    campaignSchoolParticipateRepository.save(sp);
                });
    }

    @Override
    @Transactional
    public CampaignDetailResponse createPartnershipCampaign(CreatePartnershipCampaignRequest request) {
        Partnership partnership = getCurrentPartnership();
        validateDateRange(request.getStartDate(), request.getEndDate());
        if (request.getRounds() == null || request.getRounds().isEmpty()) {
            throw new BadRequestException("Partnership campaign phải có ít nhất 1 round");
        }

        Campaign campaign = new Campaign();
        campaign.setCampaignCode(createCode("PRT"));
        campaign.setCampaignName(request.getCampaignName());
        campaign.setCampaignType(CampaignType.PARTNERSHIP_EVENT);
        campaign.setDescription(request.getDescription());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setRegistrationDate(request.getRegistrationDate());
        campaign.setRegistrationDeadline(request.getRegistrationDeadline());
        campaign.setInvitationDate(request.getInvitationDate());
        campaign.setInvitationDeadline(request.getInvitationDeadline());
        campaign.setMaxStudentsPerSchool(request.getMaxStudentsPerSchool());
        campaign.setTotalStudentQuota(request.getTotalStudentQuota());
        campaign.setTopRankingCount(request.getTopRankingCount() != null ? request.getTopRankingCount() : 10);
        campaign.setBannerImageUrl(request.getBannerImageUrl());
        campaign.setTotalRounds(request.getRounds().size());
        campaign.setCreatorPartnership(partnership);
        campaign.setCreatedBy(getCurrentUser());
        campaign.setPartnershipStatus(PartnershipCampaignStatus.DRAFT);
        campaign = campaignRepository.save(campaign);

        for (PartnershipRoundRequest roundRequest : request.getRounds()) {
            if (!roundRequest.getEndTime().isAfter(roundRequest.getStartTime())) {
                throw new BadRequestException("Thời gian round không hợp lệ");
            }
            CampaignRound round = new CampaignRound();
            round.setCampaign(campaign);
            round.setRoundNumber(roundRequest.getRoundNumber());
            round.setRoundName(roundRequest.getRoundName());
            round.setStartTime(roundRequest.getStartTime());
            round.setEndTime(roundRequest.getEndTime());
            round.setMaxParticipants(roundRequest.getMaxParticipants());
            round.setAdvanceCount(roundRequest.getAdvanceCount());
            round.setIsFinalRound(Boolean.TRUE.equals(roundRequest.getIsFinalRound()));
            campaignRoundRepository.save(round);
        }

        return mapCampaignDetail(campaign);
    }

    @Override
    public List<CampaignSummaryResponse> getMyPartnershipCampaigns() {
        Partnership partnership = getCurrentPartnership();
        return campaignRepository.findByCreatorPartnershipIdAndIsActiveTrueOrderByCreatedAtDesc(partnership.getId())
                .stream()
                .map(this::mapCampaignSummary)
                .toList();
    }

    @Override
    public CampaignDetailResponse getPartnershipCampaignById(UUID campaignId) {
        return mapCampaignDetail(getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId()));
    }

    @Override
    @Transactional
    public CampaignDetailResponse updatePartnershipCampaign(UUID campaignId, CreatePartnershipCampaignRequest request) {
        Campaign campaign = getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId());
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được sửa campaign ở trạng thái DRAFT");
        }
        validateDateRange(request.getStartDate(), request.getEndDate());
        campaign.setCampaignName(request.getCampaignName());
        campaign.setDescription(request.getDescription());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setRegistrationDate(request.getRegistrationDate());
        campaign.setRegistrationDeadline(request.getRegistrationDeadline());
        campaign.setInvitationDate(request.getInvitationDate());
        campaign.setInvitationDeadline(request.getInvitationDeadline());
        campaign.setMaxStudentsPerSchool(request.getMaxStudentsPerSchool());
        campaign.setTotalStudentQuota(request.getTotalStudentQuota());
        campaign.setTopRankingCount(request.getTopRankingCount() != null ? request.getTopRankingCount() : campaign.getTopRankingCount());
        campaign.setBannerImageUrl(request.getBannerImageUrl());
        campaignRepository.save(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public void inviteSchools(UUID campaignId, InviteSchoolsRequest request) {
        Campaign campaign = getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId());
        if (campaign.getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
            throw new BadRequestException("Chỉ hỗ trợ mời school cho partnership campaign");
        }
        List<School> schools = schoolRepository.findAllById(request.getSchoolIds());
        for (School school : schools) {
            CampaignSchoolParticipate invitation = campaignSchoolParticipateRepository
                    .findByCampaignIdAndSchoolId(campaignId, school.getId())
                    .orElseGet(CampaignSchoolParticipate::new);
            invitation.setCampaign(campaign);
            invitation.setSchool(school);
            invitation.setStatus(ParticipationStatus.INVITED);
            invitation.setInvitationSentAt(LocalDateTime.now());
            campaignSchoolParticipateRepository.save(invitation);
        }
    }

    @Override
    @Transactional
    public CampaignDetailResponse activatePartnershipCampaign(UUID campaignId) {
        Campaign campaign = getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId());
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được kích hoạt campaign ở trạng thái DRAFT");
        }
        ensureHasAtLeastOneGameAndQuiz(campaign);
        campaign.setPartnershipStatus(PartnershipCampaignStatus.JOINING);
        campaignRepository.save(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse setPartnershipCampaignDraft(UUID campaignId) {
        Campaign campaign = getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId());
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.JOINING) {
            throw new BadRequestException("Chỉ được chuyển về DRAFT khi campaign ở trạng thái JOINING");
        }
        campaign.setPartnershipStatus(PartnershipCampaignStatus.DRAFT);
        campaignRepository.save(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    @Transactional
    public CampaignDetailResponse cancelPartnershipCampaign(UUID campaignId) {
        Campaign campaign = getPartnershipCampaignOwned(campaignId, getCurrentPartnership().getId());
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.JOINING) {
            throw new BadRequestException("Chỉ được hủy campaign ở trạng thái JOINING");
        }
        campaign.setPartnershipStatus(PartnershipCampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
        return mapCampaignDetail(campaign);
    }

    @Override
    public List<PartnershipInvitationResponse> getPartnershipInvitationsForSchool() {
        School school = getCurrentSchool();
        return campaignSchoolParticipateRepository.findBySchoolIdOrderByCreatedAtDesc(school.getId())
                .stream()
                .map(i -> PartnershipInvitationResponse.builder()
                        .invitationId(i.getId())
                        .campaignId(i.getCampaign().getId())
                        .campaignName(i.getCampaign().getCampaignName())
                        .status(i.getStatus())
                        .invitationSentAt(i.getInvitationSentAt())
                        .studentsEnrolled(i.getStudentsEnrolled())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void acceptPartnershipInvitation(UUID invitationId) {
        School school = getCurrentSchool();
        CampaignSchoolParticipate invitation = campaignSchoolParticipateRepository.findByIdAndSchoolId(invitationId, school.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời"));
        if (invitation.getStatus() != ParticipationStatus.INVITED) {
            throw new BadRequestException("Lời mời đã được xử lý");
        }
        invitation.setStatus(ParticipationStatus.APPROVED);
        invitation.setParticipationConfirmedAt(LocalDateTime.now());
        campaignSchoolParticipateRepository.save(invitation);
    }

    @Override
    @Transactional
    public void rejectPartnershipInvitation(UUID invitationId) {
        School school = getCurrentSchool();
        CampaignSchoolParticipate invitation = campaignSchoolParticipateRepository.findByIdAndSchoolId(invitationId, school.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời"));
        if (invitation.getStatus() != ParticipationStatus.INVITED) {
            throw new BadRequestException("Lời mời đã được xử lý");
        }
        invitation.setStatus(ParticipationStatus.REJECTED);
        campaignSchoolParticipateRepository.save(invitation);
    }

    @Override
    @Transactional
    public void assignStudentsToPartnershipInvitation(UUID invitationId, AssignStudentsRequest request) {
        School school = getCurrentSchool();
        CampaignSchoolParticipate invitation = campaignSchoolParticipateRepository.findByIdAndSchoolId(invitationId, school.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời"));
        if (invitation.getStatus() != ParticipationStatus.APPROVED) {
            throw new BadRequestException("School phải accept lời mời trước khi phân công học sinh");
        }
        Campaign campaign = invitation.getCampaign();
        if (campaign.getPartnershipStatus() != PartnershipCampaignStatus.JOINING) {
            throw new BadRequestException("Chỉ được phân công học sinh khi campaign đang JOINING");
        }

        List<Student> students = studentRepository.findAllById(request.getStudentIds());
        int added = 0;
        for (Student student : students) {
            if (!student.getSchool().getId().equals(school.getId())) {
                continue;
            }
            if (campaignParticipantRepository.existsByCampaignIdAndStudentId(campaign.getId(), student.getId())) {
                continue;
            }
            CampaignParticipant participant = new CampaignParticipant();
            participant.setCampaign(campaign);
            participant.setStudent(student);
            participant.setSchool(school);
            participant.setEnrollmentDate(LocalDateTime.now());
            participant.setParentApprovalStatus(ParticipationStatus.PENDING_PARENT_APPROVAL);
            campaignParticipantRepository.save(participant);
            added++;
        }
        invitation.setStudentsEnrolled(invitation.getStudentsEnrolled() + added);
        campaignSchoolParticipateRepository.save(invitation);
    }

    @Override
    @Transactional
    public void updateRoundGameConfig(UUID roundId, UpdateRoundGameConfigRequest request) {
        User user = getCurrentUser();
        CampaignRound round = campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));
        Campaign campaign = round.getCampaign();

        if (campaign.getCampaignType() == CampaignType.PARTNERSHIP_EVENT) {
            if (campaign.getCreatorPartnership() == null || !campaign.getCreatorPartnership().getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Bạn không có quyền sửa round này");
            }
        } else {
            if (campaign.getCreatorSchool() == null || !campaign.getCreatorSchool().getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Bạn không có quyền sửa round này");
            }
        }

        GameType gameType = gameTypeRepository.findById(request.getGameTypeId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy game type"));

        Set<UUID> requestedPresetIds = new HashSet<>(request.getSelectedPresetIds());
        Map<UUID, Integer> presetOrder = new HashMap<>();
        for (int i = 0; i < request.getSelectedPresetIds().size(); i++) {
            presetOrder.putIfAbsent(request.getSelectedPresetIds().get(i), i);
        }
        Map<UUID, List<UUID>> presetSubCategoryRequests = normalizePresetSubCategoryConfigs(request);
        if (!presetSubCategoryRequests.keySet().equals(requestedPresetIds)) {
            throw new BadRequestException("Mỗi preset được chọn phải có cấu hình sub-category tương ứng");
        }
        List<GameLevelPreset> selectedPresets = gameLevelPresetRepository
                .findByIdInAndGameTypeId(request.getSelectedPresetIds(), gameType.getId());
        if (selectedPresets.size() != requestedPresetIds.size()) {
            throw new BadRequestException("selectedPresetIds chứa preset không hợp lệ");
        }
        selectedPresets = selectedPresets.stream()
                .sorted(Comparator.comparingInt(preset -> presetOrder.getOrDefault(preset.getId(), Integer.MAX_VALUE)))
                .toList();

        Set<UUID> requestedSubCategoryIds = presetSubCategoryRequests.values().stream()
                .flatMap(Collection::stream)
                .collect(java.util.stream.Collectors.toSet());
        List<WasteSubCategory> activeSubCategories = wasteSubCategoryRepository.findByIdInAndIsActiveTrue(new ArrayList<>(requestedSubCategoryIds));
        if (activeSubCategories.size() != requestedSubCategoryIds.size()) {
            throw new BadRequestException("Có sub-category không hợp lệ hoặc đã bị xóa mềm");
        }
        Map<UUID, WasteSubCategory> subCategoryById = new HashMap<>();
        for (WasteSubCategory subCategory : activeSubCategories) {
            subCategoryById.put(subCategory.getId(), subCategory);
        }

        Map<String, List<UUID>> normalizedPresetSubCategoryConfig = new LinkedHashMap<>();
        for (GameLevelPreset preset : selectedPresets) {
            Set<WasteCategory> allowedCategories = preset.getItems() == null
                    ? Set.of()
                    : preset.getItems().stream()
                    .filter(item -> item.getWasteCategories() != null)
                    .flatMap(item -> item.getWasteCategories().stream())
                    .collect(java.util.stream.Collectors.toSet());
            if (allowedCategories.isEmpty()) {
                throw new BadRequestException("Preset chưa được admin cấu hình wasteCategory: " + preset.getId());
            }

            List<UUID> configuredSubCategoryIds = presetSubCategoryRequests.getOrDefault(preset.getId(), List.of());
            if (configuredSubCategoryIds.isEmpty()) {
                throw new BadRequestException("Preset phải chọn ít nhất 1 sub-category: " + preset.getId());
            }

            for (UUID subCategoryId : configuredSubCategoryIds) {
                WasteSubCategory subCategory = subCategoryById.get(subCategoryId);
                if (subCategory == null || !allowedCategories.contains(subCategory.getCategory())) {
                    throw new BadRequestException("Sub-category không thuộc wasteCategory admin đã cấu hình cho preset: " + preset.getId());
                }
            }
            normalizedPresetSubCategoryConfig.put(preset.getId().toString(), configuredSubCategoryIds);
        }

        RoundGameConfig config = roundGameConfigRepository.findFirstByCampaignRoundIdOrderByDisplayOrderAsc(roundId)
                .orElseGet(RoundGameConfig::new);
        config.setCampaignRound(round);
        config.setGameType(gameType);
        config.setDifficultyOverride(request.getDifficultyOverride());
        GameLevelPreset resolvedPreset = selectedPresets.stream()
                .filter(preset -> request.getDifficultyOverride() == null || preset.getDifficulty() == request.getDifficultyOverride())
                .findFirst()
                .orElse(selectedPresets.get(0));
        config.setResolvedPreset(resolvedPreset);
        config.setResolvedDifficulty(resolvedPreset.getDifficulty());
        config.setSelectedPresets(selectedPresets);
        config.setPresetSubCategoryConfig(normalizedPresetSubCategoryConfig);
        if (campaign.getCampaignType() == CampaignType.PARTNERSHIP_EVENT) {
            config.setCoinPerSession(null);
        } else {
            config.setCoinPerSession(request.getCoinPerSession());
        }
        config.setDisplayOrder(1);
        config.setCreatedBy(user);
        roundGameConfigRepository.save(config);
    }

    @Override
    @Transactional
    public void bindQuizzesToRound(UUID roundId, List<BindRoundQuizRequest> requests) {
        User user = getCurrentUser();
        CampaignRound round = campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));

        boolean isSchool = user.getRole() == Role.PARTNERSHIP_SCHOOL;
        School school = isSchool ? getCurrentSchool() : null;
        Partnership partnership = isSchool ? null : getCurrentPartnership();

        for (int i = 0; i < requests.size(); i++) {
            BindRoundQuizRequest request = requests.get(i);

            Quiz quiz;
            if (isSchool) {
                quiz = quizRepository.findByIdAndSchoolIdAndIsActiveTrue(request.getQuizId(), school.getId())
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy quiz " + request.getQuizId() + " thuộc quyền sở hữu"));
            } else {
                quiz = quizRepository.findByIdAndPartnershipIdAndIsActiveTrue(request.getQuizId(), partnership.getId())
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy quiz " + request.getQuizId() + " thuộc quyền sở hữu"));
            }

            CampaignRoundQuiz roundQuiz = campaignRoundQuizRepository
                    .findByCampaignRoundIdAndQuizId(roundId, quiz.getId())
                    .orElse(new CampaignRoundQuiz());
            roundQuiz.setCampaignRound(round);
            roundQuiz.setQuiz(quiz);
            roundQuiz.setMaxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 3);
            roundQuiz.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : i + 1);
            roundQuiz.setRequired(request.getIsRequired() != null ? request.getIsRequired() : true);
            campaignRoundQuizRepository.save(roundQuiz);
        }
    }

    private boolean campaignMatchStudentStatus(Campaign campaign, CampaignParticipant participant, StudentCampaignStatusFilter status) {
        String campaignStatus = statusOf(campaign);
        return switch (status) {
            case INVITED -> participant.getParentApprovalStatus() == ParticipationStatus.PENDING_PARENT_APPROVAL;
            case ON_GOING -> "ON_GOING".equals(campaignStatus) && participant.getParentApprovalStatus() == ParticipationStatus.APPROVED;
            case COMPLETED -> "COMPLETED".equals(campaignStatus) && participant.getParentApprovalStatus() == ParticipationStatus.APPROVED;
        };
    }

    @Override
    public List<CampaignSummaryResponse> getStudentCampaigns(StudentCampaignStatusFilter status) {
        Student student = getCurrentStudent();
        List<CampaignParticipant> participants = campaignParticipantRepository.findByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(student.getId());
        if (status == null) {
            return participants.stream().map(CampaignParticipant::getCampaign).map(this::mapCampaignSummary).toList();
        }
        return participants.stream()
                .filter(p -> campaignMatchStudentStatus(p.getCampaign(), p, status))
                .map(CampaignParticipant::getCampaign)
                .map(this::mapCampaignSummary)
                .toList();
    }

    @Override
    public CampaignDetailResponse getStudentCampaignDetail(UUID campaignId) {
        Student student = getCurrentStudent();
        campaignParticipantRepository.findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, student.getId())
                .orElseThrow(() -> new NotFoundException("Bạn không tham gia campaign này"));
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy campaign"));
        return mapCampaignDetail(campaign);
    }

    @Override
    public PlayConfigResponse getPlayConfig(UUID campaignId, UUID roundId) {
        Student student = getCurrentStudent();
        CampaignParticipant participant = campaignParticipantRepository.findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, student.getId())
                .orElseThrow(() -> new BadRequestException("Student không có quyền vào campaign này"));

        if (participant.getParentApprovalStatus() != ParticipationStatus.APPROVED) {
            throw new BadRequestException("Student chưa được parent duyệt tham gia campaign");
        }

        CampaignRound round = campaignRoundRepository.findByIdAndCampaignId(roundId, campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round trong campaign"));

        RoundGameConfig config = roundGameConfigRepository.findFirstByCampaignRoundIdOrderByDisplayOrderAsc(round.getId())
                .orElseThrow(() -> new NotFoundException("Round chưa được cấu hình game"));

        Integer coinPerSession = null;
        if (participant.getCampaign().getCampaignType() != CampaignType.PARTNERSHIP_EVENT) {
            if (config.getCoinPerSession() != null) {
                coinPerSession = config.getCoinPerSession();
            } else if (config.getResolvedDifficulty() != null) {
                coinPerSession = defaultCoinConfigRepository
                        .findByGameTypeIdAndDifficulty(config.getGameType().getId(), config.getResolvedDifficulty())
                        .map(DefaultCoinConfig::getDefaultCoin)
                        .orElse(0);
            }
        }

        return PlayConfigResponse.builder()
                .campaignId(campaignId)
                .roundId(roundId)
                .gameTypeId(config.getGameType().getId())
                .gameTypeName(config.getGameType().getName())
                .resolvedDifficulty(config.getResolvedDifficulty())
                .coinPerSession(coinPerSession)
                .quizId(round.getQuiz() != null ? round.getQuiz().getId() : null)
                .quizIds(round.getSelectedQuizzes() == null ? List.of() : round.getSelectedQuizzes().stream().map(BaseEntity::getId).toList())
                .selectedPresetIds(config.getSelectedPresets() == null ? List.of() : config.getSelectedPresets().stream().map(BaseEntity::getId).toList())
                .presetSubCategoryConfig(config.getPresetSubCategoryConfig() == null ? Map.of() : config.getPresetSubCategoryConfig())
                .build();
    }

    @Override
    public List<LeaderboardEntryResponse> getCampaignLeaderboard(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy campaign"));
        if (campaign.getCampaignType() == CampaignType.SCHOOL_INTERNAL) {
            return schoolLeaderboardRepository.findByCampaignIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(campaignId)
                    .stream()
                    .map(e -> LeaderboardEntryResponse.builder()
                            .studentId(e.getStudent().getId())
                            .studentName(e.getStudent().getFullName())
                            .schoolId(e.getSchool().getId())
                            .schoolName(e.getSchool().getSchoolName())
                            .combinedAccuracyPercentage(e.getCombinedAccuracyPercentage())
                            .avgTimeSeconds(e.getAvgTimeSeconds())
                            .rank(e.getOverallRank())
                            .totalCoinsEarned(e.getTotalCoinsEarned())
                            .build())
                    .toList();
        }
        return roundLeaderboardRepository.findByCampaignIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(campaignId)
                .stream()
                .map(e -> LeaderboardEntryResponse.builder()
                        .studentId(e.getStudent().getId())
                        .studentName(e.getStudent().getFullName())
                        .schoolId(e.getSchool().getId())
                        .schoolName(e.getSchool().getSchoolName())
                        .combinedAccuracyPercentage(e.getCombinedAccuracyPercentage())
                        .avgTimeSeconds(e.getAvgTimeSeconds())
                        .rank(e.getOverallRankInRound())
                        .totalCoinsEarned(e.getTotalCoinsEarned())
                        .build())
                .toList();
    }

    @Override
    public List<LeaderboardEntryResponse> getCampaignRoundLeaderboard(UUID roundId) {
        campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));
        return roundLeaderboardRepository.findByCampaignRoundIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(roundId)
                .stream()
                .map(e -> LeaderboardEntryResponse.builder()
                        .studentId(e.getStudent().getId())
                        .studentName(e.getStudent().getFullName())
                        .schoolId(e.getSchool().getId())
                        .schoolName(e.getSchool().getSchoolName())
                        .combinedAccuracyPercentage(e.getCombinedAccuracyPercentage())
                        .avgTimeSeconds(e.getAvgTimeSeconds())
                        .rank(e.getOverallRankInRound())
                        .totalCoinsEarned(e.getTotalCoinsEarned())
                        .build())
                .toList();
    }

    @Override
    public List<ParentCampaignInvitationResponse> getParentCampaignInvitations() {
        Parent parent = getCurrentParent();
        List<StudentParentLink> links = studentParentLinkRepository.findByParentId(parent.getId());
        List<UUID> studentIds = links.stream().map(link -> link.getStudent().getId()).toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        List<CampaignParticipant> participants = campaignParticipantRepository
                .findByStudentIdInAndParentApprovalStatusAndIsActiveTrue(
                        studentIds,
                        ParticipationStatus.PENDING_PARENT_APPROVAL
                );

        return participants.stream()
                .map(p -> ParentCampaignInvitationResponse.builder()
                        .campaignId(p.getCampaign().getId())
                        .campaignName(p.getCampaign().getCampaignName())
                        .studentId(p.getStudent().getId())
                        .studentName(p.getStudent().getFullName())
                        .parentApprovalStatus(p.getParentApprovalStatus())
                        .invitationDeadline(p.getCampaign().getInvitationDeadline())
                        .build())
                .toList();
    }

    private void parentHandleJoin(UUID campaignId, ParentCampaignApprovalRequest request, ParticipationStatus status) {
        Parent parent = getCurrentParent();
        if (!studentParentLinkRepository.existsByStudentIdAndParentId(request.getStudentId(), parent.getId())) {
            throw new BadRequestException("Phụ huynh không có liên kết hợp lệ với học sinh");
        }

        CampaignParticipant participant = campaignParticipantRepository
                .findByCampaignIdAndStudentIdAndIsActiveTrue(campaignId, request.getStudentId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời tham gia campaign"));

        Campaign campaign = participant.getCampaign();
        // Dùng && thay vì || để kiểm tra đúng: chỉ reject khi status KHÔNG phải INVITING VÀ KHÔNG phải EXTENDED/JOINING
        String campaignStatus = statusOf(campaign);
        if (!"INVITING".equals(campaignStatus) && !"EXTENDED".equals(campaignStatus) && !"JOINING".equals(campaignStatus)) {
            throw new BadRequestException("Chỉ xử lý duyệt khi campaign đang INVITING hoặc EXTENDED");
        }
        if (participant.getParentApprovalStatus() != ParticipationStatus.PENDING_PARENT_APPROVAL) {
            throw new BadRequestException("Lời mời đã được xử lý trước đó");
        }

        participant.setParentApprovalStatus(status);
        participant.setParentApprovedBy(parent);
        participant.setParentApprovedAt(LocalDateTime.now());
        campaignParticipantRepository.save(participant);
    }

    @Override
    @Transactional
    public void parentApproveJoin(UUID campaignId, ParentCampaignApprovalRequest request) {
        parentHandleJoin(campaignId, request, ParticipationStatus.APPROVED);
    }

    @Override
    @Transactional
    public void parentRejectJoin(UUID campaignId, ParentCampaignApprovalRequest request) {
        parentHandleJoin(campaignId, request, ParticipationStatus.REJECTED);
    }

    @Override
    public List<CampaignProgressResponse> getParentStudentProgress(UUID studentId) {
        Parent parent = getCurrentParent();
        if (!studentParentLinkRepository.existsByStudentIdAndParentId(studentId, parent.getId())) {
            throw new BadRequestException("Phụ huynh không có quyền xem tiến độ của học sinh này");
        }

        List<CampaignParticipant> participants = campaignParticipantRepository.findByStudentIdAndIsActiveTrue(studentId);
        return participants.stream()
                .map(p -> {
                    int completedRounds = campaignRoundParticipantRepository
                            .countByCampaignParticipantIdAndCompletedAtIsNotNull(p.getId());
                    return CampaignProgressResponse.builder()
                            .campaignId(p.getCampaign().getId())
                            .campaignName(p.getCampaign().getCampaignName())
                            .campaignStatus(statusOf(p.getCampaign()))
                            .parentApprovalStatus(p.getParentApprovalStatus())
                            .totalRounds(p.getCampaign().getTotalRounds())
                            .completedRounds(completedRounds)
                            .build();
                })
                .toList();
    }
}


