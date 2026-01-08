package com.example.invoicesystem;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.invoicesystem.model.Invoice;
import com.example.invoicesystem.model.LineItem;
import com.example.invoicesystem.network.ApiClient;
import com.example.invoicesystem.network.InvoiceApi;
import com.google.gson.Gson;
import okhttp3.ResponseBody;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditInvoiceActivity extends AppCompatActivity {

    private EditText etClientName, etEmail, etAddress, etClientPhone, etDiscount, etTax;
    private TextView tvTotal;
    private LinearLayout layoutItems;
    private Button btnAddItem, btnUpdateInvoice, btnDeleteInvoice, btnGeneratePDF;
    private List<LineItem> itemList = new ArrayList<>();
    private InvoiceApi invoiceApi;
    private Invoice currentInvoice;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_invoice);

        etClientName = findViewById(R.id.etClientName);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        etClientPhone = findViewById(R.id.etClientPhone);
        etDiscount = findViewById(R.id.etDiscount);
        etTax = findViewById(R.id.etTax);
        tvTotal = findViewById(R.id.tvTotal);
        layoutItems = findViewById(R.id.layoutItems);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnUpdateInvoice = findViewById(R.id.btnUpdateInvoice);
        btnDeleteInvoice = findViewById(R.id.btnDeleteInvoice);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);

        invoiceApi = ApiClient.getApiClient().create(InvoiceApi.class);

        String invoiceJson = getIntent().getStringExtra("invoice");
        currentInvoice = new Gson().fromJson(invoiceJson, Invoice.class);

        if (currentInvoice != null) {
            populateInvoiceData();
        }

        btnAddItem.setOnClickListener(v -> addItemField());
        btnUpdateInvoice.setOnClickListener(v -> updateInvoice());
        btnDeleteInvoice.setOnClickListener(v -> deleteInvoice());
        btnGeneratePDF.setOnClickListener(v -> {
            if (currentInvoice != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                } else {
                    downloadPdfForInvoice(currentInvoice);
                }
            }
        });
    }

    private void populateInvoiceData() {
        etClientName.setText(currentInvoice.getClientName());
        etEmail.setText(currentInvoice.getClientEmail());
        etAddress.setText(currentInvoice.getClientAddress());
        etClientPhone.setText(currentInvoice.getClientPhone());
        etDiscount.setText(String.valueOf(currentInvoice.getDiscount()));
        etTax.setText(String.valueOf(currentInvoice.getTaxPercentage()));

        for (LineItem item : currentInvoice.getLineItems()) {
            addItemField(item);
        }

        calculateTotal();
    }

    private void addItemField() {
        addItemField(null);
    }

    private void addItemField(LineItem item) {
        View itemView = getLayoutInflater().inflate(R.layout.item_row, layoutItems, false);
        EditText etName = itemView.findViewById(R.id.itemName);
        EditText etQty = itemView.findViewById(R.id.quantity);
        EditText etPrice = itemView.findViewById(R.id.unitPrice);

        if (item != null) {
            etName.setText(item.getItemName());
            etQty.setText(String.valueOf(item.getQuantity()));
            etPrice.setText(String.valueOf(item.getUnitPrice()));
        }

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

    private void updateInvoice() {
        calculateTotal();

        currentInvoice.setClientName(etClientName.getText().toString());
        currentInvoice.setClientEmail(etEmail.getText().toString());
        currentInvoice.setClientAddress(etAddress.getText().toString());
        currentInvoice.setClientPhone(etClientPhone.getText().toString());

        double subtotal = 0;
        for (LineItem item : itemList) {
            subtotal += item.getTotalPrice();
        }

        double discount = etDiscount.getText().toString().isEmpty() ? 0 : Double.parseDouble(etDiscount.getText().toString());
        double tax = etTax.getText().toString().isEmpty() ? 0 : Double.parseDouble(etTax.getText().toString());
        double totalAfterDiscount = subtotal - (subtotal * discount / 100);
        double finalTotal = totalAfterDiscount + (totalAfterDiscount * tax / 100);

        currentInvoice.setSubtotal(subtotal);
        currentInvoice.setDiscount(discount);
        currentInvoice.setTaxPercentage(tax);
        currentInvoice.setTotal(finalTotal);
        currentInvoice.setLineItems(itemList);

        Call<Invoice> call = invoiceApi.updateInvoice(currentInvoice.getId(), currentInvoice);

        call.enqueue(new Callback<Invoice>() {
            @Override
            public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditInvoiceActivity.this, "Invoice updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("SERVER_ERROR", errorBody);
                        Toast.makeText(EditInvoiceActivity.this, "Failed to update invoice: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Log.e("SERVER_ERROR", "Error reading error body", e);
                        Toast.makeText(EditInvoiceActivity.this, "Failed to update invoice and couldn\'t read error response", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Invoice> call, Throwable t) {
                Log.e("NETWORK_ERROR", "Failed to update invoice", t);
                Toast.makeText(EditInvoiceActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteInvoice() {
        Call<Void> call = invoiceApi.deleteInvoice(currentInvoice.getId());

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditInvoiceActivity.this, "Invoice deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("SERVER_ERROR", errorBody);
                        Toast.makeText(EditInvoiceActivity.this, "Failed to delete invoice: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Log.e("SERVER_ERROR", "Error reading error body", e);
                        Toast.makeText(EditInvoiceActivity.this, "Failed to delete invoice and couldn\'t read error response", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("NETWORK_ERROR", "Failed to delete invoice", t);
                Toast.makeText(EditInvoiceActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void downloadPdfForInvoice(Invoice invoice) {
        invoiceApi.downloadInvoicePdf(invoice.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executor.execute(() -> {
                        boolean success = savePdfToDownloads(response.body());
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(EditInvoiceActivity.this, "PDF saved in Downloads folder", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(EditInvoiceActivity.this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                } else {
                    Toast.makeText(EditInvoiceActivity.this, "Failed to download PDF", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(EditInvoiceActivity.this, "Failed to download PDF: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean savePdfToDownloads(ResponseBody body) {
        String fileName = "Invoice_" + currentInvoice.getId() + ".pdf";
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                outputStream = resolver.openOutputStream(uri);
            } else {
                String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
                        + "/" + fileName;
                outputStream = new FileOutputStream(filePath);
            }

            if (outputStream != null) {
                inputStream = body.byteStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadPdfForInvoice(currentInvoice);
            } else {
                Toast.makeText(this, "Permission denied. Unable to save PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
