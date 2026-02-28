package android.tester.backend.entities;

import android.tester.backend.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table(name = "app_user") // 'user' is often a reserved keyword in SQL
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String username;
  private String password;
  //  private String salt;
  private String email;

  @Enumerated(EnumType.STRING)
  private Role role;

  //  one user to many test runs
  @OneToMany(mappedBy = "user")
  private Set<TestRun> testRuns;

  @Version
  private Integer version;

  private OffsetDateTime created;

  private OffsetDateTime edited;

  private Integer editCount;

  @PrePersist
  public void prePersist() {
    if (this.created == null) {
      this.created = OffsetDateTime.now();
    }
    this.editCount = 0;
  }

  @PreUpdate
  public void preUpdate() {
    this.edited = OffsetDateTime.now();
    // Null-safe increment
    this.editCount = (this.editCount == null ? 0 : this.editCount) + 1;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(role.name()));
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
