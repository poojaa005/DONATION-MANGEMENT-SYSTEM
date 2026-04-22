package com.ngo.donation_management.repository;

// repository/PaymentRepository.java

import com.ngo.donation_management.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository
        extends JpaRepository<Payment, Integer> {
    List<Payment> findByDonation_DonationId(Integer donationId);
    boolean existsByDonation_DonationIdAndPaymentStatus(
            Integer donationId, Payment.PaymentStatus status);
    List<Payment> findByPaymentStatus(Payment.PaymentStatus status);
    List<Payment> findByDonation_Campaign_Ngo_NgoId(Integer ngoId);
    @Query("SELECT DISTINCT p FROM Payment p " +
            "LEFT JOIN p.donation d " +
            "LEFT JOIN d.campaign c " +
            "LEFT JOIN c.ngo cNgo " +
            "LEFT JOIN d.ngo dNgo " +
            "WHERE cNgo.ngoId = :ngoId OR dNgo.ngoId = :ngoId")
    List<Payment> findByScopedNgoId(@Param("ngoId") Integer ngoId);
    List<Payment> findByPaymentStatusAndDonation_Campaign_Ngo_NgoId(
            Payment.PaymentStatus status, Integer ngoId);
    @Query("SELECT DISTINCT p FROM Payment p " +
            "LEFT JOIN p.donation d " +
            "LEFT JOIN d.campaign c " +
            "LEFT JOIN c.ngo cNgo " +
            "LEFT JOIN d.ngo dNgo " +
            "WHERE p.paymentStatus = :status " +
            "AND (cNgo.ngoId = :ngoId OR dNgo.ngoId = :ngoId)")
    List<Payment> findByPaymentStatusAndScopedNgoId(
            @Param("status") Payment.PaymentStatus status,
            @Param("ngoId") Integer ngoId);
    List<Payment> findByPaymentMethod(Payment.PaymentMethod method);
    List<Payment> findByPaymentMethodAndDonation_Campaign_Ngo_NgoId(
            Payment.PaymentMethod method, Integer ngoId);
    @Query("SELECT DISTINCT p FROM Payment p " +
            "LEFT JOIN p.donation d " +
            "LEFT JOIN d.campaign c " +
            "LEFT JOIN c.ngo cNgo " +
            "LEFT JOIN d.ngo dNgo " +
            "WHERE p.paymentMethod = :method " +
            "AND (cNgo.ngoId = :ngoId OR dNgo.ngoId = :ngoId)")
    List<Payment> findByPaymentMethodAndScopedNgoId(
            @Param("method") Payment.PaymentMethod method,
            @Param("ngoId") Integer ngoId);
    Optional<Payment> findByTransactionId(String transactionId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.paymentStatus = 'success'")
    BigDecimal getTotalSuccessfulPaymentAmount();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.paymentStatus = 'success' AND p.donation.campaign.ngo.ngoId = :ngoId")
    BigDecimal getTotalSuccessfulPaymentAmountByNgoId(Integer ngoId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "LEFT JOIN p.donation d " +
            "LEFT JOIN d.campaign c " +
            "LEFT JOIN c.ngo cNgo " +
            "LEFT JOIN d.ngo dNgo " +
            "WHERE p.paymentStatus = 'success' " +
            "AND (cNgo.ngoId = :ngoId OR dNgo.ngoId = :ngoId)")
    BigDecimal getTotalSuccessfulPaymentAmountByScopedNgoId(@Param("ngoId") Integer ngoId);

    long countByPaymentStatus(Payment.PaymentStatus status);
}
