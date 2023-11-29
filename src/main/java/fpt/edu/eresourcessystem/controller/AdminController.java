package fpt.edu.eresourcessystem.controller;

import fpt.edu.eresourcessystem.controller.advices.GlobalControllerAdvice;
import fpt.edu.eresourcessystem.dto.AccountDto;
import fpt.edu.eresourcessystem.dto.TrainingTypeDto;
import fpt.edu.eresourcessystem.enums.AccountEnum;
import fpt.edu.eresourcessystem.enums.CommonEnum;
import fpt.edu.eresourcessystem.model.*;
import fpt.edu.eresourcessystem.service.*;
import fpt.edu.eresourcessystem.utils.CommonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.*;

@Controller
@RequestMapping("/admin")
@PropertySource("web-setting.properties")
public class AdminController {
    @Value("${page-size}")
    private Integer pageSize;

    private final AccountService accountService;
    private final AdminService adminService;
    private final LibrarianService librarianService;
    private final LecturerService lecturerService;
    private final StudentService studentService;
    private final CourseService courseService;
    private final TopicService topicService;
    private final DocumentService documentService;
    private final UserLogService userLogService;
    private final FeedbackService feedbackService;

    private final TrainingTypeService trainingTypeService;

    public AdminController(AccountService accountService, AdminService adminService,
                           LibrarianService librarianService, LecturerService lecturerService,
                           StudentService studentService, CourseService courseService,
                           TopicService topicService, DocumentService documentService, UserLogService userLogService,
                           FeedbackService feedbackService, TrainingTypeService trainingTypeService) {
        this.accountService = accountService;
        this.adminService = adminService;
        this.librarianService = librarianService;
        this.lecturerService = lecturerService;
        this.studentService = studentService;
        this.courseService = courseService;
        this.topicService = topicService;
        this.documentService = documentService;
        this.userLogService = userLogService;
        this.feedbackService = feedbackService;
        this.trainingTypeService = trainingTypeService;
    }

    /*
    DASHBOARD
     */

    /**
     * @return dashboard page
     */
    @GetMapping
    public String getLibrarianDashboard() {
        return "admin/admin_dashboard";
    }

    /*
        ACCOUNTS MANAGEMENT
     */

    /**
     * @return accounts page (no data)
     */
    @GetMapping({"/accounts/list"})
    public String manageAccount() {
        return "admin/account/admin_accounts";
    }


    @GetMapping("/systemLog")
    String manageSystemLog(final Model model) {
        return "admin/system_log/admin_system_logs";
    }

    @GetMapping("/systemLog/userLog")
    String viewUserLog(final Model model) {
        return "admin/system_log/admin_user_logs";
    }

    @GetMapping("/systemLog/courseLog")
    String viewCourseLog(final Model model) {
        return "admin/system_log/admin_course_logs";
    }

    @GetMapping("/systemLog/documentLog")
    String viewDocumentLog(final Model model) {
        return "admin/system_log/admin_document_logs";
    }

    @GetMapping("/trainingTypeManagement")
    String manageTrainingType(final Model model) {
        return "admin/training_type_management/admin_training_type_management";
    }

    /*
    This function to display detail of courses for admin
     */
    @GetMapping("/course/{courseId}")
    String getCourseByLibrarian(@PathVariable String courseId, final Model model) {
        return "admin/course_creator/admin_course_detail";
    }

    /*
    This function to display librarians and created course by that librarians
     */
    @GetMapping("/course_creator/list")
    String findCourseByLibrarianList(final Model model) {

        List<Account> librarianList = accountService.findAllLibrarian();
        for (int i = 0; i < librarianList.size(); i++) {
            String librarianId = librarianList.get(i).getId();

            Librarian librarian = librarianService.findByAccountId(librarianId);
            if (null == librarian) {
                return "exception/404";
            }
            List<Course> courses = librarian.getCreatedCourses();

            System.out.println(courses.size());
            model.addAttribute("librarianList", librarianList);
            model.addAttribute("librarians", librarian);
            model.addAttribute("courses", courses);
        }

        return "admin/course_creator/admin_course_creators";
    }

