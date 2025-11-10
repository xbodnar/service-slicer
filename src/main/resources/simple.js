import http from "k6/http";
import { sleep } from "k6";

export const options = {
  vus: 1,
  duration: "1s",
};

const BASE_URL = __ENV.BASE_URL;

export default function () {
  http.get(`${BASE_URL}/articles`);
  http.get(`${BASE_URL}/articles`);
  sleep(1);
}
