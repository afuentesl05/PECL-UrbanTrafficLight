package servlets;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import logic.Log;
import logic.Logic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

@WebServlet("/GetDevicesByStreet")
public class GetDevicesByStreet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String streetId = request.getParameter("streetId");
        if (streetId == null || streetId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("[]");
            out.close();
            return;
        }

        try {
            ArrayList<Integer> devices = Logic.getDevicesByStreetFromDB(streetId);
            out.println(new Gson().toJson(devices));
        } catch (Exception e) {
            Log.log.error("Exception in GetDevicesByStreet: ", e);
            out.println("[]");
        } finally {
            out.close();
        }
    }
}