    @GetMapping("/course_creator")
    String findCourseByLibrarian(final Model model) {
        return "admin/course_creator/admin_course_creators";
    }



    @GetMapping("/accounts/list/{pageIndex}")
    String findAccountByPage(@PathVariable Integer pageIndex,
                             @RequestParam(required = false, defaultValue = "") String search,
                             @RequestParam(required = false, defaultValue = "all") String role,
                             final Model model) {
        Page<Account> page;
        if (null == role || "all".equals(role)) {
            page = accountService.findByUsernameLikeOrEmailLike(search, search, pageIndex, pageSize);
        } else {
            page = accountService.findByRoleAndUsernameLikeOrEmailLike(role, search, search, pageIndex, pageSize);
        }
        List<Integer> pages = CommonUtils.pagingFormat(page.getTotalPages(), pageIndex);
        model.addAttribute("pages", pages);
        model.addAttribute("totalPage", page.getTotalPages());
        model.addAttribute("accounts", page.getContent());
        model.addAttribute("search", search);
        model.addAttribute("roles", AccountEnum.Role.values());
        model.addAttribute("roleSearch", role);
        model.addAttribute("currentPage", pageIndex);
        return "admin/account/admin_accounts";
    }

    /**
     * @param accountDTO
     * @param model
     * @return
     */
    @GetMapping("/accounts/add")
    public String addAccountProcess(@ModelAttribute AccountDto accountDTO, final Model model) {
        model.addAttribute("account", Objects.requireNonNullElseGet(accountDTO, Account::new));
        model.addAttribute("roles", AccountEnum.Role.values());
        model.addAttribute("campuses", AccountEnum.Campus.values());
        model.addAttribute("genders", AccountEnum.Gender.values());
        return "admin/account/admin_add-account";
    }

    /**
     * @param accountDTO
     * @return
     */
    @PostMapping("/accounts/add")
    public String addAccount(@ModelAttribute AccountDto accountDTO) {
        String email = accountDTO.getEmail();
        AccountEnum.Role role = accountDTO.getRole();
        if (accountService.findByEmail(email) != null) {
            return "redirect:/admin/accounts/add?error";
        }
        Account account = accountService.addAccount(accountDTO);
        switch (role) {
            case ADMIN:
                Admin admin = new Admin();
                admin.setAccount(account);
                adminService.addAdmin(admin);
                break;
            case LIBRARIAN:
                Librarian librarian = new Librarian();
                librarian.setAccount(account);
                librarianService.addLibrarian(librarian);
                break;
            case STUDENT:
                Student student = new Student();
                student.setAccount(account);
                studentService.addStudent(student);
                break;
            case LECTURER:
                Lecturer lecturer = new Lecturer();
                lecturer.setAccount(account);
                lecturerService.addLecturer(lecturer);
                break;
            default:
                return "redirect:/admin/accounts/add?error";
        }

        RedirectAttributes attributes = new RedirectAttributesModelMap();
        attributes.addFlashAttribute("account", accountDTO);
        return "redirect:/admin/accounts/add?success";
    }

    /**
     * @param accountId
     * @param model
     * @return
     */
    @GetMapping({"/accounts/{accountId}/update", "/accounts/updated/{accountId}"})
    public String updateAccountProcess(@PathVariable(required = false) String accountId, final Model model) {
        if (null == accountId) {
            accountId = "";
        }
        Account account = accountService.findById(accountId);
        if (null == account) {
            return "exception/404";
        } else {
            if (AccountEnum.Role.LIBRARIAN.equals(account.getRole())) {
                Librarian librarian = librarianService.findByAccountId(accountId);
            }
            model.addAttribute("roles", AccountEnum.Role.values());
            model.addAttribute("campuses", AccountEnum.Campus.values());
            model.addAttribute("genders", AccountEnum.Gender.values());
            model.addAttribute("account", account);
            return "admin/account/admin_update-account";
        }
    }

