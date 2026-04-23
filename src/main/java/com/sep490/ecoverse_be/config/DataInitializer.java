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
import java.util.HashMap;
import java.util.Map;
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
    private final StudentParentLinkRepository studentParentLinkRepository;
    private final GameTypeRepository gameTypeRepository;
    private final WasteSubCategoryRepository wasteSubCategoryRepository;
    private final WasteItemRepository wasteItemRepository;

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
        linkParentWithStudentIfMissing();
        initGameTypes();
        initWasteSubCategories();
        initWasteItems();
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
        admin.setUsername(null);
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
        schoolUser.setUsername(null);
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
        partnerUser.setUsername(null);
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
        String parentUsername = "0901922117";

        if (userRepository.existsByEmail(parentEmail)) {
            log.info("Parent account already exists, skipping initialization.");
            return;
        }

        User parentUser = new User();
        parentUser.setEmail(parentEmail);
        parentUser.setUsername(parentUsername);
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
        String userName = "LVD";

        if (userRepository.existsByUsername(userName)) {
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
        studentUser.setUsername(userName);
        studentUser.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        studentUser.setRole(Role.STUDENT);
        studentUser.setStatus(AccountStatus.ACTIVE);
        studentUser.setIsActive(true);
        userRepository.save(studentUser);

        Student student = new Student();
        student.setUser(studentUser);
        student.setSchool(school);
        student.setStudentCode(userName);
        student.setFullName("Le Van D");
        student.setDateOfBirth(LocalDate.of(2005, 1, 15));
        student.setGender(Gender.MALE);
        student.setGradeLevel("5");
        student.setClassName("5A1");
        student.setTotalCoins(BigDecimal.ZERO);
        student.setIsFirstLogin(false);
        studentRepository.save(student);

        log.info("Student account created: {}", userName);
    }

    private void linkParentWithStudentIfMissing() {
        String parentEmail = "parent@ecoverse.com";
        String studentUsername = "LVD";

        User parentUser = userRepository.findByEmail(parentEmail).orElse(null);
        User studentUser = userRepository.findByUsername(studentUsername).orElse(null);

        if (parentUser == null || studentUser == null) {
            log.warn("Parent or Student user not found, cannot create link.");
            return;
        }

        Parent parent = parentRepository.findByUserId(parentUser.getId()).orElse(null);
        Student student = studentRepository.findByUserId(studentUser.getId()).orElse(null);

        if (parent == null || student == null) {
            log.warn("Parent or Student entity not found, cannot create link.");
            return;
        }

        boolean exists = studentParentLinkRepository
                .existsByStudentIdAndParentId(student.getId(), parent.getId());

        if (exists) {
            log.info("Student-Parent link already exists, skipping.");
            return;
        }

        StudentParentLink link = new StudentParentLink();
        link.setStudent(student);
        link.setParent(parent);

        studentParentLinkRepository.save(link);

        log.info("Linked parent [{}] with student [{}]", parentEmail, studentUsername);
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

    // ── Game Types ─────────────────────────────────────────────────────────────

    private void initGameTypes() {
        User admin = userRepository.findByEmail("ecoversesep490@gmail.com").orElse(null);

        createGameTypeIfMissing(
                GameTypeCode.RUN_SORTING,
                "Eco Runner",
                "Chạy đua và phân loại rác trên đường phố.",
                "Eco Runner là trò chơi hành động tốc độ cao, nơi người chơi điều khiển nhân vật chạy qua các khu vực thành phố, thu gom và phân loại rác thải vào đúng thùng quy định để làm sạch môi trường.",
                "Sử dụng các phím mũi tên hoặc thao tác vuốt để di chuyển trái/phải, né tránh chướng ngại vật và chạm vào các vật phẩm rác để bỏ vào thùng tương ứng.",
                true, 50, 1, admin
        );

        createGameTypeIfMissing(
                GameTypeCode.COLLECT_SORTING,
                "Sea Rescue",
                "Giải cứu đại dương khỏi rác thải nhựa.",
                "Sea Rescue đưa bạn xuống lòng đại dương sâu thẳm. Nhiệm vụ của bạn là điều khiển tàu ngầm thu gom các loại rác trôi nổi và bảo vệ sinh vật biển khỏi sự ô nhiễm.",
                "Chạm và giữ để điều khiển tàu ngầm thu thập rác. Phân loại rác ngay trên tàu trước khi khoang chứa đầy để tiếp tục hành trình.",
                true, 40, 2, admin
        );
    }

    private void createGameTypeIfMissing(
            GameTypeCode typeCode, String name, String shortDesc, String fullDesc,
            String howToPlay, boolean supportsCoin, int maxLevels, int displayOrder, User admin
    ) {
        if (gameTypeRepository.existsByTypeCodeAndIsDeleteFalse(typeCode)) {
            return;
        }

        GameType gt = new GameType();
        gt.setTypeCode(typeCode);
        gt.setName(name);
        gt.setShortDescription(shortDesc);
        gt.setFullDescription(fullDesc);
        gt.setHowToPlay(howToPlay);
        gt.setSupportsCoin(supportsCoin);
        gt.setMaxLevels(maxLevels);
        gt.setDisplayOrder(displayOrder);
        gt.setActive(true);
        gt.setDelete(false);
        gt.setFeatures(new HashMap<>());
        gt.setCreatedBy(admin);
        gt.setUpdatedBy(admin);
        gameTypeRepository.save(gt);

        log.info("Initialized GameType: {}", typeCode);
    }

    // ── Waste Sub-Categories ──────────────────────────────────────────────────

    private void initWasteSubCategories() {
        User admin = userRepository.findByEmail("ecoversesep490@gmail.com").orElse(null);

        // GENERAL
        createSubCategoryIfMissing(WasteCategory.GENERAL, "TEXTILE_WASTE", "Rác thải Vải",
                "Các loại vải trong nhà không sử dụng nữa", 1, admin);
        createSubCategoryIfMissing(WasteCategory.GENERAL, "HOUSE_WASTE", "Rác gia dụng",
                "Các loại rác sinh hoạt thường ngày trong nhà, có thể xuất phát từ đồ dùng hư hỏng...", 1, admin);

        // RECYCLABLE
        createSubCategoryIfMissing(WasteCategory.RECYCLABLE, "PAPER", "Rác thải Giấy",
                "Rác thải giấy có thể tái chế được", 3, admin);
        createSubCategoryIfMissing(WasteCategory.RECYCLABLE, "METAL", "Rác thải Kim Loại",
                "Các loại rác kim loại có khả năng tái chế", 0, admin);
        createSubCategoryIfMissing(WasteCategory.RECYCLABLE, "PLASTIC", "Rác thải Nhựa",
                "Rác thải nhựa có thể tái chế được", 1, admin);
        createSubCategoryIfMissing(WasteCategory.RECYCLABLE, "GLASS", "Rác Thủy Tinh",
                "Rác thủy tinh có thể dùng được", 2, admin);

        // ORGANIC
        createSubCategoryIfMissing(WasteCategory.ORGANIC, "FOOD_WASTE", "Rác thải thực phẩm",
                "Các loại rác liên quan đến đồ ăn", 1, admin);
        createSubCategoryIfMissing(WasteCategory.ORGANIC, "GARDEN_WASTE", "Rác thải sân vườn",
                "Các loại rác thải được tìm thấy trong sân vườn", 1, admin);

        // HAZARDOUS
        createSubCategoryIfMissing(WasteCategory.HAZARDOUS, "TECH_WASTE", "Rác thải điện tử",
                "Các đồ gia dụng điện tử không sử dụng được nữa", 1, admin);
        createSubCategoryIfMissing(WasteCategory.HAZARDOUS, "CHEMICAL_WASTE", "Rác thải hóa học",
                "Các hóa chất độc hại không sử dụng", 1, admin);
        createSubCategoryIfMissing(WasteCategory.HAZARDOUS, "MEDICAL_WASTE", "Rác thải Y học lây nhiễm",
                "Các loại dược phẩm, chất thải lây nhiễm", 1, admin);
        createSubCategoryIfMissing(WasteCategory.HAZARDOUS, "EXPLOSIVE_WASTE", "Rác thải gây nổ",
                "Các bình áp suất, chất gây nổ", 1, admin);
    }

    private void createSubCategoryIfMissing(
            WasteCategory category, String subCategoryCode, String displayName,
            String description, int displayOrder, User admin
    ) {
        if (wasteSubCategoryRepository.existsByCategoryAndSubCategoryCodeAndIsDeleteFalse(category, subCategoryCode)) {
            return;
        }

        WasteSubCategory sc = new WasteSubCategory();
        sc.setCategory(category);
        sc.setSubCategoryCode(subCategoryCode);
        sc.setDisplayName(displayName);
        sc.setDescription(description);
        sc.setDisplayOrder(displayOrder);
        sc.setActive(true);
        sc.setDelete(false);
        sc.setCreatedBy(admin);
        wasteSubCategoryRepository.save(sc);

        log.info("Initialized WasteSubCategory: {} / {}", category, subCategoryCode);
    }

    // ── Waste Items ───────────────────────────────────────────────────────────

    private void initWasteItems() {
        User admin = userRepository.findByEmail("ecoversesep490@gmail.com").orElse(null);

        // TECH_WASTE
        createWasteItemIfMissing("TECH_WASTE", WasteCategory.HAZARDOUS, "Điện thoại cũ",
                "Các thiết bị điện thoại di động đã hỏng hoặc không còn sử dụng được.",
                "Trong 1 tấn điện thoại cũ có chứa lượng vàng nhiều gấp 100 lần so với 1 tấn quặng vàng thô đấy!",
                "500 - 1000 năm",
                "Đừng vứt vào thùng rác nhà mình nhé. Hãy mang đến các điểm thu gom rác điện tử để bác thợ lấy lại kim loại quý.",
                admin);

        // CHEMICAL_WASTE
        createWasteItemIfMissing("CHEMICAL_WASTE", WasteCategory.HAZARDOUS, "Thùng chất thải hóa học",
                "Các loại chai, thùng chứa hóa chất độc hại, thuốc tẩy hoặc dung môi công nghiệp.",
                "Một giọt hóa chất độc hại có thể làm ô nhiễm hàng nghìn lít nước sạch dưới lòng đất.",
                "50 - 100 năm",
                "Tuyệt đối không dùng để đựng nước hay đồ ăn. Phải để người lớn xử lý tại các trạm rác nguy hại chuyên dụng.",
                admin);

        // FOOD_WASTE
        createWasteItemIfMissing("FOOD_WASTE", WasteCategory.ORGANIC, "Trái cây thừa",
                "Vỏ trái cây, hạt hoặc phần trái cây bị hỏng không ăn được nữa.",
                "Vỏ cam, quýt có thể đuổi muỗi và làm sạch vết bẩn trên bàn ghế cực tốt luôn.",
                "2 - 5 tuần",
                "Băm nhỏ rồi ủ làm phân bón cho cây trong vườn sẽ giúp cây lớn nhanh và khỏe mạnh.",
                admin);

        // PAPER
        createWasteItemIfMissing("PAPER", WasteCategory.RECYCLABLE, "Ly cà phê giấy",
                "Ly giấy dùng một lần, thường có lớp nhựa mỏng bên trong để không bị thấm nước.",
                "Vì có lớp nhựa dính chặt vào giấy nên việc tái chế chiếc ly này khó hơn giấy bình thường rất nhiều.",
                "20 - 30 năm",
                "Hạn chế dùng ly một lần nhé! Hãy tự mang theo bình nước cá nhân để bảo vệ môi trường.",
                admin);

        // TEXTILE_WASTE
        createWasteItemIfMissing("TEXTILE_WASTE", WasteCategory.GENERAL, "Quần áo cũ",
                "Vải vụn, quần áo đã rách hoặc quá chật không thể mặc được nữa.",
                "Để làm ra một chiếc áo phông cotton, người ta phải dùng tới 2.700 lít nước, đủ cho một người uống trong 2,5 năm!",
                "200 năm (vải tổng hợp)",
                "Áo cũ có thể cắt ra làm giẻ lau sàn hoặc may thành những chiếc túi vải xinh xắn.",
                admin);

        // GLASS
        createWasteItemIfMissing("GLASS", WasteCategory.RECYCLABLE, "Tô thủy tinh",
                "Đồ dùng bằng thủy tinh bị nứt, vỡ hoặc không dùng đến.",
                "Thủy tinh là vật liệu tuyệt vời vì có thể tái chế đi tái chế lại mãi mãi mà không bao giờ cũ.",
                "1 triệu năm",
                "Nếu bị vỡ, hãy nhờ người lớn gói thật kỹ vào báo cũ để các cô chú lao công không bị đứt tay.",
                admin);

        // PLASTIC
        createWasteItemIfMissing("PLASTIC", WasteCategory.RECYCLABLE, "Chai nước nhựa",
                "Chai nhựa đựng nước ngọt, nước suối sau khi uống hết.",
                "Cứ mỗi phút trên thế giới lại có khoảng 1 triệu chai nhựa được bán ra.",
                "450 năm",
                "Hãy bóp dẹp chai trước khi bỏ vào thùng rác tái chế để tiết kiệm chỗ trống nhé.",
                admin);

        // PAPER
        createWasteItemIfMissing("PAPER", WasteCategory.RECYCLABLE, "Giấy vo viên",
                "Các tờ giấy nháp, giấy bài tập bị vo tròn lại.",
                "Tái chế 1 tấn giấy sẽ cứu được 17 cây xanh không bị chặt hạ đấy.",
                "2 - 6 tuần",
                "Đừng vo viên! Hãy vuốt thẳng giấy và gom lại để bán giấy vụn, như vậy giấy sẽ dễ tái chế hơn.",
                admin);

        // HOUSE_WASTE
        createWasteItemIfMissing("HOUSE_WASTE", WasteCategory.GENERAL, "Giày cũ",
                "Giày dép đã hư hỏng, rách nát không thể sửa chữa.",
                "Bạn có thể dùng chiếc giày cũ để làm một chậu hoa độc đáo cho ban công nhà mình.",
                "25 - 40 năm",
                "Nếu giày còn tốt, hãy giặt sạch và tặng cho các bạn nhỏ khó khăn hơn mình nhé.",
                admin);

        // PAPER
        createWasteItemIfMissing("PAPER", WasteCategory.RECYCLABLE, "Vở ghi cũ",
                "Sách vở đã học xong hoặc bị rách nát.",
                "Giấy có thể tái chế khoảng 5 đến 7 lần trước khi các sợi giấy trở nên quá yếu.",
                "2 - 5 tháng",
                "Những trang giấy trắng còn thừa, bạn hãy đóng lại thành một cuốn sổ nháp nhỏ để vẽ nhé.",
                admin);

        // FOOD_WASTE
        createWasteItemIfMissing("FOOD_WASTE", WasteCategory.ORGANIC, "Vỏ chuối",
                "Phần vỏ của quả chuối sau khi đã ăn xong.",
                "Mặt trong của vỏ chuối có thể dùng để lau bóng giày da cực kỳ hiệu quả.",
                "2 - 5 tuần",
                "Bỏ vào thùng rác hữu cơ hoặc chôn xuống đất để làm thức ăn cho các bạn giun đất.",
                admin);

        // MEDICAL_WASTE
        createWasteItemIfMissing("MEDICAL_WASTE", WasteCategory.HAZARDOUS, "Rác khẩu trang",
                "Khẩu trang y tế đã qua sử dụng.",
                "Mỗi tháng, thế giới sử dụng khoảng 129 tỷ chiếc khẩu trang, nếu vứt bừa bãi sẽ rất hại cho sinh vật biển.",
                "450 năm",
                "Hãy cắt đứt dây đeo trước khi vứt để tránh làm các bạn chim hoặc cá bị mắc kẹt.",
                admin);

        // HOUSE_WASTE
        createWasteItemIfMissing("HOUSE_WASTE", WasteCategory.GENERAL, "Bút chì",
                "Bút chì gỗ đã quá ngắn không thể cầm viết được nữa.",
                "Lõi bút chì làm từ than chì và đất sét, không phải làm từ kim loại chì nên rất an toàn.",
                "10 - 50 năm",
                "Phần vụn gỗ khi gọt bút chì có thể dùng để trang trí tranh vẽ hoặc ủ làm phân bón.",
                admin);

        // EXPLOSIVE_WASTE
        createWasteItemIfMissing("EXPLOSIVE_WASTE", WasteCategory.HAZARDOUS, "Bình Gas",
                "Các loại bình chứa khí gas dùng trong nấu nướng.",
                "Bình gas rất cứng cáp nhưng nếu bị rò rỉ khí bên trong thì có thể gây nổ rất nguy hiểm.",
                "50 - 100 năm",
                "Khi hết gas, hãy gọi các chú ở cửa hàng đến đổi bình mới, tuyệt đối không tự ý nghịch hay đập phá bình.",
                admin);

        // TECH_WASTE
        createWasteItemIfMissing("TECH_WASTE", WasteCategory.HAZARDOUS, "Sạc pin điện thoại cũ",
                "Dây sạc, củ sạc đã hỏng hoặc không dùng tới.",
                "Dây sạc chứa những sợi đồng nhỏ bên trong có khả năng dẫn điện rất tốt.",
                "100 - 500 năm",
                "Nên dùng bộ sạc chính hãng để bền hơn. Nếu hỏng, hãy gom lại cùng với các loại rác điện tử khác.",
                admin);
    }

    private void createWasteItemIfMissing(
            String subCategoryCode, WasteCategory category, String itemName,
            String description, String funFact, String decompositionTime,
            String recyclingTips, User admin
    ) {
        // Look up the sub-category
        WasteSubCategory subCategory = wasteSubCategoryRepository
                .findByIsDeleteFalse().stream()
                .filter(sc -> sc.getSubCategoryCode().equals(subCategoryCode) && sc.getCategory() == category)
                .findFirst().orElse(null);

        if (subCategory == null) {
            log.warn("WasteSubCategory not found: {} / {}, skipping WasteItem: {}", category, subCategoryCode, itemName);
            return;
        }

        // Check if item already exists
        if (wasteItemRepository.existsByItemNameIgnoreCaseAndSubCategoryIdAndIsDeleteFalse(itemName, subCategory.getId())) {
            return;
        }

        WasteItem item = new WasteItem();
        item.setItemName(itemName);
        item.setCategory(category);
        item.setSubCategory(subCategory);
        item.setDescription(description);
        item.setFunFact(funFact);
        item.setDecompositionTime(decompositionTime);
        item.setRecyclingTips(recyclingTips);
        item.setActive(true);
        item.setDelete(false);
        item.setCreatedBy(admin);
        wasteItemRepository.save(item);

        log.info("Initialized WasteItem: {} [{}]", itemName, subCategoryCode);
    }
}
