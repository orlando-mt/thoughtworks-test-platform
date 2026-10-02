# platform-engineer-problem-1


## How to test locally

### Register a new user
Curl example
```shell
curl -X 'POST' \
  'http://localhost:8080/api/register' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "q1w2e3"
}'
```

Expected output
```json
{"id":2,"name":"John Doe","email":"john.doe@example.com"}
```
### Auth with user and password
```shell
curl -X 'POST' \
  'http://localhost:8080/api/auth' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "john.doe@example.com",
  "password": "q1w2e3"
}'
```

Expected output
```json
{"message":"Successfully authenticated user.","id":"2","name":"John Doe","email":"john.doe@example.com","accessToken":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTY4MzY1Nzg1MX0.CWz_oMSXkgMuc79kcEFBE0FcyLwYzq23o-vbfJPyyx0"}
```

### Execute an API call
We tested here with GET on `/api/users'
```shell
curl -X 'GET' \
  'http://localhost:8080/api/users' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTY4MzY1Nzg1MX0.CWz_oMSXkgMuc79kcEFBE0FcyLwYzq23o-vbfJPyyx0'
```

Expected output
```json
[{"id":1,"name":"John Doe","email":"john.doe@example.com"}]%
```