    /**
     * @param accountDTO
     * @param model
     * @return
     */
    @PostMapping("/accounts/update")
    public String updateAccount(@ModelAttribute AccountDto accountDTO,
                                final Model model) {
        Account checkExist = accountService.findById(accountDTO.getId());
        if (null == checkExist) {
            model.addAttribute("errorMessage", "account not exist.");
            return "exception/404";
        } else {
            Account checkEmailDuplicate = accountService.findByEmail(accountDTO.getEmail());
            if (checkEmailDuplicate != null &&
                    !checkExist.getEmail().equalsIgnoreCase(accountDTO.getEmail())) {
                String result = "redirect:/admin/accounts/updated/" + accountDTO.getId() + "?error";
                return result;
            }
            AccountEnum.Role role = accountDTO.getRole();
            Account account = new Account(accountDTO);
            System.out.println(role);
            switch (role) {
                case ADMIN:
                    if (null == adminService.findByAccountId(account.getId())) {
                        Admin admin = new Admin();
                        admin.setAccount(account);
                        adminService.updateAdmin(admin);
                    }
                    break;
                case LIBRARIAN:
                    Librarian librarian = librarianService.findByAccountId(accountDTO.getId());
                    if (librarian == null) {
                        librarian = new Librarian();
                        librarian.setAccount(account);
                        librarianService.addLibrarian(librarian);
                    } else {
                        librarianService.updateLibrarian(librarian);
                    }
                    break;
                case STUDENT:
                    if (null == studentService.findByAccountId(account.getId())) {
                        Student student = new Student();
                        student.setAccount(account);
                        studentService.updateStudent(student);
                    }
                    break;
                case LECTURER:
                    if (null == lecturerService.findByAccountId(account.getId())) {
                        Lecturer lecturer = new Lecturer();
                        lecturer.setAccount(account);
                        lecturerService.updateLecturer(lecturer);
                    }
                    break;
                default:
                    return "redirect:/admin/accounts/updated/" + account.getId() + "?error";
            }
            accountService.updateAccount(account);
            model.addAttribute("account", account);
            model.addAttribute("roles", AccountEnum.Role.values());
            model.addAttribute("campuses", AccountEnum.Campus.values());
            model.addAttribute("genders", AccountEnum.Gender.values());
            model.addAttribute("success", "");
            String result = "redirect:/admin/accounts/updated/" + account.getId() + "?success";
            return result;
        }
    }

    /**
     * Delete an account by accountId
     *
     * @param accountId id of account that delete
     * @return list of accounts after delete
     */
    @GetMapping("/accounts/{accountId}/delete")
    @Transactional
    public String deleteAccount(@PathVariable String accountId) {
        Account foundAccount = accountService.findById(accountId);
        AccountEnum.Role role = foundAccount.getRole();
        if (null != foundAccount) {
            // SOFT DELETE

            switch (role) {
                case ADMIN:
                    Admin admin = adminService.findByAccountId(accountId);
                    admin.setDeleteFlg(CommonEnum.DeleteFlg.DELETED);
                    adminService.updateAdmin(admin);
                    break;
                case LIBRARIAN:
                    Librarian librarian = librarianService.findByAccountId(accountId);
                    librarian.setDeleteFlg(CommonEnum.DeleteFlg.DELETED);
                    librarianService.updateLibrarian(librarian);

                    break;
                case LECTURER:
                    Lecturer lecturer = lecturerService.findByAccountId(accountId);
                    lecturer.setDeleteFlg(CommonEnum.DeleteFlg.DELETED);
                    lecturerService.updateLecturer(lecturer);
                    // Xóa quyền quản lý môn học + ghi log
                    // Xóa mềm topic đã tạo
                    // Xóa mềm resource type đã tạo
                    // Xóa mềm tài liệu đã đăng

                    break;
                case STUDENT:
                    Student student = studentService.findByAccountId(accountId);
                    student.setDeleteFlg(CommonEnum.DeleteFlg.DELETED);
                    studentService.updateStudent(student);
                    // Xóa mềm note on document
                    // Xóa mềm question của student
                    // Xóa mềm saved course
                    // Xóa mềm saved document
                    // Xóa mềm student self note

                    break;
            }
            foundAccount.setDeleteFlg(CommonEnum.DeleteFlg.DELETED);
            accountService.updateAccount(foundAccount);

            return "redirect: /admin/accounts/list?success";
        } else {
            return "redirect:/admin/accounts/" + accountId + "/update?error";
        }
    }

