package android.tester.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Application {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String name;
  private String packageName;
  private String version;
  private String apkFile;
  private String apkPath; // New field for the full APK file path

  @OneToMany(mappedBy = "application")
  private Set<ApplicationLocale> applicationLocales;

  @OneToMany(mappedBy = "application")
  private Set<Job> jobs;

  @OneToMany(mappedBy = "application")
  private Set<ScreenShot> screenShots;

  @Version
  private Integer versionNumber;

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
