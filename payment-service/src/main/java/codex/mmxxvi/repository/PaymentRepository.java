package codex.mmxxvi.repository;

import codex.mmxxvi.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByTransactionId(UUID transactionId);
    Optional<Payment> findByTransactionIdAndUserId(UUID transactionId, UUID userId);
    Page<Payment> findByUserId(UUID userId, Pageable pageable);
}
