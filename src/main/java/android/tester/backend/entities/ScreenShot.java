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
public class ScreenShot {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String fileName;

  @ManyToOne
  @JoinColumn(name = "test_device_id")
  private TestDevice testDevice;

  @ManyToOne
  @JoinColumn(name = "application_id")
  private Application application;

  private Boolean invalid;

  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Temporal(TemporalType.TIMESTAMP)
  private Date edited;

  private Integer editCount;

  @OneToMany(mappedBy = "screenShot")
  private Set<Defect> defects;
}
