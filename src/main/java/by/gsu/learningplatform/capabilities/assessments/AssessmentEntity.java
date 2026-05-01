package by.gsu.learningplatform.capabilities.assessments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "assessments")
public class AssessmentEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "questions_json")
    private String questionsJson;

    @Column(name = "answer_key_json")
    private String answerKeyJson;

    @Column(name = "rubric_json")
    private String rubricJson;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public void setCourseId(final UUID courseId) {
        this.courseId = courseId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final UUID createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public void setQuestionsJson(final String questionsJson) {
        this.questionsJson = questionsJson;
    }

    public String getAnswerKeyJson() {
        return answerKeyJson;
    }

    public void setAnswerKeyJson(final String answerKeyJson) {
        this.answerKeyJson = answerKeyJson;
    }

    public String getRubricJson() {
        return rubricJson;
    }

    public void setRubricJson(final String rubricJson) {
        this.rubricJson = rubricJson;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(final String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(final UUID sourceId) {
        this.sourceId = sourceId;
    }
}
