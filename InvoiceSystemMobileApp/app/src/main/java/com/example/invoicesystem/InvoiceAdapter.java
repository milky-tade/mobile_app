package com.example.invoicesystem;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.invoicesystem.model.Invoice;
import com.google.gson.Gson;

import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {
    private List<Invoice> invoices;
    private OnGeneratePdfClickListener listener;

    public interface OnGeneratePdfClickListener {
        void onGeneratePdfClick(Invoice invoice);
    }

    public InvoiceAdapter(List<Invoice> invoices, OnGeneratePdfClickListener listener) {
        this.invoices = invoices;
        this.listener = listener;
    }

    public void updateInvoices(List<Invoice> newInvoices) {
        InvoiceDiffCallback diffCallback = new InvoiceDiffCallback(this.invoices, newInvoices);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.invoices.clear();
        this.invoices.addAll(newInvoices);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.invoice_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice invoice = invoices.get(position);
        Context context = holder.itemView.getContext();
        holder.clientName.setText(invoice.getClientName());
        holder.total.setText(context.getString(R.string.total, String.valueOf(invoice.getTotal())));

        holder.btnGeneratePdf.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGeneratePdfClick(invoice);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditInvoiceActivity.class);
            intent.putExtra("invoice", new Gson().toJson(invoice));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return invoices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView clientName, total;
        Button btnGeneratePdf;

        public ViewHolder(View itemView) {
            super(itemView);
            clientName = itemView.findViewById(R.id.tvClientName);
            total = itemView.findViewById(R.id.tvTotal);
            btnGeneratePdf = itemView.findViewById(R.id.btn_generate_pdf_item);
        }
    }
}
