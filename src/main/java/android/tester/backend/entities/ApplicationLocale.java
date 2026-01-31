package android.tester.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Temporal(TemporalType.TIMESTAMP)
  private Date edited;

  private Integer editCount;
}
