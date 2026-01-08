package com.example.invoicesystem;

import androidx.recyclerview.widget.DiffUtil;

import com.example.invoicesystem.model.Invoice;

import java.util.List;

public class InvoiceDiffCallback extends DiffUtil.Callback {
    private final List<Invoice> oldList;
    private final List<Invoice> newList;

    public InvoiceDiffCallback(List<Invoice> oldList, List<Invoice> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
