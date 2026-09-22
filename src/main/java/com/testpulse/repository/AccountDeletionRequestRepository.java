package com.testpulse.repository;

import com.testpulse.model.AccountDeletionRequest;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, Long> {

    boolean existsByEmailAndStatusIn(String email, Collection<AccountDeletionRequest.Status> statuses);

    List<AccountDeletionRequest> findAllByOrderByRequestedAtAsc();
}
