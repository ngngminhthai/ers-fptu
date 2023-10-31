package fpt.edu.eresourcessystem.controller;

import fpt.edu.eresourcessystem.dto.CourseDTO;
import fpt.edu.eresourcessystem.enums.AccountEnum;
import fpt.edu.eresourcessystem.enums.CommonEnum;
import fpt.edu.eresourcessystem.enums.CourseEnum;
import fpt.edu.eresourcessystem.model.*;
import fpt.edu.eresourcessystem.service.*;
import fpt.edu.eresourcessystem.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/lecturer")
@PropertySource("web-setting.properties")
public class LecturerController {
    @Value("${page-size}")
    private Integer pageSize;
    private final CourseService courseService;
    private final AccountService accountService;
    private final LecturerService lecturerService;
    private final TopicService topicService;

    private final DocumentService documentService;
    private final CourseLogService courseLogService;

    @Autowired
    public LecturerController(CourseService courseService, AccountService accountService,
                              LecturerService lecturerService, TopicService topicService,
                              DocumentService documentService, CourseLogService courseLogService) {
        this.courseService = courseService;
        this.accountService = accountService;
        this.lecturerService = lecturerService;
        this.topicService = topicService;
        this.documentService = documentService;
        this.courseLogService = courseLogService;
    }

    public Lecturer getLoggedInLecturer() {
        return lecturerService.findAll().get(0);
    }

    @GetMapping
    public String getLibraryManageDashboard(@ModelAttribute Account account) {
        return "lecturer/lecturer";
    }

    /*
        LECTURER COURSES
     */

    @GetMapping({"/courses/{courseId}/update"})
    public String updateCourseProcess(@PathVariable(required = false) String courseId, final Model model) {
        if (null == courseId) {
            courseId = "";
        }
        Course course = courseService.findByCourseId(courseId);
        if (null == course) {
            return "redirect:lecturer/courses/update?error";
        } else {
            List<Account> lecturers = accountService.findAllLecturer();
            model.addAttribute("lecturers", lecturers);
            model.addAttribute("course", course);
            return "lecturer/course/lecturer_update-course";
        }
    }

    @PostMapping("/update")
    public String updateCourse(@ModelAttribute CourseDTO courseDTO, final Model model) { //thêm tham số course state
        // if status.equals("PUBLISH")
        Course course = new Course(courseDTO, CourseEnum.Status.PUBLISH);
        // else ...
        Course checkExist = courseService.findByCourseId(course.getId());
        if (null == checkExist) {
            model.addAttribute("errorMessage", "Course not exist.");
            return "exception/404";
        } else {
            Course checkCodeDuplicate = courseService.findByCourseCode(course.getCourseCode());
            if (checkCodeDuplicate != null &&
                    !checkExist.getCourseCode().equalsIgnoreCase(course.getCourseCode())) {
                //
            }
            courseService.updateCourse(course);
            List<Account> lecturers = accountService.findAllLecturer();
            model.addAttribute("course", course);
            model.addAttribute("lecturers", lecturers);
            model.addAttribute("success", "");
            return "lecturer/course/lecturer_update-course";
        }
    }

    @GetMapping({"courses/{courseId}"})
    public String showCourseDetail(@PathVariable String courseId, final Model model) {
        Lecturer lecturer = getLoggedInLecturer();
        Course course = courseService.findByCourseId(courseId);
        if(null==course || null == lecturer){
            return "exception/404";
        }

        // add course log
        CourseLog courseLog = new CourseLog(new CourseLogId(lecturer.getAccount().getId(), courseId, CommonEnum.Action.VIEW, LocalDateTime.now()));
        courseLogService.addCourseLog(courseLog);

        List<Topic> topics = topicService.findByCourseId(courseId);
        model.addAttribute("course", course);
        model.addAttribute("topics", topics);
        return "lecturer/course/lecturer_course-detail";
    }


    @GetMapping("/delete/{courseId}")
    public String delete(@PathVariable String courseId) {
        Course checkExist = courseService.findByCourseId(courseId);
        if (null != checkExist) {
            for (String topicId : checkExist.getTopics()) {
                topicService.delete(topicId);
            }
            courseService.delete(checkExist);
            return "redirect:/lecturer/courses/list?success";
        }
        return "redirect:/lecturer/courses/list?error";
    }

