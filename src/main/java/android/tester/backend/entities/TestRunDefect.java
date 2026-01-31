package android.tester.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Temporal(TemporalType.TIMESTAMP)
  private Date edited;

  private Integer editCount;

  @OneToMany(mappedBy = "testRunDefect")
  private Set<TestRunDefectImage> images;
}
