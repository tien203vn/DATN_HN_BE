# rental-car-project-backend

## Tích hợp VNPAY nạp ví

Dịch vụ backend đã hỗ trợ nạp tiền vào ví người dùng thông qua cổng thanh toán VNPAY. Dưới đây là
những thông tin quan trọng để cấu hình và thử nghiệm luồng thanh toán.

### Biến môi trường cần thiết

Khai báo các biến sau trước khi khởi động ứng dụng (giá trị `DEMO_*` chỉ dùng để phát triển):

| Biến môi trường        | Ý nghĩa                                                                                 | Giá trị mẫu sandbox                                             |
|------------------------|------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `VNPAY_TMN_CODE`       | Mã terminal do VNPAY cấp                                                                 | `DEMO_TMN_CODE`                                                 |
| `VNPAY_HASH_SECRET`    | Chuỗi bí mật dùng để ký HMAC                                                             | `DEMO_SECRET_KEY`                                               |
| `VNPAY_RETURN_URL`     | Endpoint frontend nhận kết quả (nếu muốn override giá trị mặc định trong `application.yml`) | `http://localhost:8080/api/v1/payment/vnpay/return`              |
| `VNPAY_IPN_URL`        | Endpoint backend nhận IPN (có thể trỏ tới public URL/ngrok trong môi trường phát triển) | `http://localhost:8080/api/v1/payment/vnpay/ipn`                 |

Ngoài ra có thể điều chỉnh các khóa cấu hình khác trong nhóm `vnpay` của `application.yml` nếu cần thay đổi
`pay-url`, `api-url`, `expire-minutes`, …

### Luồng nạp ví

1. Người dùng đã đăng nhập gửi yêu cầu POST tới `/api/v1/payment/vnpay/create` với số tiền cần nạp.
2. Backend sinh mã giao dịch, ký chữ ký HMAC và trả về `paymentUrl` để frontend redirect sang trang VNPAY.
3. Sau khi thanh toán, VNPAY gọi đồng thời:
	 - IPN tới `/api/v1/payment/vnpay/ipn` (backend xử lý, cập nhật ví + trạng thái giao dịch).
	 - Return URL (frontend hoặc backend dùng để hiển thị kết quả cho người dùng).
4. Hệ thống chỉ cộng tiền vào `wallet` khi chữ ký hợp lệ và `vnp_ResponseCode = "00"`.

### API chi tiết

#### 1. Tạo giao dịch nạp ví

- **Endpoint:** `POST /api/v1/payment/vnpay/create`
- **Yêu cầu bảo mật:** Bearer token (JWT)
- **Request body:**

```json
{
	"amount": 200000,
	"bankCode": "NCB",
	"language": "vn",
	"returnUrl": "https://frontend.local/vnpay/return"
}
```

- **Phản hồi thành công (`201 CREATED`):**

```json
{
	"success": true,
	"status": 201,
	"message": "Tạo liên kết thanh toán thành công",
	"data": {
		"paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/...",
		"transactionReference": "RC240927123456",
		"amount": 200000,
		"expireAt": "2024-09-27T12:35:00+07:00"
	}
}
```

#### 2. Nhận kết quả từ Return URL

- **Endpoint:** `GET /api/v1/payment/vnpay/return`
- **Trả về:** cấu trúc `Response<VnpayReturnResponseDTO>` để frontend hiển thị thông điệp thành công/thất bại.

#### 3. Callback IPN từ VNPAY

- **Endpoint:** `GET /api/v1/payment/vnpay/ipn`
- **Trả về:** đối tượng `VnpayCallbackResponseDTO` bao gồm mã kết quả (`RspCode`) gửi ngược lại VNPAY.

### Kiểm thử nhanh

```powershell
mvn -q test
```

Lệnh trên sẽ chạy toàn bộ bộ test JUnit (bao gồm kiểm tra `RentalCalculateUtil`) để đảm bảo build ổn định sau
khi cấu hình VNPAY.