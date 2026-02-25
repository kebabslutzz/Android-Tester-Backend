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
public class TestRun {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  private OffsetDateTime runDate;

  private String description;
  private Boolean finished;

  @OneToOne(mappedBy = "testRun")
  private Job job;

  @OneToOne(mappedBy = "testRun")
  private TestDevice testDevice;

  @OneToMany(mappedBy = "testRun")
  private Set<TestRunDefect> testRunDefects;

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
