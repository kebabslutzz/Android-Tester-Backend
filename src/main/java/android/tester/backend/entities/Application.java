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
public class Application {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String name;
  private String packageName;
  private String version;
  private String apkFile;

  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Temporal(TemporalType.TIMESTAMP)
  private Date edited;

  private Integer editCount;

  @OneToMany(mappedBy = "application")
  private Set<ApplicationLocale> applicationLocales;

  @OneToMany(mappedBy = "application")
  private Set<Job> jobs;

  @OneToMany(mappedBy = "application")
  private Set<ScreenShot> screenShots;
}
