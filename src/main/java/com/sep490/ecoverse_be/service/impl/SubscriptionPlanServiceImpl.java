package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.request.UpdateSubscriptionPlanRequest;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.SubscriptionPlanResponse;
import com.sep490.ecoverse_be.entity.SubscriptionPlan;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.exception.ResourceNotFoundException;
import com.sep490.ecoverse_be.mapper.SubscriptionPlanMapper;
import com.sep490.ecoverse_be.repository.SubscriptionPlanRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.ISubscriptionPlanService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanServiceImpl implements ISubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanMapper subscriptionPlanMapper;

    @Override
    @Transactional
    public SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request, UUID adminUserId) {
        if (subscriptionPlanRepository.existsByPlanCode(request.planCode())) {
            throw new FuncErrorException("Plan code '" + request.planCode() + "' already exists.");
        }

        if (subscriptionPlanRepository.existsByPlanName(request.planName())) {
            throw new FuncErrorException("Plan name '" + request.planName() + "' already exists.");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found."));

        SubscriptionPlan plan = subscriptionPlanMapper.toEntity(request);
        plan.setCreatedBy(admin);

        SubscriptionPlan saved = subscriptionPlanRepository.save(plan);
        return subscriptionPlanMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse updatePlan(UUID planId, UpdateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with id: " + planId));

        if (request.planName() != null) {
            if (subscriptionPlanRepository.existsByPlanNameAndIdNot(request.planName(), planId)) {
                throw new FuncErrorException("Plan name '" + request.planName() + "' already exists.");
            }
        }

        subscriptionPlanMapper.updateEntity(plan, request);

        SubscriptionPlan updated = subscriptionPlanRepository.save(plan);
        return subscriptionPlanMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanById(UUID planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with id: " + planId));
        return subscriptionPlanMapper.toResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanByCode(String planCode) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with code: " + planCode));
        return subscriptionPlanMapper.toResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionPlanResponse> getAllPlans(
            SubscriberType subscriberType,
            Boolean isActive,
            String keyword,
            Pageable pageable
    ) {
        Specification<SubscriptionPlan> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (subscriberType != null) {
                predicates.add(cb.equal(root.get("subscriberType"), subscriberType));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("planName")), pattern);
                Predicate codeLike = cb.like(cb.lower(root.get("planCode")), pattern);
                predicates.add(cb.or(nameLike, codeLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SubscriptionPlan> page = subscriptionPlanRepository.findAll(spec, pageable);
        return PageResponse.from(page, subscriptionPlanMapper::toResponse);
    }

    @Override
    @Transactional
    public void toggleActiveStatus(UUID planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with id: " + planId));
        plan.setActive(!plan.isActive());
        subscriptionPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public void deletePlan(UUID planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with id: " + planId));
        subscriptionPlanRepository.delete(plan);
    }
}
