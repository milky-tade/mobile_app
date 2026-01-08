package com.example.invoicesystem;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.invoicesystem.model.Invoice;
import com.example.invoicesystem.model.LineItem;
import com.example.invoicesystem.network.ApiClient;
import com.example.invoicesystem.network.InvoiceApi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddInvoiceActivity extends AppCompatActivity {

    private EditText etClientName, etEmail, etAddress, etClientPhone, etDiscount, etTax;
    private TextView tvTotal;
    private LinearLayout layoutItems;
    private Button btnAddItem, btnSaveInvoice;
    private List<LineItem> itemList = new ArrayList<>();
    private InvoiceApi invoiceApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_invoice);

        etClientName = findViewById(R.id.etClientName);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        etClientPhone = findViewById(R.id.etClientPhone);
        etDiscount = findViewById(R.id.etDiscount);
        etTax = findViewById(R.id.etTax);
        tvTotal = findViewById(R.id.tvTotal);
        layoutItems = findViewById(R.id.layoutItems);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnSaveInvoice = findViewById(R.id.btnSaveInvoice);

        invoiceApi = ApiClient.getApiClient().create(InvoiceApi.class);

        btnAddItem.setOnClickListener(v -> addItemField());
        btnSaveInvoice.setOnClickListener(v -> saveInvoice());
    }

    private void addItemField() {
        View itemView = getLayoutInflater().inflate(R.layout.item_row, layoutItems, false);
        layoutItems.addView(itemView);
    }

    private void calculateTotal() {
        double subtotal = 0;
        itemList.clear();

        for (int i = 0; i < layoutItems.getChildCount(); i++) {
            View view = layoutItems.getChildAt(i);
            EditText etName = view.findViewById(R.id.itemName);
            EditText etQty = view.findViewById(R.id.quantity);
            EditText etPrice = view.findViewById(R.id.unitPrice);

            String name = etName.getText().toString();
            double qty = etQty.getText().toString().isEmpty() ? 0 : Double.parseDouble(etQty.getText().toString());
            double price = etPrice.getText().toString().isEmpty() ? 0 : Double.parseDouble(etPrice.getText().toString());

            double total = qty * price;
            subtotal += total;

            LineItem lineItem = new LineItem();
            lineItem.setItemName(name);
            lineItem.setQuantity((int) qty);
            lineItem.setUnitPrice(price);
            lineItem.setTotalPrice(total);
            itemList.add(lineItem);
        }

        double discount = etDiscount.getText().toString().isEmpty() ? 0 : Double.parseDouble(etDiscount.getText().toString());
        double tax = etTax.getText().toString().isEmpty() ? 0 : Double.parseDouble(etTax.getText().toString());

        double totalAfterDiscount = subtotal - (subtotal * discount / 100);
        double finalTotal = totalAfterDiscount + (totalAfterDiscount * tax / 100);

        tvTotal.setText("Total: " + String.format("%.2f", finalTotal));
    }

    private void saveInvoice() {
        calculateTotal();

        Invoice invoice = new Invoice();
        invoice.setClientName(etClientName.getText().toString());
        invoice.setClientEmail(etEmail.getText().toString());
        invoice.setClientAddress(etAddress.getText().toString());
        invoice.setClientPhone(etClientPhone.getText().toString());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        invoice.setInvoiceDate(sdf.format(new Date()));

        double subtotal = 0;
        for (LineItem item : itemList) {
            subtotal += item.getTotalPrice();
        }

        double discount = etDiscount.getText().toString().isEmpty() ? 0 : Double.parseDouble(etDiscount.getText().toString());
        double tax = etTax.getText().toString().isEmpty() ? 0 : Double.parseDouble(etTax.getText().toString());
        double totalAfterDiscount = subtotal - (subtotal * discount / 100);
        double finalTotal = totalAfterDiscount + (totalAfterDiscount * tax / 100);

        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discount);
        invoice.setTaxPercentage(tax);
        invoice.setTotal(finalTotal);
        invoice.setLineItems(itemList);

        Call<Invoice> call = invoiceApi.createInvoice(invoice);

        call.enqueue(new Callback<Invoice>() {
            @Override
            public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddInvoiceActivity.this, "Invoice saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    try {
                        String errorBody;
                        errorBody = response.errorBody().string();
                        Log.e("SERVER_ERROR", errorBody);
                        Toast.makeText(AddInvoiceActivity.this, "Failed to save invoice: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Log.e("SERVER_ERROR", "Error reading error body", e);
                        Toast.makeText(AddInvoiceActivity.this, "Failed to save invoice and couldn't read error response", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Invoice> call, Throwable t) {
                Log.e("NETWORK_ERROR", "Failed to save invoice", t);
                Toast.makeText(AddInvoiceActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
