package com.example.invoicesystem.network;

import com.example.invoicesystem.model.Invoice;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

public interface InvoiceApi {
    @GET("/api/android/invoices")
    Call<List<Invoice>> getAllInvoices();

    @POST("/api/android/invoices")
    Call<Invoice> createInvoice(@Body Invoice invoice);

    @PUT("/api/android/invoices/{id}")
    Call<Invoice> updateInvoice(@Path("id") Long id, @Body Invoice invoice);

    @DELETE("/api/android/invoices/{id}")
    Call<Void> deleteInvoice(@Path("id") Long id);

    @Streaming
    @GET("/api/android/invoices/{id}/pdf")
    Call<ResponseBody> downloadInvoicePdf(@Path("id") Long id);
}
