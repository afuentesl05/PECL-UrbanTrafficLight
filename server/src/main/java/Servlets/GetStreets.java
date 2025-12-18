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

@WebServlet("/GetStreets")
public class GetStreets extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            ArrayList<String> streets = Logic.getStreetsFromDB();
            out.println(new Gson().toJson(streets));
        } catch (Exception e) {
            Log.log.error("Exception in GetStreets: ", e);
            out.println("[]");
        } finally {
            out.close();
        }
    }
}
