# P7B Runtime Port Contract

| Component | Default loopback endpoint | Override |
| --- | --- | --- |
| MySQL 8.4 | `127.0.0.1:3307` | `MYSQL_PORT` |
| Redis 8.0.2 | `127.0.0.1:6380` | `REDIS_PORT` |
| Mall API | `127.0.0.1:8080` | `MALL_API_PORT` |
| AI service | `127.0.0.1:8000` | `AI_SERVICE_PORT` |
| Admin | `127.0.0.1:5174` | `ADMIN_WEB_PORT` / `VITE_ADMIN_PORT` |
| Mobile H5 | `127.0.0.1:5173` | `MOBILE_H5_PORT` / `VITE_MOBILE_PORT` |

Admin and Mobile proxy `/api` to the selected Java API port. Java CORS is configured through `COMMERCEFLOW_CORS_ALLOWED_ORIGINS`; its template default explicitly lists `http://127.0.0.1:5174,http://127.0.0.1:5173`.

`8080` was occupied by an unknown WSL relay on the verification machine. `start.ps1` failed closed and did not terminate it. A controlled environment override started the Showcase API at `8081`; `status.ps1` and `verify.ps1` read the recorded runtime PID/port rather than incorrectly falling back to the template port.