    @GetMapping({"/user_log/tracking"})
    public String userLogManage(@RequestParam(required = false, defaultValue = "") String search,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
                                @RequestParam(required = false, defaultValue = "all") String roleSearch,
                                final Model model) {
        List<UserLog> userLogs = userLogService.findAll();
        model.addAttribute("userLogs", userLogs);
        model.addAttribute("roles", AccountEnum.Role.values());
        model.addAttribute("roleSearch", roleSearch);
        return "admin/system_log/admin_user_logs";
    }

    @GetMapping("/course_log/tracking")
    public String courseLogManage() {
        return "admin/system_log/admin_course_logs";
    }

    @GetMapping("/feedbacks/list/{pageIndex}")
    String showFeedbacksByPage(@PathVariable Integer pageIndex,
                               @RequestParam(required = false, defaultValue = "") String search,
                               final Model model, HttpServletRequest request) {
        Page<Course> page;
//        page = courseService.findByCodeOrNameOrDescription(search, search, search, pageIndex, pageSize);
//        List<Integer> pages = CommonUtils.pagingFormat(page.getTotalPages(), pageIndex);
//        model.addAttribute("pages", pages);
//        model.addAttribute("totalPage", page.getTotalPages());
//        model.addAttribute("courses", page.getContent());
//        model.addAttribute("search", search);
//        model.addAttribute("currentPage", pageIndex);
        return "librarian/course/librarian_courses";
    }

    @GetMapping({"/feedbacks"})
    public String showFeedbacks(final Model model) {
//        List<Feedback> feedbacks = feedbackService.findAll();
//        model.addAttribute("feedbacks", feedbacks);
        return "admin/feedback/admin_feedbacks";
//        return  "librarian/course/detailCourseTest";
    }

    @GetMapping({"/feedbacks/list"})
    public String showFeedbacksList(final Model model) {
        List<Feedback> feedbacks = feedbackService.findAll();
        model.addAttribute("feedbacks", feedbacks);
        return "admin/feedback/admin_feedbacks";
//        return  "librarian/course/detailCourseTest";
    }

    @GetMapping("/feedbacks/{id}")
    public String showFeedbackDetail(@PathVariable("id") String id, Model model) {
        Feedback feedback = feedbackService.getFeedbackById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found for this id :: " + id));
        model.addAttribute("feedback", feedback);
        return "admin/feedback/admin_feedback-detail"; // Name of your Thymeleaf template for feedback detail
    }


    @GetMapping("/trainingtypes/add")
    public String showAddForm(Model model) {
        List<Course> allCourses = courseService.findAll(); // Retrieve all available courses
        model.addAttribute("allCourses", allCourses);
        model.addAttribute("trainingType", new TrainingType());
        return "admin/training_type/admin_training-type-add";
    }

    @PostMapping("/trainingtypes/add")
    public String addTrainingType(@ModelAttribute("trainingtype") @Valid TrainingTypeDto trainingType,
                                  BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Validation failed. Please check your input.");
            return "redirect:/admin/trainingtypes/add";
        }
        try {
            TrainingType trainingType1 =  new TrainingType(trainingType);
            TrainingType save = trainingTypeService.save(trainingType1);
            redirectAttributes.addFlashAttribute("success", "Training Type added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error occurred while adding Training Type.");
        }
        return "redirect:/admin/trainingtypes/add";
    }


    @GetMapping("/trainingtypes/list")
    public String listTrainingTypes(Model model) {
        model.addAttribute("trainingTypes", trainingTypeService.findAll());
        return "admin/training_type/admin_training-type"; // Thymeleaf template for listing training types
    }


