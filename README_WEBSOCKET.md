# Hướng Dẫn Tích Hợp WebSocket STOMP (Dành Cho Frontend)

Tài liệu này chứa nội dung hướng dẫn và đoạn mã nguồn hoàn chỉnh để bạn (hoặc AI coding assistant của bạn) thay thế cơ chế WebSocket thô cũ (FastAPI) bằng kết nối **STOMP WebSocket** mới của **Spring Boot**.

---

## 1. Thông Tin Cấu Hình WebSocket Kết Nối Đến Backend
* **Endpoint kết nối chính (Broker URL)**: `ws://localhost:8080/ws/websocket`
  *(Sử dụng giao thức STOMP trên nền tảng WebSocket thô của SockJS)*.
* **Chủ đề đăng ký (Subscription Topic)**: `/parking/dashboard`
* **Thời gian tự động kết nối lại (Reconnect Delay)**: `5000`ms (5 giây).
* **Heartbeat**: 4000ms / 4000ms.

---

## 2. Prompt Yêu Cầu Code Cho Frontend AI (Dán Trực Tiếp)

Bạn có thể copy đoạn bên dưới gửi cho AI của Frontend để yêu cầu code:

```text
Hãy cập nhật helper/composable kết nối WebSocket trong dự án Vue 3 + Vite của tôi. 
Trước đây chúng ta kết nối tới một WebSocket thô (FastAPI) qua endpoint /ws/telemetry. 
Bây giờ backend đã chuyển sang Spring Boot sử dụng WebSocket STOMP.

Yêu cầu:
1. Sử dụng thư viện `@stomp/stompjs` để thiết lập kết nối STOMP.
2. Endpoint kết nối (Broker URL) là: `ws://localhost:8080/ws/websocket`.
3. Khi kết nối thành công (onConnect), đăng ký (subscribe) lắng nghe chủ đề: `/parking/dashboard`.
4. Khi nhận được dữ liệu (chuyển đổi từ JSON string sang object), hãy gọi hàm `applyRealtimeMessage(data, slotsRef)` để cập nhật trạng thái ô đỗ xe thời gian thực.
5. Cập nhật logic trong `applyRealtimeMessage` để xử lý cấu trúc JSON mới từ Spring Boot:
   - Dữ liệu trả về từ kênh `/parking/dashboard` có cấu trúc:
     {
       "slots": [
         {
           "id": "Tên_Slot_VD_A01",
           "occupied": 1, // 1 là có xe, 0 là trống
           "timestamp": "2026-06-28T17:44:49"
         }
       ],
       "history": [...]
     }
   - Duyệt qua mảng `slots` trong tin nhắn đẩy về, tìm ô đỗ xe tương ứng trong state `slotsRef.value` (so khớp qua `id`) và cập nhật thuộc tính `occupied` (đổi 1 thành true, 0 thành false) cùng trường `timestamp`.
6. Đảm bảo hỗ trợ đầy đủ các hàm connect(slots), disconnect() và các biến reactive ref: `isConnected`.
```

---

## 3. Mã Nguồn Tham Khảo Cho File `useParkingRealtime.ts`

Hãy cài đặt thư viện `@stomp/stompjs` trước bằng lệnh:
```bash
pnpm add @stomp/stompjs
```

Sau đó thay thế nội dung file `src/composables/useParkingRealtime.ts` bằng đoạn code dưới đây:

```typescript
import { ref, type Ref } from 'vue'
import { Client } from '@stomp/stompjs'
import type { Slot } from '../module/type'

const isConnected = ref(false)
let stompClient: Client | null = null
let slotsRef: Ref<Slot[]> | null = null

/**
 * Cập nhật trạng thái các slot dựa trên thông tin thời gian thực từ Spring Boot WebSocket
 */
export function applyRealtimeMessage(payload: any, slots: Ref<Slot[]>) {
  if (payload && payload.slots) {
    for (const record of payload.slots) {
      // Tìm slot tương ứng trong state của Frontend dựa trên ID (Ví dụ: "A01", "D03")
      const slot = slots.value.find((s) => s.id === record.id)
      if (!slot) continue

      const wasOccupied = slot.occupied
      // Convert từ occupied (1/0) của backend sang boolean (true/false) của frontend
      slot.occupied = record.occupied === 1
      
      // Parse timestamp nếu có
      if (record.timestamp) {
        slot.timestamp = new Date(record.timestamp).getTime()
      }

      // Xử lý logic hiển thị mô hình xe 3D tương ứng
      if (!wasOccupied && slot.occupied) {
        slot.carType = slot.carType ?? 'Sedan'
        slot.carColor = slot.carColor ?? 'Silver'
      } else if (wasOccupied && !slot.occupied) {
        delete slot.carType
        delete slot.carColor
      }
    }
  }
}

export function useParkingRealtime() {
  function connect(slots: Ref<Slot[]>) {
    slotsRef = slots

    // Nếu đã kết nối rồi thì không tạo thêm kết nối mới
    if (stompClient && stompClient.connected) {
      return
    }

    stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws/websocket', // URL WebSocket của Spring Boot
      reconnectDelay: 5000, // Tự động kết nối lại sau 5 giây nếu đứt kết nối
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      onConnect: () => {
        isConnected.value = true
        console.log('Đã kết nối thành công đến Spring Boot STOMP Broker')
        
        // Đăng ký kênh nhận thông tin bãi đỗ xe thay đổi
        stompClient?.subscribe('/parking/dashboard', (message) => {
          try {
            const data = JSON.parse(message.body)
            if (slotsRef) {
              applyRealtimeMessage(data, slotsRef)
            }
          } catch (e) {
            console.error('Lỗi phân tích dữ liệu từ WebSocket:', e)
          }
        })
      },
      
      onDisconnect: () => {
        isConnected.value = false
        console.log('Đã ngắt kết nối với WebSocket Broker')
      },
      
      onStompError: (frame) => {
        console.error('Lỗi từ STOMP Broker:', frame.headers['message'])
        console.error('Chi tiết lỗi:', frame.body)
      }
    })

    stompClient.activate()
  }

  function disconnect() {
    if (stompClient) {
      stompClient.deactivate()
      stompClient = null
    }
    isConnected.value = false
    slotsRef = null
  }

  return {
    isConnected,
    connect,
    disconnect
  }
}
```
