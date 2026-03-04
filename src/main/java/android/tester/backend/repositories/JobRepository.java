package android.tester.backend.repositories;

import android.tester.backend.entities.Job;
import android.tester.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
  @Query("SELECT j FROM Job j WHERE j.testRun.user = :user")
  List<Job> findByUser(@Param("user") User user);
}
