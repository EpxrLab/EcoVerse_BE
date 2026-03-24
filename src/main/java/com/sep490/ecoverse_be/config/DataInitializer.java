package com.sep490.ecoverse_be.config;

import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.*;
import com.sep490.ecoverse_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PartnershipRepository partnershipRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    private static final String DEFAULT_PASSWORD = "SP26@sep490";
    private static final String SCHOOL_FREE_PLAN_CODE = "SCHOOL_FREE";
    private static final String PARTNERSHIP_FREE_PLAN_CODE = "PARTNERSHIP_FREE";

    @Override
    public void run(String... args) {
        initFreeSubscriptionPlans();
        initAdminAccount();
        initSchoolAccount();
        initPartnershipAccount();
        initParentAccount();
        initStudentAccount();
    }

    private void initFreeSubscriptionPlans() {
        initFreePlan(
                SCHOOL_FREE_PLAN_CODE,
                "School Free Plan",
                SubscriberType.SCHOOL,
                "Goi mien phi mac dinh cho School",
                1000,
                1,
                1,
                null,
                0
        );

        initFreePlan(
                PARTNERSHIP_FREE_PLAN_CODE,
                "Partnership Free Plan",
                SubscriberType.PARTNERSHIP,
                "Goi mien phi mac dinh cho Partnership",
                null,
                1,
                1,
                1,
                0
        );
    }

    private void initFreePlan(
            String planCode,
            String planName,
            SubscriberType subscriberType,
            String description,
            Integer maxStudents,
            Integer maxCampaignsPerMonth,
            Integer maxRoundsPerCampaign,
            Integer maxSchoolsPerCampaign,
            Integer maxAiQuizGenerations
    ) {
        if (subscriptionPlanRepository.existsByPlanCode(planCode)) {
            return;
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setPlanCode(planCode);
        plan.setPlanName(planName);
        plan.setSubscriberType(subscriberType);
        plan.setDescription(description);
        plan.setDurationDays(3650);
        plan.setPrice(BigDecimal.ZERO);
        plan.setCurrency("VND");
        plan.setMaxStudents(maxStudents);
        plan.setMaxCampaignsPerMonth(maxCampaignsPerMonth);
        plan.setMaxRoundsPerCampaign(maxRoundsPerCampaign);
        plan.setMaxSchoolsPerCampaign(maxSchoolsPerCampaign);
        plan.setMaxAiQuizGenerations(maxAiQuizGenerations);
        plan.setGracePeriodDays(30);
        plan.setActive(true);
        plan.setDisplayOrder(0);
        subscriptionPlanRepository.save(plan);

        log.info("Initialized free subscription plan: {}", planCode);
    }

    private void initAdminAccount() {
        String adminEmail = "ecoversesep490@gmail.com";

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists, skipping initialization.");
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setUsername("admin_system");
        admin.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        admin.setRole(Role.ADMINISTRATOR);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setIsActive(true);

        userRepository.save(admin);
        log.info("Admin account created: {}", adminEmail);
    }

    private void initSchoolAccount() {
        String schoolEmail = "school@ecoverse.com";

        if (userRepository.existsByEmail(schoolEmail)) {
            log.info("School account already exists, skipping initialization.");
            return;
        }

        User schoolUser = new User();
        schoolUser.setEmail(schoolEmail);
        schoolUser.setUsername("school_system");
        schoolUser.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        schoolUser.setRole(Role.PARTNERSHIP_SCHOOL);
        schoolUser.setStatus(AccountStatus.ACTIVE);
        schoolUser.setIsActive(true);
        userRepository.save(schoolUser);

        School school = new School();
        school.setUser(schoolUser);
        school.setSchoolName("EcoVerse Demo School");
        school.setSchoolType(SchoolType.PUBLIC);
        school.setTaxCode("0100000000");
        school.setAddress("123 Demo Street, Ha Noi");
        school.setProvince("Ha Noi");
        school.setPhoneNumber("0901234567");
        school.setPrincipalName("Nguyen Van A");
        school.setContactEmail(schoolEmail);
        school.setApprovalStatus(ApprovalStatus.APPROVED);
        schoolRepository.save(school);

        assignFreeSubscriptionToSchoolIfMissing(school, schoolUser);
        log.info("School account created: {}", schoolEmail);
    }

    private void initPartnershipAccount() {
        String partnerEmail = "partnership@ecoverse.com";

        if (userRepository.existsByEmail(partnerEmail)) {
            log.info("Partnership account already exists, skipping initialization.");
            return;
        }

        User partnerUser = new User();
        partnerUser.setEmail(partnerEmail);
        partnerUser.setUsername("partnership_system");
        partnerUser.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        partnerUser.setRole(Role.THIRD_PARTY_PARTNERSHIP);
        partnerUser.setStatus(AccountStatus.ACTIVE);
        partnerUser.setIsActive(true);
        userRepository.save(partnerUser);

        Partnership partnership = new Partnership();
        partnership.setUser(partnerUser);
        partnership.setOrganizationName("EcoVerse Demo Partnership");
        partnership.setPartnershipType(PartnershipType.PUBLIC_ORGANIZATION);
        partnership.setContactEmail(partnerEmail);
        partnership.setPhoneNumber("0912345678");
        partnership.setContactPerson("Tran Van B");
        partnership.setRegisteredAddress("456 Demo Street, Ha Noi");
        partnership.setGeographicScopeProvince("Ha Noi");
        partnership.setApprovalStatus(ApprovalStatus.APPROVED);
        partnershipRepository.save(partnership);

        assignFreeSubscriptionToPartnershipIfMissing(partnership, partnerUser);
        log.info("Partnership account created: {}", partnerEmail);
    }

    private void initParentAccount() {
        String parentEmail = "parent@ecoverse.com";

        if (userRepository.existsByEmail(parentEmail)) {
            log.info("Parent account already exists, skipping initialization.");
            return;
        }

        User parentUser = new User();
        parentUser.setEmail(parentEmail);
        parentUser.setUsername("parent_system");
        parentUser.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        parentUser.setRole(Role.PARENT);
        parentUser.setStatus(AccountStatus.ACTIVE);
        parentUser.setIsActive(true);
        userRepository.save(parentUser);

        Parent parent = new Parent();
        parent.setUser(parentUser);
        parent.setFullName("Pham Thi C");
        parent.setPhoneNumber("0923456789");
        parent.setIsFirstLogin(false);
        parentRepository.save(parent);

        log.info("Parent account created: {}", parentEmail);
    }

    private void initStudentAccount() {
        String studentEmail = "student@ecoverse.com";

        if (userRepository.existsByEmail(studentEmail)) {
            log.info("Student account already exists, skipping initialization.");
            return;
        }

        // Student requires a School - find the demo school
        User schoolUser = userRepository.findByEmail("school@ecoverse.com").orElse(null);
        if (schoolUser == null) {
            log.warn("School account not found, cannot create student account.");
            return;
        }

        School school = schoolRepository.findByUserId(schoolUser.getId()).orElse(null);
        if (school == null) {
            log.warn("School profile not found, cannot create student account.");
            return;
        }

        User studentUser = new User();
        studentUser.setEmail(studentEmail);
        studentUser.setUsername("student_system");
        studentUser.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        studentUser.setRole(Role.STUDENT);
        studentUser.setStatus(AccountStatus.ACTIVE);
        studentUser.setIsActive(true);
        userRepository.save(studentUser);

        Student student = new Student();
        student.setUser(studentUser);
        student.setSchool(school);
        student.setStudentCode("SV000001");
        student.setFullName("Le Van D");
        student.setDateOfBirth(LocalDate.of(2005, 1, 15));
        student.setGender(Gender.MALE);
        student.setGradeLevel("10");
        student.setClassName("10A1");
        student.setTotalCoins(BigDecimal.ZERO);
        student.setIsFirstLogin(false);
        studentRepository.save(student);

        log.info("Student account created: {}", studentEmail);
    }

    private void assignFreeSubscriptionToSchoolIfMissing(School school, User schoolUser) {
        if (subscriptionRepository.findBySchoolIdAndStatus(school.getId(), SubscriptionStatus.ACTIVE).isPresent()) {
            return;
        }

        SubscriptionPlan freePlan = subscriptionPlanRepository.findByPlanCode(SCHOOL_FREE_PLAN_CODE)
                .orElseThrow(() -> new IllegalStateException("Missing free plan: " + SCHOOL_FREE_PLAN_CODE));

        createFreeSubscriptionAndPayment(
                SubscriberType.SCHOOL,
                school,
                null,
                schoolUser,
                freePlan
        );
    }

    private void assignFreeSubscriptionToPartnershipIfMissing(Partnership partnership, User partnerUser) {
        if (subscriptionRepository.findByPartnershipIdAndStatus(partnership.getId(), SubscriptionStatus.ACTIVE).isPresent()) {
            return;
        }

        SubscriptionPlan freePlan = subscriptionPlanRepository.findByPlanCode(PARTNERSHIP_FREE_PLAN_CODE)
                .orElseThrow(() -> new IllegalStateException("Missing free plan: " + PARTNERSHIP_FREE_PLAN_CODE));

        createFreeSubscriptionAndPayment(
                SubscriberType.PARTNERSHIP,
                null,
                partnership,
                partnerUser,
                freePlan
        );
    }

    private void createFreeSubscriptionAndPayment(
            SubscriberType subscriberType,
            School school,
            Partnership partnership,
            User user,
            SubscriptionPlan plan
    ) {
        LocalDateTime now = LocalDateTime.now();

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
        payment.setNotes("Free plan - auto-assigned on initialization");
        paymentRepository.save(payment);
    }
}
