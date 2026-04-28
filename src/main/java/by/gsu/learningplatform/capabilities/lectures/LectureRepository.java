package by.gsu.learningplatform.capabilities.lectures;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LectureRepository extends JpaRepository<LectureEntity, UUID> {

    List<LectureEntity> findByLessonId(UUID lessonId);

    @Query("""
            select l.id
            from LectureEntity l
            join LessonEntity lesson on lesson.id = l.lessonId
            where lesson.courseId = :courseId
            order by l.id
            """)
    List<UUID> findIdsByCourseId(UUID courseId);

    @Query("""
            select count(l)
            from LectureEntity l
            join LessonEntity lesson on lesson.id = l.lessonId
            where lesson.courseId = :courseId
            """)
    long countByCourseId(UUID courseId);

    @Query("""
            select case when count(l) > 0 then true else false end
            from LectureEntity l
            join LessonEntity lesson on lesson.id = l.lessonId
            where l.id = :lectureId and lesson.courseId = :courseId
            """)
    boolean existsByIdAndCourseId(UUID lectureId, UUID courseId);
}
