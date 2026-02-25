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
public class TestDevice {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @OneToOne
  @JoinColumn(name = "test_run_id")
  private TestRun testRun;

  private String name;
  private Boolean tablet;
  private Long screenSize;
  private Boolean englishInterface;
  private Boolean leftToRight;

  private String size;
  private String resolution;

  @OneToMany(mappedBy = "testDevice")
  private Set<ScreenShot> screenShots;

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
