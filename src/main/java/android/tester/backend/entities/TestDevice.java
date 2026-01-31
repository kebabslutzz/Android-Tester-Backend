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

  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Temporal(TemporalType.TIMESTAMP)
  private Date edited;

  private Integer editCount;

  @OneToMany(mappedBy = "testDevice")
  private Set<ScreenShot> screenShots;
}
