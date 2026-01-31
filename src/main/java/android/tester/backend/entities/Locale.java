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
public class Locale {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String code;

  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Temporal(TemporalType.TIMESTAMP)
  private Date edited;

  private Integer editCount;

  @OneToMany(mappedBy = "locale")
  private Set<ApplicationLocale> applicationLocales;
}
