package com.example.invoicesystem.service;

import com.example.invoicesystem.model.Invoice;
import com.example.invoicesystem.model.LineItem;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class PdfGeneratorService {

    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
    private static final Font SUB_HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 13, Font.NORMAL);
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font TABLE_HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);

    public ByteArrayInputStream generateInvoicePdf(Invoice invoice) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // ---------- HEADER ----------
            Paragraph header = new Paragraph("FOMS Technologies", HEADER_FONT);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subHeader = new Paragraph("INVOICE SAVER AND PDF GENERATOR", SUB_HEADER_FONT);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);

            document.add(Chunk.NEWLINE);

            // ---------- INVOICE TITLE ----------
            Paragraph title = new Paragraph("INVOICE #" + invoice.getId(), TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(Chunk.NEWLINE);

            // ---------- CLIENT INFO ----------
            PdfPTable clientTable = new PdfPTable(2);
            clientTable.setWidthPercentage(100);
            clientTable.setWidths(new float[]{1, 3});
            clientTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

            addClientRow(clientTable, "Client Name:", invoice.getClientName());
            addClientRow(clientTable, "Email:", invoice.getClientEmail());
            addClientRow(clientTable, "Phone:", invoice.getClientPhone());
            addClientRow(clientTable, "Address:", invoice.getClientAddress());
            addClientRow(clientTable, "Invoice Date:", String.valueOf(invoice.getInvoiceDate()));

            document.add(clientTable);

            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // ---------- LINE ITEMS TABLE ----------
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1, 2, 2});
            
            // Table headers
            addTableHeader(table, "Item Description");
            addTableHeader(table, "Qty");
            addTableHeader(table, "Unit Price");
            addTableHeader(table, "Total");

            // Add line items
            double subtotal = 0;
            for (LineItem item : invoice.getLineItems()) {
                table.addCell(new PdfPCell(new Phrase(item.getItemName(), CELL_FONT)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), CELL_FONT)));
                table.addCell(new PdfPCell(new Phrase(formatCurrency(item.getUnitPrice()), CELL_FONT)));
                table.addCell(new PdfPCell(new Phrase(formatCurrency(item.getTotalPrice()), CELL_FONT)));
                subtotal += item.getTotalPrice();
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // ---------- SUMMARY ----------
            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(40);
            summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summary.setWidths(new float[]{2, 1});

            // Calculate values
            double taxAmount = subtotal * invoice.getTaxPercentage() / 100;
            double discountAmount = subtotal * invoice.getDiscount() / 100;
            double total = subtotal + taxAmount - discountAmount;

            addSummaryRow(summary, "Subtotal:", formatCurrency(subtotal));
            addSummaryRow(summary, "Tax (" + invoice.getTaxPercentage() + "%):", formatCurrency(taxAmount));
            addSummaryRow(summary, "Discount (" + invoice.getDiscount() + "%):", formatCurrency(discountAmount));
            
            // Add a separator line before total
            PdfPCell emptyCell1 = new PdfPCell(new Phrase(""));
            PdfPCell emptyCell2 = new PdfPCell(new Phrase(""));
            emptyCell1.setBorder(PdfPCell.NO_BORDER);
            emptyCell2.setBorder(PdfPCell.NO_BORDER);
            summary.addCell(emptyCell1);
            summary.addCell(emptyCell2);
            
            // Total row
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL:", BOLD_FONT));
            totalLabelCell.setBorder(PdfPCell.NO_BORDER);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(total), BOLD_FONT));
            totalValueCell.setBorder(PdfPCell.NO_BORDER);
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            summary.addCell(totalLabelCell);
            summary.addCell(totalValueCell);

            document.add(summary);

            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            
            // ---------- FOOTER ----------
            Paragraph footer = new Paragraph("Thank you for choosing FOMS Technologies!", SUB_HEADER_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ---------- HELPER METHODS ----------
    
    private void addClientRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key, CELL_FONT));
        keyCell.setBorder(PdfPCell.NO_BORDER);
        keyCell.setPaddingBottom(5);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", CELL_FONT));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPaddingBottom(5);
        
        table.addCell(keyCell);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String header) {
        PdfPCell cell = new PdfPCell(new Phrase(header, TABLE_HEADER_FONT));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, CELL_FONT));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(5);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, CELL_FONT));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        return formatter.format(amount);
    }
}