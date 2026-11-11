import request from '@/request'

export function uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)

    return request<API.BaseResponseString>('/user/avatar', {
        method: 'POST',
        data: formData,
    })
}