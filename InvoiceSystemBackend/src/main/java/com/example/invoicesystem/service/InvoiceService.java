package com.example.invoicesystem.service;

import com.example.invoicesystem.model.Invoice;
import com.example.invoicesystem.model.LineItem;
import com.example.invoicesystem.repository.InvoiceRepository;
import com.example.invoicesystem.repository.LineItemRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private LineItemRepository lineItemRepository;

    // CREATE
    @Transactional
    public Invoice createInvoice(Invoice invoice) {
        // Save the invoice first to get the ID
        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        // Set the invoice reference for each line item and save them
        if (invoice.getLineItems() != null) {
            for (LineItem item : invoice.getLineItems()) {
                item.setInvoice(savedInvoice);
                lineItemRepository.save(item);
            }
        }
        
        return savedInvoice;
    }

    // READ ALL
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    // READ BY ID
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));
    }

    // UPDATE
    @Transactional
    public Invoice updateInvoice(Long id, Invoice updatedData) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Update invoice fields
        existing.setClientName(updatedData.getClientName());
        existing.setClientPhone(updatedData.getClientPhone());
        existing.setClientAddress(updatedData.getClientAddress());
        existing.setClientEmail(updatedData.getClientEmail());
        existing.setInvoiceDate(updatedData.getInvoiceDate());
        existing.setSubtotal(updatedData.getSubtotal());
        existing.setTaxPercentage(updatedData.getTaxPercentage());
        existing.setDiscount(updatedData.getDiscount());
        existing.setTotal(updatedData.getTotal());

        // Update line items
        lineItemRepository.deleteByInvoiceId(existing.getId());  // remove old items

        for (LineItem item : updatedData.getLineItems()) {
            item.setInvoice(existing);    // connect to invoice
            lineItemRepository.save(item);
        }

        return invoiceRepository.save(existing);
    }

    // DELETE
    @Transactional
    public boolean deleteInvoice(Long invoiceId) {
        try {
            // 1. Check if invoice exists
            if (!invoiceRepository.existsById(invoiceId)) {
                return false;
            }
            
            // 2. Delete all line items first
            lineItemRepository.deleteByInvoiceId(invoiceId);
            
            // 3. Delete invoice
            invoiceRepository.deleteById(invoiceId);
            
            return true;
        } catch (Exception e) {
            // Log the error if needed
            // e.printStackTrace();
            return false;
        }
    }
    
    // Alternative delete method (more explicit)
    @Transactional
    public void deleteInvoiceVoid(Long invoiceId) {
        // 1. Check if invoice exists
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        // 2. Delete all line items first
        lineItemRepository.deleteByInvoiceId(invoiceId);

        // 3. Delete invoice
        invoiceRepository.delete(invoice);
    }
}