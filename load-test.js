import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 500 },
    { duration: '30s', target: 1000 },
    { duration: '30s', target: 2000 },
    { duration: '30s', target: 3000 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],  // พัง ถ้า p95 เกิน 1 วิ
    http_req_failed: ['rate<0.05'],     // พัง ถ้า error เกิน 5%
  },
};

export default function () {
  const res = http.post('http://192.168.1.35:8080/api/orders',
    JSON.stringify({
      productName: 'Tablet',
      quantity: 1,
      price: '15000.00'
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    'status is 201': (r) => r.status === 201,
  });

  sleep(0.05);
}