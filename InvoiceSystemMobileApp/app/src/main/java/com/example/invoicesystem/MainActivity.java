package com.example.invoicesystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAdd = findViewById(R.id.btnAddInvoice);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddInvoiceActivity.class);
            startActivity(intent);
        });
        
        Button btnSeeInvoices = findViewById(R.id.btnSeeInvoices);
        btnSeeInvoices.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewInvoicesActivity.class);
            startActivity(intent);
        });
    }
}
