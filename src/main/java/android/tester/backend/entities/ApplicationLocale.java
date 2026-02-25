package android.tester.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ApplicationLocale {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "application_id")
  private Application application;

  @ManyToOne
  @JoinColumn(name = "locale_id")
  private Locale locale;

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
