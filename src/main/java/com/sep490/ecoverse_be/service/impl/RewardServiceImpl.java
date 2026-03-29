package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.CreateRewardRequest;
import com.sep490.ecoverse_be.dto.request.UpdateRewardRequest;
import com.sep490.ecoverse_be.dto.response.RewardResponse;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.RewardType;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.IRewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RewardServiceImpl implements IRewardService {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentParentLinkRepository studentParentLinkRepository;
    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserPrincipal) auth.getPrincipal()).getUser();
    }

    private School resolveSchool(UUID userId) {
        return schoolRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học"));
    }

    private Reward assertOwnership(UUID rewardId, UUID schoolId) {
        return rewardRepository.findByIdAndSchoolIdAndIsActiveTrue(rewardId, schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy quà"));
    }

    private RewardResponse mapToResponse(Reward reward) {
        return RewardResponse.builder()
                .id(reward.getId())
                .rewardName(reward.getRewardName())
                .rewardType(reward.getRewardType())
                .description(reward.getDescription())
                .coinCost(reward.getCoinCost())
                .imageUrl(s3PresignedUrlService.generatePresignedUrl(reward.getImageUrl()))
                .stockQuantity(reward.getStockQuantity())
                .isUnlimited(reward.getIsUnlimited())
                .isActive(reward.getIsActive())
                .termsConditions(reward.getTermsConditions())
                .createdAt(reward.getCreatedAt())
                .updatedAt(reward.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public RewardResponse createReward(CreateRewardRequest request) {
        User currentUser = getCurrentUser();
        School school = resolveSchool(currentUser.getId());

        if (rewardRepository.existsByRewardNameAndSchoolIdAndIsActiveTrue(request.getRewardName(), school.getId())) {
            throw new BadRequestException("Tên quà '" + request.getRewardName() + "' đã tồn tại trong trường này");
        }

        boolean isUnlimited = Boolean.TRUE.equals(request.getIsUnlimited());
        if (!isUnlimited && request.getStockQuantity() == null) {
            throw new BadRequestException("Phải nhập số lượng tồn kho nếu quà không unlimited");
        }

        Reward reward = new Reward();
        reward.setSchool(school);
        reward.setCreatedBy(currentUser);
        reward.setRewardName(request.getRewardName());
        reward.setRewardType(request.getRewardType());
        reward.setDescription(request.getDescription());
        reward.setCoinCost(request.getCoinCost());
        reward.setImageUrl(request.getImageUrl());
        reward.setIsUnlimited(isUnlimited);
        reward.setStockQuantity(isUnlimited ? null : request.getStockQuantity());
        reward.setTermsConditions(request.getTermsConditions());
        reward.setIsActive(true);

        return mapToResponse(rewardRepository.save(reward));
    }

    @Override
    public List<RewardResponse> getRewardsForSchool(RewardType rewardType) {
        User currentUser = getCurrentUser();
        School school = resolveSchool(currentUser.getId());

        List<Reward> rewards = rewardType != null
                ? rewardRepository.findBySchoolIdAndRewardTypeAndIsActiveTrueOrderByCreatedAtDesc(school.getId(), rewardType)
                : rewardRepository.findBySchoolIdAndIsActiveTrueOrderByCreatedAtDesc(school.getId());

        return rewards.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<RewardResponse> getRewardsForStudent(RewardType rewardType) {
        User currentUser = getCurrentUser();
        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học sinh"));
        School school = student.getSchool();

        List<Reward> rewards = rewardType != null
                ? rewardRepository.findBySchoolIdAndRewardTypeAndIsActiveTrueOrderByCreatedAtDesc(school.getId(), rewardType)
                : rewardRepository.findBySchoolIdAndIsActiveTrueOrderByCreatedAtDesc(school.getId());

        return rewards.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<RewardResponse> getRewardsForParent(RewardType rewardType) {
        User currentUser = getCurrentUser();
        Parent parent = parentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phụ huynh"));

        Student student = studentParentLinkRepository
                .findFirstByParentId(parent.getId())
                .map(StudentParentLink::getStudent)
                .orElseThrow(() -> new NotFoundException("Phụ huynh chưa liên kết với học sinh"));

        School school = student.getSchool();

        List<Reward> rewards = rewardType != null
                ? rewardRepository.findBySchoolIdAndRewardTypeAndIsActiveTrueOrderByCreatedAtDesc(school.getId(), rewardType)
                : rewardRepository.findBySchoolIdAndIsActiveTrueOrderByCreatedAtDesc(school.getId());

        return rewards.stream().map(this::mapToResponse).collect(Collectors.toList());
    }


    @Override
    public RewardResponse getRewardById(UUID rewardId) {
        User currentUser = getCurrentUser();
        School school = resolveSchool(currentUser.getId());
        return mapToResponse(assertOwnership(rewardId, school.getId()));
    }

    @Override
    @Transactional
    public RewardResponse updateReward(UUID rewardId, UpdateRewardRequest request) {
        User currentUser = getCurrentUser();
        School school = resolveSchool(currentUser.getId());
        Reward reward = assertOwnership(rewardId, school.getId());

        if (request.getRewardName() != null && !request.getRewardName().equals(reward.getRewardName())) {
            if (rewardRepository.existsByRewardNameAndSchoolIdAndIsActiveTrue(request.getRewardName(), school.getId())) {
                throw new BadRequestException("Tên quà '" + request.getRewardName() + "' đã tồn tại trong trường này");
            }
            reward.setRewardName(request.getRewardName());
        }

        if (request.getRewardType() != null) reward.setRewardType(request.getRewardType());
        if (request.getDescription() != null) reward.setDescription(request.getDescription());
        if (request.getCoinCost() != null) reward.setCoinCost(request.getCoinCost());
        if (request.getImageUrl() != null) reward.setImageUrl(request.getImageUrl());
        if (request.getTermsConditions() != null) reward.setTermsConditions(request.getTermsConditions());

        if (request.getIsUnlimited() != null) {
            reward.setIsUnlimited(request.getIsUnlimited());
            if (Boolean.TRUE.equals(request.getIsUnlimited())) {
                reward.setStockQuantity(null);
            }
        }

        if (request.getStockQuantity() != null && !Boolean.TRUE.equals(reward.getIsUnlimited())) {
            reward.setStockQuantity(request.getStockQuantity());
        }

        return mapToResponse(rewardRepository.save(reward));
    }

    @Override
    @Transactional
    public void deleteReward(UUID rewardId) {
        User currentUser = getCurrentUser();
        School school = resolveSchool(currentUser.getId());
        Reward reward = assertOwnership(rewardId, school.getId());

        reward.setIsActive(false);
        rewardRepository.save(reward);
    }

}
