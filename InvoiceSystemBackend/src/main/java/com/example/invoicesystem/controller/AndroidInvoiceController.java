package com.example.invoicesystem.controller;

import com.example.invoicesystem.model.Invoice;
import com.example.invoicesystem.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.invoicesystem.service.PdfGeneratorService;
import java.io.ByteArrayInputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.InputStreamResource;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/android")
@CrossOrigin(origins = "*") // Add this for frontend access
public class AndroidInvoiceController {

    @Autowired
    private InvoiceService invoiceService;
    
    @Autowired
    private PdfGeneratorService pdfGeneratorService;
    
    // CREATE
    @PostMapping("/invoices")
    public ResponseEntity<?> createInvoice(@RequestBody Invoice invoice) {
        try {
            Invoice createdInvoice = invoiceService.createInvoice(invoice);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdInvoice);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // GET ALL
    @GetMapping("/invoices")
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    // GET BY ID
    @GetMapping("/invoices/{id}")
    public ResponseEntity<?> getInvoice(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id);
            return ResponseEntity.ok(invoice);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // UPDATE
    // UPDATE - Accept both PUT and POST
@PutMapping("/invoices/{id}")
@PostMapping("/invoices/{id}")  // Add this line
public ResponseEntity<?> updateInvoice(
        @PathVariable Long id,
        @RequestBody Invoice updatedInvoice) {
    try {
        Invoice invoice = invoiceService.updateInvoice(id, updatedInvoice);
        return ResponseEntity.ok(invoice);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", e.getMessage()));
    }
}

    // DELETE - Option 1: Returns boolean
    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<?> deleteInvoice(@PathVariable Long id) {
        boolean deleted = invoiceService.deleteInvoice(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                "message", "Invoice deleted successfully",
                "success", true
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "message", "Invoice not found or could not be deleted",
                    "success", false
                ));
        }
    }
    
    // Alternative DELETE - Option 2: Uses void return
    /*
    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<?> deleteInvoice(@PathVariable Long id) {
        try {
            invoiceService.deleteInvoiceVoid(id);
            return ResponseEntity.ok(Map.of(
                "message", "Invoice deleted successfully",
                "success", true
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "message", e.getMessage(),
                    "success", false
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "message", "Error deleting invoice: " + e.getMessage(),
                    "success", false
                ));
        }
    }
    */
    
    @GetMapping("/invoices/{id}/pdf")
public ResponseEntity<InputStreamResource> downloadInvoicePdf(@PathVariable Long id) {
    try {
        // Get the invoice
        Invoice invoice = invoiceService.getInvoiceById(id);
        
        // Generate PDF
        ByteArrayInputStream pdfStream = pdfGeneratorService.generateInvoicePdf(invoice);
        
        // Set response headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=invoice_" + id + ".pdf");
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
                
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
 }
}