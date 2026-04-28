package by.gsu.learningplatform.capabilities.courses;

import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final TagRepository tagRepository;
    private final AuthFacade authFacade;
    private final UserService userService;

    public CourseService(CourseRepository courseRepository,
                         CourseMapper courseMapper,
                         TagRepository tagRepository,
                         AuthFacade authFacade,
                         UserService userService) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.tagRepository = tagRepository;
        this.authFacade = authFacade;
        this.userService = userService;
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.STUDENT) {
            throw new ForbiddenException("Students cannot create courses");
        }

        final var entity = new CourseEntity();
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setTeacherId(actor.getId());
        entity.setTags(resolveTags(request.tags()));

        return courseMapper.toResponse(courseRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> findAll(Pageable pageable, Collection<String> requestedTags) {
        final var tags = normalizeTags(requestedTags);
        final var page = tags.isEmpty()
                ? courseRepository.findAll(pageable)
                : courseRepository.findDistinctByTagsNameIn(tags, pageable);
        return page.map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> findOwn(Pageable pageable) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.STUDENT) {
            throw new ForbiddenException("Students do not create courses");
        }
        return courseRepository.findByTeacherId(actor.getId(), pageable).map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseEntity getEntity(UUID courseId) {
        return courseRepository.findById(courseId).orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
    }

    @Transactional
    public CourseResponse update(UUID courseId, CourseRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = getEntity(courseId);
        if (actor.getRole() != UserRole.ADMIN && !actor.getId().equals(course.getTeacherId())) {
            throw new ForbiddenException("You can update only your own course");
        }

        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setTags(resolveTags(request.tags()));
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public void delete(UUID courseId) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = getEntity(courseId);

        if (actor.getRole() != UserRole.ADMIN && !actor.getId().equals(course.getTeacherId())) {
            throw new ForbiddenException("You can delete only your own course");
        }

        courseRepository.delete(course);
    }

    private Set<TagEntity> resolveTags(Collection<String> requestedTags) {
        final var normalizedTags = normalizeTags(requestedTags);
        if (normalizedTags.isEmpty()) {
            return new LinkedHashSet<>();
        }

        final var existingTags = tagRepository.findByNameIn(normalizedTags).stream()
                .collect(Collectors.toMap(TagEntity::getName, Function.identity()));
        final var resolvedTags = new LinkedHashSet<TagEntity>();
        for (var tagName : normalizedTags) {
            resolvedTags.add(existingTags.computeIfAbsent(tagName, this::createTag));
        }
        return resolvedTags;
    }

    private TagEntity createTag(String name) {
        final var tag = new TagEntity();
        tag.setName(name);
        return tagRepository.save(tag);
    }

    private List<String> normalizeTags(Collection<String> requestedTags) {
        if (requestedTags == null) {
            return List.of();
        }
        return requestedTags.stream()
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
