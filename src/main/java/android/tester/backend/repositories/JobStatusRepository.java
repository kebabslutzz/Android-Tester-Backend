package android.tester.backend.repositories;

import android.tester.backend.entities.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobStatusRepository extends JpaRepository<JobStatus, UUID> {
}
