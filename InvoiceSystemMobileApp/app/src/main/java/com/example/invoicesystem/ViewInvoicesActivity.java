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
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.invoicesystem.model.Invoice;
import com.example.invoicesystem.network.ApiClient;
import com.example.invoicesystem.network.InvoiceApi;
import okhttp3.ResponseBody;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewInvoicesActivity extends AppCompatActivity implements InvoiceAdapter.OnGeneratePdfClickListener {
    private RecyclerView recyclerInvoices;
    private EditText etSearch;
    private Button btnSearch;
    private InvoiceAdapter adapter;
    private InvoiceApi invoiceApi;
    private List<Invoice> invoiceList = new ArrayList<>();
    private Invoice invoiceToDownload;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_invoices);

        recyclerInvoices = findViewById(R.id.recyclerInvoices);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);

        invoiceApi = ApiClient.getApiClient().create(InvoiceApi.class);

        recyclerInvoices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InvoiceAdapter(invoiceList, this);
        recyclerInvoices.setAdapter(adapter);

        fetchInvoices();

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!TextUtils.isEmpty(query)) {
                searchInvoices(query);
            } else {
                fetchInvoices();
            }
        });
    }

    private void fetchInvoices() {
        invoiceApi.getAllInvoices().enqueue(new Callback<List<Invoice>>() {
            @Override
            public void onResponse(Call<List<Invoice>> call, Response<List<Invoice>> response) {
                if (response.isSuccessful()) {
                    invoiceList = response.body();
                    adapter.updateInvoices(invoiceList);
                }
            }

            @Override
            public void onFailure(Call<List<Invoice>> call, Throwable t) {
                Toast.makeText(ViewInvoicesActivity.this, "Failed to load invoices", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchInvoices(String query) {
        List<Invoice> filtered = invoiceList.stream()
                .filter(i -> i.getClientName().toLowerCase().contains(query.toLowerCase())
                        || String.valueOf(i.getId()).equals(query))
                .collect(Collectors.toList());
        adapter.updateInvoices(filtered);
    }

    @Override
    public void onGeneratePdfClick(Invoice invoice) {
        invoiceToDownload = invoice;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                downloadPdfForInvoice(invoice);
            }
        } else {
            downloadPdfForInvoice(invoice);
        }
    }

    private void downloadPdfForInvoice(Invoice invoice) {
        invoiceApi.downloadInvoicePdf(invoice.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executor.execute(() -> {
                        boolean success = savePdfToDownloads(response.body(), invoice);
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(ViewInvoicesActivity.this, "PDF saved in Downloads folder", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ViewInvoicesActivity.this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                } else {
                    Toast.makeText(ViewInvoicesActivity.this, "Failed to download PDF", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(ViewInvoicesActivity.this, "Failed to download PDF: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean savePdfToDownloads(ResponseBody body, Invoice invoice) {
        String fileName = "Invoice_" + invoice.getId() + ".pdf";
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
                if (invoiceToDownload != null) {
                    downloadPdfForInvoice(invoiceToDownload);
                }
            } else {
                Toast.makeText(this, "Permission denied. Unable to save PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
