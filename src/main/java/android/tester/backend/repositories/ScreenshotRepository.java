package android.tester.backend.repositories;

import android.tester.backend.entities.ScreenShot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScreenshotRepository extends JpaRepository<ScreenShot, UUID> {
}
