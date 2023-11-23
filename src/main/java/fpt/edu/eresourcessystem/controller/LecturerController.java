package fpt.edu.eresourcessystem.controller;

import fpt.edu.eresourcessystem.dto.DocumentDTO;
import fpt.edu.eresourcessystem.enums.CourseEnum;
import fpt.edu.eresourcessystem.enums.DocumentEnum;
import fpt.edu.eresourcessystem.model.*;
import fpt.edu.eresourcessystem.service.*;
import fpt.edu.eresourcessystem.utils.CommonUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ResourceTypeService resourceTypeService;

    private final DocumentService documentService;
    private final CourseLogService courseLogService;

    private final QuestionService questionService;
    private final AnswerService answerService;
    

    public LecturerController(CourseService courseService, AccountService accountService, LecturerService lecturerService, TopicService topicService, ResourceTypeService resourceTypeService, DocumentService documentService, CourseLogService courseLogService, QuestionService questionService, AnswerService answerService) {
        this.courseService = courseService;
        this.accountService = accountService;
        this.lecturerService = lecturerService;
        this.topicService = topicService;
        this.resourceTypeService = resourceTypeService;
        this.documentService = documentService;
        this.courseLogService = courseLogService;
        this.questionService = questionService;
        this.answerService = answerService;
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

    /**
     * @param pageIndex
     * @param model
     * @return
     */
    @GetMapping({"/courses/list/{status}/{pageIndex}"})
    public String viewCourseManaged(@PathVariable(required = false) Integer pageIndex, final Model model, @PathVariable String status) {
        // get account authorized
        Lecturer lecturer = getLoggedInLecturer();
        if (null == lecturer || "".equalsIgnoreCase(status)) {
            return "common/login";
        }
        Page<Course> page = lecturerService.findListManagingCourse(lecturer, status, pageIndex, pageSize);
        List<Integer> pages = CommonUtils.pagingFormat(page.getTotalPages(), pageIndex);
        model.addAttribute("pages", pages);
        model.addAttribute("totalPage", page.getTotalPages());
        model.addAttribute("courses", page.getContent());
        model.addAttribute("status", status);

        return "lecturer/course/lecturer_courses";
    }

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

    @PostMapping("/courses/{courseID}/changeStatus")
    public String changeCourseStatus(@PathVariable String courseID, @RequestParam String status) {
        Course course = courseService.findByCourseId(courseID);
        switch (status.toUpperCase()){
            case "PUBLISH":
                course.setStatus(CourseEnum.Status.PUBLISH);
                break;
            case "DRAFT":
                course.setStatus(CourseEnum.Status.DRAFT);
                break;
            case "HIDE":
                course.setStatus(CourseEnum.Status.HIDE);
                break;
        }
        courseService.updateCourse(course);
        return "redirect:/lecturer/courses/" + courseID;
    }

    @GetMapping({"/courses/{courseId}/{getBy}", "/courses/{courseId}"})
    public String showCourseDetail(@PathVariable String courseId, final Model model, @PathVariable(required = false) String getBy) {
        Lecturer lecturer = getLoggedInLecturer();
        Course course = courseService.findByCourseId(courseId);
        if (null == course || null == lecturer) {
            return "exception/404";
        }

        // add course log
//        CourseLog courseLog = new CourseLog(course, CommonEnum.Action.VIEW);
//        courseLogService.addCourseLog(courseLog);
        model.addAttribute("course", course);
        model.addAttribute("courseStatus", CourseEnum.ChangeableStatus.values());
        if(getBy == null || getBy.equalsIgnoreCase("topics")) {
            List<Topic> topics = course.getTopics();
            model.addAttribute("topics", topics);
        } else {
            List<ResourceType> resourceTypes = course.getResourceTypes();
            model.addAttribute("resourceTypes", resourceTypes);
        }
        model.addAttribute("getBy", getBy);

        return "lecturer/course/lecturer_course-detail";
    }

    @GetMapping("/{courseId}/delete")
    public String delete(@PathVariable String courseId) {
        Course checkExist = courseService.findByCourseId(courseId);
        if (null != checkExist) {
            for (Topic topic : checkExist.getTopics()) {
                topicService.delete(topic.getId());
            }
            courseService.delete(checkExist);
            return "redirect:/lecturer/courses/list?success";
        }
        return "redirect:/lecturer/courses/list?error";
    }

    @GetMapping({"/courses/{courseId}/add_topic"})
    public String addTopicProcess(@PathVariable String courseId, final Model model) {
        Course course = courseService.findByCourseId(courseId);
        List<Topic> topics = course.getTopics();
        Topic modelTopic = new Topic();
        modelTopic.setCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("topics", topics);
        model.addAttribute("topic", modelTopic);
        return "lecturer/topic/lecturer_add-topic-to-course";
    }

    @PostMapping({"topics/add_topic"})
    public String addTopic(@ModelAttribute Topic topic, final Model model) {
        topic = topicService.addTopic(topic);
        courseService.addTopic(topic);
        Course course = courseService.findByCourseId(topic.getCourse().getId());
        List<Topic> topics = course.getTopics();
        Topic modelTopic = new Topic();
        modelTopic.setCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("topics", topics);
        model.addAttribute("topic", modelTopic);
        model.addAttribute("success", "success");
        return "lecturer/topic/lecturer_add-topic-to-course";
    }

    @GetMapping({"/topics/{topicId}/update"})
    public String editTopicProcess(@PathVariable String topicId, final Model model) {
        Topic topic = topicService.findById(topicId);
        Course course = courseService.findByCourseId(topic.getCourse().getId());
        List<Topic> topics = course.getTopics();
        model.addAttribute("course", course);
        model.addAttribute("topics", topics);
        model.addAttribute("topic", topic);
        model.addAttribute("topicId", topicId);
        return "lecturer/topic/lecturer_update-topic-of-course";
    }


    @PostMapping({"/topics/{topicId}/update"})
    public String editTopic(@PathVariable String topicId, @ModelAttribute Topic topic) {
        Topic checkTopicExist = topicService.findById(topicId);
        if (null != checkTopicExist) {
            checkTopicExist.setTopicTitle(topic.getTopicTitle());
            checkTopicExist.setTopicDescription(topic.getTopicDescription());
            topicService.updateTopic(checkTopicExist);
            return "redirect:/lecturer/topics/" + topicId + "/update?success";
        }
        return "redirect:/lecturer/topics/" + topicId + "/update?error";

    }

    @GetMapping({"/topics/{topicId}/delete_topic/"})
    public String deleteTopic(@PathVariable String courseId, @PathVariable String topicId, final Model model) {
        Topic topic = topicService.findById(topicId);
        if (null != topic) {
            courseService.removeTopic(topic);
            topicService.softDelete(topic);
        }
        return "redirect:/lecturer/" + topic.getCourse().getId();
    }

    @GetMapping("/topics/{topicId}")
    public String viewTopicDetail(@PathVariable String topicId, final Model model) {
        Topic topic = topicService.findById(topicId);
        model.addAttribute("course", topic.getCourse());
        model.addAttribute("topic", topic);
        return "lecturer/topic/lecturer_topic-detail";
    }

    @GetMapping({"/courses/{courseId}/add_resource_type"})
    public String addResourceTypeProcess(@PathVariable String courseId, final Model model) {
        Course course = courseService.findByCourseId(courseId);
        List<ResourceType> resourceTypes = course.getResourceTypes();
        ResourceType modelResourceType = new ResourceType();
        modelResourceType.setCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("resourceTypes", resourceTypes);
        model.addAttribute("resourceType", modelResourceType);
        return "lecturer/resource_type/lecturer_add-resource-type-to-course";
    }

    @PostMapping({"resource_types/add_resource_type"})
    public String addResourceType(ResourceType resourceType, final Model model) {
        ResourceType resourcetype = resourceTypeService.addResourceType(resourceType);
        courseService.addResourceType(resourcetype);
        Course course = courseService.findByCourseId(resourcetype.getCourse().getId());
        List<ResourceType> resourceTypes = course.getResourceTypes();
        ResourceType modelResourceType = new ResourceType();
        modelResourceType.setCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("resourceTypes", resourceTypes);
        model.addAttribute("resourceType", modelResourceType);
        model.addAttribute("success", "success");
        return "lecturer/resource_type/lecturer_add-resource-type-to-course";
    }

    @GetMapping({"/resource_types/{resourceTypeId}/update"})
    public String editResourceTypeProcess(@PathVariable String resourceTypeId, final Model model) {
        ResourceType resourcetype = resourceTypeService.findById(resourceTypeId);
        Course course = resourcetype.getCourse();
        List<ResourceType> resourceTypes = course.getResourceTypes();
        model.addAttribute("course", course);
        model.addAttribute("resourceTypes", resourceTypes);
        model.addAttribute("resourceType", resourcetype);
        return "lecturer/resource_type/lecturer_update-resource-type-of-course";
    }


    @PostMapping({"/resource_types/{resourceTypeId}/update"})
    public String editResourceType(@PathVariable String resourceTypeId, @ModelAttribute ResourceType resourcetype) {
        ResourceType checkResourceTypeExist = resourceTypeService.findById(resourceTypeId);
        if (null != checkResourceTypeExist) {
            checkResourceTypeExist.setResourceTypeName(resourcetype.getResourceTypeName());
            resourceTypeService.updateResourceType(checkResourceTypeExist);
            return "redirect:/lecturer/resource_types/" + resourceTypeId + "/update?success";
        }
        return "redirect:/lecturer/resource_types/" + resourceTypeId + "/update?error";

    }
//
//    @GetMapping({"resourcetypes/{resourcetypeId}/delete_resourcetype/"})
//    public String deleteResourceType(@PathVariable String courseId, @PathVariable String resourcetypeId, final Model model) {
//        ResourceType resourcetype = resourceTypeService.findById(resourcetypeId);
//        if (null != resourcetype) {
//            courseService.removeResourceType(resourcetype);
//            resourceTypeService.softDelete(resourcetype);
//        }
//        return "redirect:/lecturer/" + resourcetype.getCourse().getId();
//    }
//
//    @GetMapping({"resourcetypes/{resourcetypeId}/delete_resourcetype/"})
//    public String deleteResourceType(@PathVariable String courseId, @PathVariable String resourcetypeId, final Model model) {
//        ResourceType resourcetype = resourceTypeService.findById(resourcetypeId);
//        if (null != resourcetype) {
//            courseService.removeResourceType(resourcetype);
//            resourceTypeService.softDelete(resourcetype);
//        }
//        return "redirect:/lecturer/" + resourcetype.getCourse().getId();
//    }

    @GetMapping("/resource_types/{resourceTypeId}")
    public String viewResourceTypeDetail(@PathVariable String resourceTypeId, final Model model) {
        ResourceType resourcetype = resourceTypeService.findById(resourceTypeId);
        model.addAttribute("course", resourcetype.getCourse());
        model.addAttribute("resourceType", resourcetype);
        return "lecturer/resource_type/lecturer_resource-type-detail";
    }

    /*
        DOCUMENTS
     */

    /**
     * List all documents
     *
     * @return list documents
     */
    @GetMapping({"/documents/list"})
    public String showDocuments() {
        return "librarian/document/librarian_documents";
    }

    @GetMapping("/documents/list/{pageIndex}")
    String showDocumentsByPage(@PathVariable Integer pageIndex,
                               @RequestParam(required = false, defaultValue = "") String search,
                               @RequestParam(required = false, defaultValue = "all") String course,
                               @RequestParam(required = false, defaultValue = "all") String topic,
                               final Model model, HttpServletRequest request) {
        Page<Document> page = documentService.filterAndSearchDocument(course, topic, search, search, pageIndex, pageSize);

        List<Integer> pages = CommonUtils.pagingFormat(page.getTotalPages(), pageIndex);
        model.addAttribute("pages", pages);
        model.addAttribute("totalPage", page.getTotalPages());
        model.addAttribute("currentPage", pageIndex);
        model.addAttribute("documents", page.getContent());
        model.addAttribute("search", search);
        model.addAttribute("selectedCourse", course);
        model.addAttribute("courses", courseService.findAll());
        return "lecturer/document/lecturer_documents";
    }

    @GetMapping({"/documents/{documentId}"})
    public String viewDocument(@PathVariable(required = false) String documentId, final Model model) {
        Document document = documentService.findById(documentId);
        if (null == document) {
            model.addAttribute("errorMessage", "Could not found document.");
            return "exception/404";
        } else {
            // get list question
            List<Question> questions =  questionService.findByDocId(document);

            if(!document.getDocType().toString().equalsIgnoreCase("UNKNOWN")){
                String base64EncodedData = Base64.getEncoder().encodeToString(document.getContent());
                model.addAttribute("data", base64EncodedData);
            }
            model.addAttribute("newAnswer", new Answer());
            model.addAttribute("document", document);
            model.addAttribute("topic", document.getTopic());
            model.addAttribute("questions", questions);
            return "lecturer/document/lecturer_document-detail";
        }
    }


    @GetMapping({"topics/{topicId}/documents/add"})
    public String addDocumentInTopic(@PathVariable String topicId, final Model model) {
        Topic topic = topicService.findById(topicId);
        model.addAttribute("document", new Document());
        model.addAttribute("topic", topic);
//        List<String> defaultRt = Arrays.stream(DocumentEnum.DefaultTopicResourceTypes.values())
//                .map(DocumentEnum.DefaultTopicResourceTypes::getDisplayValue)
//                .collect(Collectors.toList());
//
//        List<ResourceType> resourceTypesByCourse = topic.getCourse().getResourceTypes();
//        List<String> resourceTypes = new ArrayList<>();
//
//        if (resourceTypesByCourse != null) {
//            resourceTypes = resourceTypesByCourse.stream()
//                    .map(ResourceType::getResourceTypeName)
//                    .collect(Collectors.toList());
//        } else {
//            resourceTypes = new ArrayList<>();
//        }
//
//        resourceTypes.addAll(defaultRt);
        model.addAttribute("resourceTypes", DocumentEnum.DefaultTopicResourceTypes.values());
//        System.out.println(DocumentEnum.DefaultTopicResourceTypes.values());
        return "lecturer/document/lecturer_add-document";
    }

    @GetMapping({"resource_types/{resourceTypeId}/documents/add"})
    public String addDocumentInResourceType(@PathVariable String resourceTypeId, final Model model) {
        ResourceType resourceType = resourceTypeService.findById(resourceTypeId);
        model.addAttribute("document", new Document());
        model.addAttribute("resourceType", resourceType);
        List<Topic> topics = resourceType.getCourse().getTopics();
        model.addAttribute("topics", topics);
//        System.out.println(DocumentEnum.DefaultTopicResourceTypes.values());
        return "lecturer/document/lecturer_add-document-to-resource-type";
    }

    @PostMapping("/documents/add")
    @Transactional
    public String addDocumentProcess(@ModelAttribute DocumentDTO documentDTO,
                                     @RequestParam(value = "topicId") String topicId,
                                     @RequestParam(value = "respondResourceType") String respondResourceType,
                                     @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        // set topic vào document
        Topic topic = topicService.findById(topicId);
        documentDTO.setTopic(topic);
        ResourceType resourceType = new ResourceType(respondResourceType, topic.getCourse());

        // Thêm resource type
        List<ResourceType> resourceTypesInCourse = topic.getCourse().getResourceTypes();
        boolean checkResourceTypeExist = true;
        ResourceType existedResourceType = null;
        for(ResourceType resourceType1 : resourceTypesInCourse) {
            if(resourceType1.getResourceTypeName().equalsIgnoreCase(respondResourceType)) {
                checkResourceTypeExist = false;
                existedResourceType = resourceType1;
                break;
            }
        }
        if(checkResourceTypeExist == true) {
            ResourceType addedResourceType = resourceTypeService.addResourceType(resourceType);
            documentDTO.setResourceType(addedResourceType);
        } else {
            documentDTO.setResourceType(existedResourceType);
        }
        // set resource type vào document
        

        // Xử lý file
        // thêm check file trước khi add
        String id = "fileNotFound";
        if (file != null && !file.isEmpty()) {
            id = documentService.addFile(file);
        }
        Document document = documentService.addDocument(documentDTO, id);
        // Xử lý sau khi add document

        resourceTypeService.addDocumentToResourceType(document.getResourceType().getId(), new ObjectId(document.getId()));

        // Thêm document vào topic
        topicService.addDocumentToTopic(topicId, new ObjectId(document.getId()));

        return "redirect:/lecturer/topics/" + topicId + "/documents/add?success";
    }

    @GetMapping({"/documents/{documentId}/update"})
    public String updateDocumentProcess(@PathVariable(required = false) String documentId, final Model model) {
        if (null == documentId) {
            documentId = "";
        }
        Document document = documentService.findById(documentId);
        if (null == document) {
            return "redirect:lecturer/documents/update?error";
        } else {
            model.addAttribute("document", document);
            model.addAttribute("resourceTypes", DocumentEnum.DefaultTopicResourceTypes.values());
            return "lecturer/document/lecturer_update-document";
        }
    }

    @PostMapping("/documents/update")
    public String updateCourse(@ModelAttribute DocumentDTO document, final Model model) throws IOException {
        Document checkExist = documentService.findById(document.getId());

        if (null == checkExist) {
            return "redirect:/lecturer/documents/" + document.getId() + "/update?error";
        } else {
            checkExist.setTitle(document.getTitle());
            checkExist.setDescription(document.getDescription());
            checkExist.setResourceType(document.getResourceType());
            checkExist.setContent(document.getContent());
            documentService.updateDocument(checkExist);
            return "redirect:/lecturer/documents/" + document.getId() + "/update?success";
        }
    }

    @GetMapping("/documents/{documentId}/delete")
    public String deleteDocument(@PathVariable String documentId) {
        Document document = documentService.findById(documentId);
        if (null != document) {
            topicService.removeDocuments(document);
            documentService.softDelete(document);
            return "redirect:/librarian/courses/list/1?success";
        }
        return "redirect:/librarian/courses/list/1?error";
    }

    /*
        My document
    */
    @GetMapping({"/my_documents/list/{status}/{pageIndex}"})
    public String viewUploadedDocuments(@PathVariable(required = false) Integer pageIndex, final Model model, @PathVariable String status) {
        // get account authorized
        Lecturer lecturer = getLoggedInLecturer();
        if (null == lecturer || "".equalsIgnoreCase(status)) {
            return "common/login";
        }
        Page<Document> page = lecturerService.findListDocuments(lecturer, status, pageIndex, pageSize);
        List<Integer> pages = CommonUtils.pagingFormat(page.getTotalPages(), pageIndex);
        model.addAttribute("pages", pages);
        model.addAttribute("totalPage", page.getTotalPages());
        model.addAttribute("documents", page.getContent());
        model.addAttribute("status", status);

        return "lecturer/document/lecturer_my-documents";
    }

    /*
        Question ans
    */
    @GetMapping({"/questions/list/{status}/{pageIndex}"})
    public String viewListOfQuestions(@PathVariable(required = false) Integer pageIndex, final Model model, @PathVariable String status) {
        // get account authorized
        Lecturer lecturer = getLoggedInLecturer();
        List<Question> questions = questionService.findByLecturer(lecturer);
        for (Question q: questions) {
            q.setAnswers(new HashSet<>(answerService.findByQuestion(q)));
        }
        model.addAttribute("studentQuestions", questions);
        // add log
//        addUserLog("/my_library/my_questions/history");
        model.addAttribute("status", status);

        return "lecturer/document/lecturer_questions";
    }

    /*
        Feedback
    */
    @GetMapping({"/feedback"})
    public String feedback(@PathVariable(required = false) Integer pageIndex, final Model model, @PathVariable String status) {
        return "";
    }

}
