package com.example.apppecl3;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
public interface ApiService {
    @GET("GetDataFiltered")
    Call<List<MeasurementDto>> getMeasurementsFiltered(
            @Query("streetId") String streetId,
            @Query("device") String device,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );
    @GET("SetData")
    Call<Void> forcePedestrian(
            @Query("action") String action,
            @Query("streetId") String streetId,
            @Query("deviceId") int deviceId
    );
    @GET("SetData")
    Call<Void> setBuzzer(
            @Query("action") String action,
            @Query("enabled") boolean enabled,
            @Query("streetId") String streetId,
            @Query("deviceId") int deviceId
    );

    @GET("GetStreets")
    Call<List<String>> getStreets();
    @GET("GetDevicesByStreet")
    Call<List<Integer>> getDevicesByStreet(@Query("streetId") String streetId);

}

