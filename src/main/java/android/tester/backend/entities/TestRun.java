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
public class TestRun {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @Temporal(TemporalType.TIMESTAMP)
  private Date runDate;

  private String description;
  private Boolean finished;

  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Temporal(TemporalType.TIMESTAMP)
  private Date edited;

  private Integer editCount;

  @OneToOne(mappedBy = "testRun")
  private Job job;

  @OneToOne(mappedBy = "testRun")
  private TestDevice testDevice;

  @OneToMany(mappedBy = "testRun")
  private Set<TestRunDefect> testRunDefects;
}
