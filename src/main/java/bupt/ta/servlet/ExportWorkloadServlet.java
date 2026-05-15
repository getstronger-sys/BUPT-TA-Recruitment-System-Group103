package bupt.ta.servlet;

import bupt.ta.model.AdminSettings;
import bupt.ta.service.AdminService;
import bupt.ta.storage.DataStorage;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Admin CSV export of TA workload derived from selected applications.
 */
public class ExportWorkloadServlet extends HttpServlet {

    private final AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DataStorage storage = new DataStorage(getServletContext());
        AdminSettings settings = storage.loadAdminSettings();
        List<AdminService.WorkloadRow> rows = adminService.buildWorkloadRows(storage, settings);

        resp.setContentType("text/csv;charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"ta_workload.csv\"");
        PrintWriter out = resp.getWriter();
        out.write("\uFEFF");
        out.println("TA Name,User ID,# Selected Jobs,Est. Hours,# Pending Jobs,Limit Status,Job Titles");
        for (AdminService.WorkloadRow row : rows) {
            String csvName = "\"" + safeCsv(row.getApplicantName()) + "\"";
            String csvTitles = "\"" + safeCsv(String.join("; ", row.getSelectedJobTitles())) + "\"";
            String limitStatus = row.isAboveLimit() ? "OVER_LIMIT"
                    : row.isAtOrOverLimit() ? "AT_LIMIT"
                    : "WITHIN_LIMIT";
            out.println(csvName + ",\"" + safeCsv(row.getApplicantId()) + "\"," + row.getSelectedCount()
                    + "," + String.format(java.util.Locale.US, "%.2f", row.getEstimatedSelectedHours())
                    + "," + row.getPendingCount() + ",\"" + limitStatus + "\"," + csvTitles);
        }
        out.flush();
    }

    private static String safeCsv(String value) {
        return value != null ? value.replace("\"", "\"\"") : "";
    }
}
