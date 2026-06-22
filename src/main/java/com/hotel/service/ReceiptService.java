package com.hotel.service;

import com.hotel.model.Payment;
import com.hotel.model.PaymentStatus;
import com.hotel.model.Receipt;
import com.hotel.repository.ReceiptRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {
    private final ReceiptRepository receipts;

    public ReceiptService(ReceiptRepository receipts) {
        this.receipts = receipts;
    }

    public Receipt createForPaidPayment(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID || payment.getGuest() == null) {
            return null;
        }
        return receipts.findByPaymentId(payment.getId()).orElseGet(() -> {
            String prefix = "RC" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            Receipt receipt = new Receipt();
            receipt.setReceiptNo(prefix + String.format("%04d", receipts.countByReceiptNoStartingWith(prefix) + 1));
            receipt.setGuest(payment.getGuest());
            receipt.setPayment(payment);
            receipt.setReceiptDate(payment.getPaymentDate() == null ? LocalDate.now() : payment.getPaymentDate());
            receipt.setTotalAmount(payment.getAmount());
            return receipts.save(receipt);
        });
    }
}
