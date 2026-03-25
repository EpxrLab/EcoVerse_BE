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
import java.util.stream.Collectors;

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
    private WasteSubCategoryRepository wasteSubCategoryRepository;
    @Autowired
    private DefaultCoinConfigRepository defaultCoinConfigRepository;

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
                .map(r -> CampaignRoundInfoResponse.builder()
                        .id(r.getId())
                        .roundNumber(r.getRoundNumber())
                        .roundName(r.getRoundName())
                        .status(r.getStatus())
                        .startTime(r.getStartTime())
                        .endTime(r.getEndTime())
                        .quizId(r.getQuiz() != null ? r.getQuiz().getId() : null)
                        .build())
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
        round.setRoundName("Round 1");
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
        Campaign campaign = getSchoolCampaignOwned(campaignId, getCurrentSchool().getId());
        if (campaign.getSchoolStatus() != SchoolCampaignStatus.DRAFT) {
            throw new BadRequestException("Chỉ được kích hoạt campaign ở trạng thái DRAFT");
        }
        campaign.setSchoolStatus(SchoolCampaignStatus.SCHEDULED);
        campaignRepository.save(campaign);
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

        List<WasteSubCategory> selected = wasteSubCategoryRepository.findByIdIn(request.getSelectedSubCategoryIds());
        Set<UUID> supportedIds = gameType.getSupportedSubCategories().stream().map(BaseEntity::getId).collect(Collectors.toSet());
        for (WasteSubCategory subCategory : selected) {
            if (!supportedIds.contains(subCategory.getId())) {
                throw new BadRequestException("selectedSubCategoryIds phải thuộc game type được chọn");
            }
        }

        RoundGameConfig config = roundGameConfigRepository.findFirstByCampaignRoundIdOrderByDisplayOrderAsc(roundId)
                .orElseGet(RoundGameConfig::new);
        config.setCampaignRound(round);
        config.setGameType(gameType);
        config.setDifficultyOverride(request.getDifficultyOverride());
        config.setResolvedDifficulty(request.getDifficultyOverride());
        config.setAllowedSubCategories(selected);
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
    public void bindExistingQuiz(UUID roundId, BindRoundQuizRequest request) {
        User user = getCurrentUser();
        CampaignRound round = campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy round"));
        Quiz quiz;
        if (user.getRole() == Role.PARTNERSHIP_SCHOOL) {
            School school = getCurrentSchool();
            quiz = quizRepository.findByIdAndSchoolIdAndIsActiveTrue(request.getQuizId(), school.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy quiz thuộc quyền sở hữu"));
        } else {
            Partnership partnership = getCurrentPartnership();
            quiz = quizRepository.findByIdAndPartnershipIdAndIsActiveTrue(request.getQuizId(), partnership.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy quiz thuộc quyền sở hữu"));
        }
        round.setQuiz(quiz);
        campaignRoundRepository.save(round);
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
                .selectedSubCategoryIds(config.getAllowedSubCategories().stream().map(BaseEntity::getId).toList())
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
        if (!"INVITING".equals(statusOf(campaign)) || !"EXTENDED".equals(statusOf(campaign))) {
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


