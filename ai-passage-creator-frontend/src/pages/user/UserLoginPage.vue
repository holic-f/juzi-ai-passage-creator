<template>
  <div id="userLoginPage">
    <h2 class="title">用户登录</h2>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
          name="userPassword"
          :rules="[
          { required: true, message: '请输入密码' },
          { min: 6, message: '密码长度不能小于 6 位' },
        ]"
      >
      <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      <div style="height: 16px;"/>
      <a-form-item name="captchaCode" :rules="[{ required: true, message: '请输入验证码' }]">
        <div class="captcha-row">
          <a-input v-model:value="formState.captchaCode" placeholder="请输入验证码"/>
          <CaptchaImage ref="captchaImageRef" />
        </div>
      </a-form-item>
      </a-form-item>
      <div class="tips">
        没有账号
        <RouterLink to="/user/register">去注册</RouterLink>
      </div>

      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { reactive, ref} from 'vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import CaptchaImage from "@/components/CaptchaImage.vue";


const captchaImageRef = ref<InstanceType<typeof CaptchaImage> | null>(null)

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
  captchaCode: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserLoginRequest) => {
  try {
    const res = await userLogin(values)
    // 登录成功，把登录态保存到全局状态中
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      router.push({
        path: '/',
        replace: true,
      })
    } else {
      message.error('登录失败，' + res.data.message)
      refreshCaptcha()
    }
  }catch {
    message.error('登录失败，请检查账号密码和验证码')
    refreshCaptcha()
  }
}

//登录失败后刷新验证码
const refreshCaptcha = () => {
  formState.captchaCode = ''
  captchaImageRef.value?.refresh()
}
</script>

<style scoped>
#userLoginPage {
  background: white;
  max-width: 720px;
  padding: 24px;
  margin: 24px auto;
  border-radius: 8px;
}

.title {
  text-align: center;
  margin-bottom: 24px;
}

.tips {
  text-align: right;
  color: #bbb;
  font-size: 13px;
  margin-bottom: 16px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>