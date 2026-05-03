package by.gsu.learningplatform.capabilities.courses;

import lombok.val;
import by.gsu.learningplatform.capabilities.lectures.LectureRepository;
import by.gsu.learningplatform.capabilities.lessons.LessonRepository;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.observability.BusinessMetrics;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final TagRepository tagRepository;
    private final LessonRepository lessonRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuthFacade authFacade;
    private final UserService userService;
    private final BusinessMetrics businessMetrics;

    public CourseService(CourseRepository courseRepository,
                         CourseMapper courseMapper,
                         TagRepository tagRepository,
                         LessonRepository lessonRepository,
                         LectureRepository lectureRepository,
                         EnrollmentRepository enrollmentRepository,
                         AuthFacade authFacade,
                         UserService userService,
                         BusinessMetrics businessMetrics) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.tagRepository = tagRepository;
        this.lessonRepository = lessonRepository;
        this.lectureRepository = lectureRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.authFacade = authFacade;
        this.userService = userService;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public CourseResponse create(final CourseRequest request) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.STUDENT) {
            throw new ForbiddenException("Students cannot create courses");
        }

        val entity = new CourseEntity();
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setTeacherId(actor.getId());
        entity.setTags(resolveTags(request.tags()));

        val saved = courseRepository.saveAndFlush(entity);
        businessMetrics.incrementCourses();
        return courseMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> findAll(final Pageable pageable, final Collection<String> requestedTags) {
        val tags = normalizeTags(requestedTags);
        val page = tags.isEmpty()
                ? courseRepository.findAll(pageable)
                : courseRepository.findByAllTagNames(tags, tags.size(), pageable);
        return page.map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseCatalogDetailsResponse getCatalogDetails(final UUID courseId) {
        val course = getEntity(courseId);
        val lessons = lessonRepository.findByCourseId(courseId).stream()
                .map(lesson -> new CourseCatalogLessonResponse(
                        lesson.getId(),
                        lesson.getCourseId(),
                        lesson.getTitle(),
                        lectureRepository.findByLessonId(lesson.getId()).stream()
                                .map(lecture -> new CourseCatalogLectureResponse(
                                        lecture.getId(),
                                        lecture.getLessonId(),
                                        lecture.getTitle()))
                                .toList()))
                .toList();

        return new CourseCatalogDetailsResponse(courseMapper.toResponse(course), lessons);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> findOwn(final Pageable pageable) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.STUDENT) {
            return Page.empty(pageable);
        }
        return courseRepository.findByTeacherId(actor.getId(), pageable).map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> findEnrolled(final Pageable pageable) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.TEACHER) {
            return Page.empty(pageable);
        }
        return courseRepository.findEnrolledByUserId(actor.getId(), pageable).map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CourseEnrollmentResponse> findEnrolledWithEnrollment(final Pageable pageable) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.TEACHER) {
            return Page.empty(pageable);
        }
        val enrollments = enrollmentRepository.findByUserId(actor.getId(), pageable);
        val courseIds = enrollments.stream().map(enrollment -> enrollment.getCourseId()).toList();
        val courses = courseIds.isEmpty()
                ? Map.<UUID, CourseEntity>of()
                : courseRepository.findByIdIn(courseIds).stream().collect(Collectors.toMap(CourseEntity::getId, Function.identity()));
        val result = enrollments.stream()
                .map(enrollment -> {
                    val course = courses.get(enrollment.getCourseId());
                    if (course == null) {
                        return null;
                    }
                    return new CourseEnrollmentResponse(
                            enrollment.getId(),
                            enrollment.getEnrolledAt(),
                            courseMapper.toResponse(course));
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        return new PageImpl<>(result, pageable, enrollments.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CourseEntity getEntity(final UUID courseId) {
        return courseRepository.findById(courseId).orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
    }

    @Transactional
    public CourseResponse update(final UUID courseId, final CourseRequest request) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val course = getEntity(courseId);
        if (actor.getRole() != UserRole.ADMIN && !actor.getId().equals(course.getTeacherId())) {
            throw new ForbiddenException("You can update only your own course");
        }

        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setTags(resolveTags(request.tags()));
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public void delete(final UUID courseId) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val course = getEntity(courseId);

        if (actor.getRole() != UserRole.ADMIN && !actor.getId().equals(course.getTeacherId())) {
            throw new ForbiddenException("You can delete only your own course");
        }

        courseRepository.delete(course);
    }

    private Set<TagEntity> resolveTags(final Collection<String> requestedTags) {
        val normalizedTags = normalizeTags(requestedTags);
        if (normalizedTags.isEmpty()) {
            return new LinkedHashSet<>();
        }

        val existingTags = tagRepository.findByNameIn(normalizedTags).stream()
                .collect(Collectors.toMap(TagEntity::getName, Function.identity()));
        val resolvedTags = new LinkedHashSet<TagEntity>();
        for (val tagName : normalizedTags) {
            resolvedTags.add(existingTags.computeIfAbsent(tagName, this::createTag));
        }
        return resolvedTags;
    }

    private TagEntity createTag(final String name) {
        val tag = new TagEntity();
        tag.setName(name);
        return tagRepository.save(tag);
    }

    private List<String> normalizeTags(final Collection<String> requestedTags) {
        if (requestedTags == null) {
            return List.of();
        }
        return requestedTags.stream()
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
