package android.tester.backend.repositories;

import android.tester.backend.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByToken(String token);

  @Query("select t from RefreshToken t inner join User u on t.user.id = u.id where u.id = :userId and (t.revoked = false)")
  List<RefreshToken> findAllValidRefreshTokenByUser(UUID userId);
}