//    @GetMapping("/trainingtypes/list")
//    public DataTableResponse listTrainingTypes(
//            @RequestParam int draw,
//            @RequestParam int start,
//            @RequestParam int length,
//            @RequestParam(required = false) String search
//    ) {
//        Pageable pageable = PageRequest.of(start / length, length);
//
//        Page<TrainingType> page = trainingTypeService.findAllWithFilter(search, pageable);
//
//        DataTableResponse response = new DataTableResponse();
//        response.setDraw(draw);
//        response.setRecordsTotal(page.getTotalElements());
//        response.setRecordsFiltered(page.getTotalElements());
//        response.setData(page.getContent());
//
//        return response;
//    }


    @GetMapping("/trainingtypes")
    public String listTrainingTypesNone(Model model) {
        return "admin/training_type/admin_training-type";
    }

    @GetMapping("/trainingtypes/{id}")
    public String viewTrainingTypeDetail(@PathVariable("id") String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<TrainingType> trainingTypeOptional = trainingTypeService.findById(id);
        if (trainingTypeOptional.isPresent()) {
            model.addAttribute("trainingType", trainingTypeOptional.get());
            return "admin/training_type/admin_training-type-detail"; // Thymeleaf template for the details view
        } else {
            redirectAttributes.addFlashAttribute("error", "Training type not found!");
            return "redirect:/admin/trainingtypes/list";
        }
    }

    @PostMapping("/trainingtypes/update")
    public String updateTrainingType(@ModelAttribute TrainingType trainingType,
                                     RedirectAttributes redirectAttributes) {
        try {
            trainingTypeService.updateTrainingType(trainingType);
            redirectAttributes.addFlashAttribute("success", "Training type updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating training type: " + e.getMessage());
        }
        return "redirect:/admin/trainingtypes/"+trainingType.getId();
    }


    //
    @PostMapping("/trainingtypes/delete/{id}")
    public String deleteTrainingType(@PathVariable("id") String  id,
                                     RedirectAttributes redirectAttributes) {
        try {
            trainingTypeService.deleteById(String.valueOf(id));
            redirectAttributes.addFlashAttribute("success", "Training type deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting training type: " + e.getMessage());
        }
        return "redirect:/admin/trainingtypes/list";
    }

    @GetMapping("/trainingtypes/delete/{id}")
    public String softDeleteTrainingType(@PathVariable("id") String id,
                                         RedirectAttributes redirectAttributes) {
        Optional<TrainingType> trainingType = trainingTypeService.findById(String.valueOf(id));
        if (trainingType.isPresent()) {
            boolean isDeleted = trainingTypeService.softDelete(trainingType.get());
            if (isDeleted) {
                redirectAttributes.addFlashAttribute("success", "Training type was successfully deleted.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to delete training type.");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Training type not found.");
        }
        return "redirect:/admin/trainingtypes/list";
    }

    @GetMapping("/document_log/tracking")
    public String documentLogManage() {
        return "admin/system_log/admin_document_logs";
    }


    /*
        Login as
     */

    @GetMapping("/login_as_student")
    public String loginAsStudent(Model model) {
        Account loggedInAccount = GlobalControllerAdvice.getLoggedInAccount();
        if (loggedInAccount != null) {
            Student existStudent = studentService.findByAccountId(loggedInAccount.getId());
            if(existStudent == null){
                Student student = new Student();
                student.setAccount(loggedInAccount);
                studentService.addStudent(student);
            }
        }
        return "redirect:/student";
    }

    @GetMapping("/login_as_lecturer")
    public String loginAsLecturer() {
        Account loggedInAccount = GlobalControllerAdvice.getLoggedInAccount();
        if (loggedInAccount != null) {
            Lecturer existLecturer = lecturerService.findByAccountId(loggedInAccount.getId());
            if(existLecturer == null){
                Lecturer lecturer = new Lecturer();
                lecturer.setAccount(loggedInAccount);
                lecturerService.addLecturer(lecturer);
            }
        }
        return "redirect:/lecturer";
    }

    @GetMapping("/login_as_librarian")
    public String loginAsLibrarian() {
        Account loggedInAccount = GlobalControllerAdvice.getLoggedInAccount();
        if (loggedInAccount != null) {
            Librarian existLibrarian = librarianService.findByAccountId(loggedInAccount.getId());
            if(existLibrarian == null){
                Librarian librarian = new Librarian();
                librarian.setAccount(loggedInAccount);
                librarianService.addLibrarian(librarian);
            }
        }
        return "redirect:/librarian";
    }
}