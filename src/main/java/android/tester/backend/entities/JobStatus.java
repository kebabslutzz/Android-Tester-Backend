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
public class JobStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String name;

  @OneToMany(mappedBy = "status")
  private Set<Job> jobs;

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
