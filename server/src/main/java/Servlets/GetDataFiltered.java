package servlets;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import logic.Log;
import logic.Logic;
import logic.Measurement;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

@WebServlet("/GetDataFiltered")
public class GetDataFiltered extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String streetId = firstNonEmpty(
                    request.getParameter("streetId"),
                    request.getParameter("street")      
            );

            String deviceParam = firstNonEmpty(
                    request.getParameter("device"),
                    request.getParameter("deviceId")
            );

            String startParam = firstNonEmpty(
                    request.getParameter("startDate"),
                    request.getParameter("start")
            );

            String endParam = firstNonEmpty(
                    request.getParameter("endDate"),
                    request.getParameter("end")
            );

            ArrayList<Measurement> values =
                    Logic.getDataFromDBFiltered(streetId, deviceParam, startParam, endParam);

            out.println(new Gson().toJson(values));

        } catch (Exception e) {
            Log.log.error("Exception in GetDataFiltered: ", e);
            out.println("[]");
        } finally {
            out.close();
        }
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
