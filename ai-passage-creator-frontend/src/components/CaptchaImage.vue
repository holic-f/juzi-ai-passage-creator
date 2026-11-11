<template>
  <button
      type="button"
      class="captcha-button"
      aria-label="刷新验证码"
      title="点击刷新验证码"
      @click="refresh"
  >
    <img
        :src="imageUrl"
        alt="图形验证码，点击刷新"
        class="captcha-image"
    />
  </button>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 与 src/request.ts 的后端地址保持一致
const captchaApi = 'http://localhost:8567/api/captcha/image'

// 改变 URL，让浏览器重新请求图片
const imageUrl = ref('')

const refresh = () => {
  imageUrl.value = `${captchaApi}?t=${Date.now()}`
}

// 组件加载时获取第一张图片
refresh()

// 允许父页面在提交失败后调用刷新方法
defineExpose({ refresh })
</script>

<style scoped>
.captcha-button {
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.captcha-image {
  display: block;
  width: 120px;
  height: 60px;
  object-fit: contain;
}
</style>