    @GetMapping({"/addTopic/{courseId}", "/topics/{courseId}"})
    public String addTopicProcess(@PathVariable String courseId, final Model model) {
        Course course = courseService.findByCourseId(courseId);
        List<Topic> topics = topicService.findByCourseId(courseId);
        Topic modelTopic = new Topic();
        modelTopic.setCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("topics", topics);
        model.addAttribute("topic", modelTopic);
        return "lecturer/course/lecturer_add-topic-to-course";
    }

    @PostMapping({"/addTopic"})
    public String addTopic(@ModelAttribute Topic topic, final Model model) {
        topic = topicService.addTopic(topic);
        courseService.addTopic(topic);
        Course course = courseService.findByCourseId(topic.getCourse().getId());
        List<Topic> topics = topicService.findByCourseId(topic.getCourse().getId());
        Topic modelTopic = new Topic();
        modelTopic.setCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("topics", topics);
        model.addAttribute("topic", modelTopic);
        return "lecturer/topic/lecturer_add-topic-to-course";
    }

    @GetMapping({"/updateTopic/{topicId}"})
    public String editTopicProcess(@PathVariable String topicId, final Model model) {
        Topic topic = topicService.findById(topicId);
        Course course = courseService.findByCourseId(topic.getCourse().getId());
        List<Topic> topics = topicService.findByCourseId(course.getId());
        model.addAttribute("course", course);
        model.addAttribute("topics", topics);
        model.addAttribute("topic", topic);
        return "lecturer/topic/lecturer_update-topic-of-course";
    }


    @PostMapping({"/updateTopic/{topicId}"})
    public String editTopic(@PathVariable String topicId, @ModelAttribute Topic topic) {
        Topic checkTopicExist = topicService.findById(topicId);
        if (null != checkTopicExist) {
            topicService.updateTopic(topic);
            return "redirect:/lecturer/courses/updateTopic/" + topicId;
        }
        return "lecturer/topic/lecturer_add-topic-to-course";

    }

    @GetMapping({"/deleteTopic/{courseId}/{topicId}"})
    public String deleteTopic(@PathVariable String courseId, @PathVariable String topicId, final Model model) {
        Topic topic = topicService.findById(topicId);
        if (null != topic) {
            courseService.removeTopic(topic);
            topicService.delete(topicId);
            Course course = courseService.findByCourseId(courseId);
            List<Topic> topics = topicService.findByCourseId(courseId);
            Topic modelTopic = new Topic();
            modelTopic.setCourse(course);
            model.addAttribute("course", course);
            model.addAttribute("topics", topics);
            model.addAttribute("topic", modelTopic);
        }
        return "lecturer/topic/lecturer_add-topic-to-course";
    }

//    @GetMapping("/lecturer/topic/detail/{topicId}")
//    public String viewTopicDetail(@PathVariable String topicId, final Model model) {
//        Topic topic = topicService.findById(topicId);
//        Course course = courseService.findByCourseId(topic.getCourse().getId());
//        List<Document> documents = new ArrayList<>();
//        for(ResourceType resourceType : topic.getResourceTypes()) {
//            documents = documentService.findDocumentsByResourceTypeId(resourceType.getId());
//        }
//        model.addAttribute("course", course);
//        model.addAttribute("topic", topic);
//        model.addAttribute("documents", documents);
//        return "lecturer/course/lecturer_topic-detail";
//    }

    @GetMapping({"/courses/list/{pageIndex}", "/courses/list"})
    public String viewCourseManaged(@PathVariable(required = false) Integer pageIndex, final Model model) {
        // get account authorized
        Lecturer lecturer = getLoggedInLecturer();
        if (null == lecturer){
            return "common/login";
        }
        Page<Course> page = lecturerService.findListManagingCourse(lecturer, pageIndex, pageSize);
        List<Integer> pages = CommonUtils.pagingFormat(page.getTotalPages(), pageIndex);
        model.addAttribute("pages", pages);
        model.addAttribute("totalPage", page.getTotalPages());
        model.addAttribute("courses", page.getContent());
        return "lecturer/course/lecturer_courses";
    }
}
