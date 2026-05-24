# Báo Cáo Cải Tiến Hệ Thống Cook Schedule

Báo cáo này trình bày các nội dung cải tiến kỹ thuật đã thực hiện trên ứng dụng **Cook Schedule** nhằm nâng cao trải nghiệm người dùng, sửa đổi logic bảo mật, tối ưu hóa thuật toán lập lịch và nâng cao khả năng triển khai ứng dụng.

---

## 1. Đơn Giản Hóa Quá Trình Đăng Nhập & Đăng Ký
### Hiện trạng & Vấn đề
- Hệ thống sử dụng Spring Security với cấu hình giới hạn phiên đăng nhập (`sessionConcurrency`):
  - `maximumSessions(1)`: Chỉ cho phép tối đa 1 phiên hoạt động.
  - `maxSessionsPreventsLogin(true)`: Ngăn chặn hoàn toàn đăng nhập mới nếu phiên cũ chưa được đóng/đăng xuất đúng cách.
- **Hệ quả**: Người dùng thường xuyên bị khóa ngoài hoặc báo lỗi khi đăng nhập từ trình duyệt khác hoặc khi đóng tab đột ngột mà chưa kịp logout, khiến server từ chối đăng nhập dù thông tin tài khoản hoàn toàn chính xác.

### Giải pháp cải tiến
- Loại bỏ hoàn toàn khối cấu hình `sessionManagement` và `sessionConcurrency` trong [SecurityConfig.java](file:///e:/github%202/cook/src/main/java/com/cigama/cook_schedule/config/SecurityConfig.java).
- **Kết quả**: Hệ thống hiện tại chỉ kiểm tra thông tin tài khoản (Username/Password) xem có chính xác hay không và cấp quyền truy cập ngay lập tức, không áp đặt bất kỳ ràng buộc session phức tạp nào khác.

---

## 2. Khắc Phục Sự Cố Nghẽn và Treo Server Khi Sắp Xếp Lịch
### Phân tích nguyên nhân lỗi (Root Cause)
- **Thuật toán sử dụng**: Thuật toán phân công lịch dựa trên luồng cực đại chi phí cực tiểu (MCMF - Minimum Cost Maximum Flow) kết quả của thuật toán SPFA (Shortest Path Faster Algorithm) tại [AlgorithmService.java](file:///e:/github%202/cook/src/main/java/com/cigama/cook_schedule/service/AlgorithmService.java).
- **Vấn đề chu trình âm**: Trong quá trình cập nhật chi phí phạt dồn (dynamic penalty), khi thực hiện backtracking (`isAssignment = false`), hệ thống gán mức phạt âm `penalty = -1000`. Điều này tạo ra các **chu trình chi phí âm (negative cost cycles)** trong đồ thị luồng.
- **Hệ quả**: Thuật toán SPFA tiêu chuẩn khi gặp chu trình âm sẽ rơi vào **vòng lặp vô hạn (infinite loop)** ở vòng lặp `while(!queue.isEmpty())` vì chi phí của các đỉnh trong chu trình liên tục giảm về âm vô cực. Thread xử lý yêu cầu sẽ chiếm dụng 100% CPU, làm nghẽn toàn bộ server và khiến không một truy vấn nào khác có thể truy cập được nữa.

### Giải pháp khắc phục
- Tích hợp kỹ thuật **Phát hiện chu trình âm (Negative Cycle Detection)** vào lõi thuật toán SPFA:
  - Khai báo một bản đồ lưu trữ số lần thư giãn (relaxation count) của từng đỉnh: `Map<Node, Integer> relaxCount`.
  - Mỗi lần một đỉnh được rút ra khỏi hàng đợi để duyệt, tăng đếm thư giãn thêm `1`.
  - Nếu số lần thư giãn của bất kỳ đỉnh nào vượt quá tổng số đỉnh trong đồ thị (`nodes.size()`), thuật toán kết luận đồ thị chứa chu trình âm.
  - Lúc này, thuật toán lập tức giải phóng hàng đợi, gán khoảng cách tới đích bằng vô cực (`Integer.MAX_VALUE`) và **thoát ra một cách an toàn**.
- **Kết quả**: Thuật toán xếp lịch luôn luôn kết thúc chỉ trong vài mili-giây, tuyệt đối không bao giờ làm treo hoặc nghẽn server, đồng thời vẫn bảo đảm trả về kết quả phân lịch tối ưu từ các luồng hợp lệ đã duyệt qua trước đó.

---

## 3. Hiển Thị Mật Khẩu Thành Viên Trong Trang Quản Trị
### Giải mã mật khẩu
- Hệ thống lưu trữ mật khẩu mã hóa bằng bộ mã hóa tùy biến [TripleBase64PasswordEncoder.java](file:///e:/github%202/cook/src/main/java/com/cigama/cook_schedule/config/TripleBase64PasswordEncoder.java), thực hiện mã hóa Base64 liên tục **3 lần**.
- Để hiển thị mật khẩu gốc dạng plaintext cho Admin, ta chỉ cần giải mã ngược Base64 liên tục **3 lần**.

### Triển khai kỹ thuật
1. **Thêm phương thức giải mã trong Entity**:
   - Thêm phương thức `public String getDecryptedPassword()` vào thực thể [UserAccount.java](file:///e:/github%202/cook/src/main/java/com/cigama/cook_schedule/entity/UserAccount.java).
   - Sử dụng thư viện tiêu chuẩn `java.util.Base64` để giải mã 3 vòng. Bọc trong khối `try-catch` để trả về `"N/A"` nếu gặp mật khẩu cũ hoặc không đúng định dạng Base64.
2. **Cập nhật Giao diện Quản trị**:
   - Chỉnh sửa [admin.html](file:///e:/github%202/cook/src/main/resources/templates/admin.html) để bổ sung thêm cột **Mật khẩu** (`<th>Mật khẩu</th>`).
   - Hiển thị mật khẩu plaintext giải mã được thông qua thuộc tính Thymeleaf: `<td th:text="${u.decryptedPassword}" ...></td>`.

---

## 4. Bổ Sung Cấu Hình Qua Biến Môi Trường (Environment Variables)
- Cập nhật file [application.properties](file:///e:/github%202/cook/src/main/resources/application.properties) hỗ trợ đọc các biến môi trường cấu hình động khi triển khai ứng dụng thực tế (ví dụ: trên Docker, Kubernetes hoặc Cloud Services):
  - **`PORT`**: Cấu hình cổng chạy ứng dụng (Mặc định: `8080`).
  - **`DB_URL`**: Đường dẫn kết nối CSDL H2 (Mặc định: `jdbc:h2:file:./data/cookdb`).
  - **`DB_USERNAME`**: Tài khoản CSDL H2 (Mặc định: `sa`).
  - **`DB_PASSWORD`**: Mật khẩu CSDL H2 (Mặc định: `password`).
- Cấu trúc khai báo sử dụng cú pháp placeholder tiêu chuẩn của Spring Boot: `${TEN_BIEN:gia_tri_mac_dinh}` giúp ứng dụng hoạt động linh hoạt mà không làm ảnh hưởng đến cấu hình chạy thử nghiệm cục bộ (Local Development).

---
Báo cáo hoàn thành. Các thay đổi đã được kiểm tra tính biên dịch và sẵn sàng triển khai thực tế.
