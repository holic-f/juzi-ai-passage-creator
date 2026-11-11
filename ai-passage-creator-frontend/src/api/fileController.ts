// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /file/avatar/${param0}/${param1} */
export async function getAvatar(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAvatarParams,
  options?: { [key: string]: any }
) {
  const { userId: param0, filename: param1, ...queryParams } = params;
  return request<any>(`/file/avatar/${param0}/${param1}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /file/test/download/ */
export async function testDownloadFile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.testDownloadFileParams,
  options?: { [key: string]: any }
) {
  return request<any>("/file/test/download/", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /file/test/upload */
export async function testUploadFile(
  body: {},
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>("/file/test/upload", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
