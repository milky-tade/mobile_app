package com.example.invoicesystem.repository;

import com.example.invoicesystem.model.LineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LineItemRepository extends JpaRepository<LineItem, Long> {
    void deleteByInvoiceId(Long invoiceId);
}
