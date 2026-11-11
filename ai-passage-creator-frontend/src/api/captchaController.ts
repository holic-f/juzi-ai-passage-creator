// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /captcha/image */
export async function getCaptcha(options?: { [key: string]: any }) {
  return request<any>("/captcha/image", {
    method: "GET",
    ...(options || {}),
  });
}
