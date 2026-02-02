package android.tester.backend.repositories;

import android.tester.backend.entities.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocaleRepository extends JpaRepository<Locale, UUID> {
}
