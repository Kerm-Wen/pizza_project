package student.wwj.pizza_project.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 判斷是否為業主專屬操作
        boolean isAdminPage = uri.equals("/admin.html");
        boolean isGetAllOrders = uri.equals("/api/orders") && "GET".equalsIgnoreCase(method);
        boolean isUpdateStatus = uri.matches("^/api/orders/\\d+/status$");

        if (!isAdminPage && !isGetAllOrders && !isUpdateStatus) {
            return true; // 非業主專屬路徑直接放行
        }

        HttpSession session = request.getSession(false);
        String role = (session != null) ? (String) session.getAttribute("loginRole") : null;

        if (!"ADMIN".equals(role)) {
            if (isAdminPage) {
                response.sendRedirect("/admin-login.html"); // 網頁請求導向業主登入頁
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // API 請求回傳 403 Forbidden
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\": \"權限不足：僅限業主帳號操作\"}");
            }
            return false;
        }
        return true;
    }
}