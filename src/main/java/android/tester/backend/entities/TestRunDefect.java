package android.tester.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TestRunDefect {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "defect_type_id")
  private DefectType defectType;

  @ManyToOne
  @JoinColumn(name = "screenshot_id")
  private ScreenShot screenShot;

  @ManyToOne
  @JoinColumn(name = "test_run_id")
  private TestRun testRun;

  @ManyToOne
  @JoinColumn(name = "application_id")
  private Application application;

  private Long defectsCount;
  private String message;

  @OneToMany(mappedBy = "testRunDefect")
  private Set<TestRunDefectImage> images;

  private OffsetDateTime created;

  @LastModifiedDate
  private OffsetDateTime edited;

  private Integer editCount;

  @PrePersist
  public void prePersist() {
    this.setCreated(OffsetDateTime.now());
    this.setEditCount(0);
  }

  @PreUpdate
  public void preUpdate() {
    this.setEditCount(this.getEditCount() + 1);
  }
}
