package android.tester.backend.repositories;

import android.tester.backend.entities.TestRunDefect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestRunDefectRepository extends JpaRepository<TestRunDefect, UUID> {
}
