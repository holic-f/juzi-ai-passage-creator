<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <img class="logo" src="@/assets/ai-writer-orange.ico" alt="Logo" />
            <h1 class="site-title">项目名称</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
            v-model:selectedKeys="selectedKeys"
            mode="horizontal"
            :items="menuItems"
            @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="avatarUrl" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item :disabled="avatarUploading" @click="openAvatarPicker">
                    更换头像
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <input
                ref="avatarInput"
                type="file"
                accept="image/jpeg,image/png"
                style="display: none"
                @change="handleAvatarChange"
            />
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { LogoutOutlined, HomeOutlined } from '@ant-design/icons-vue'
import { uploadAvatar } from '@/api/avatar'

const loginUserStore = useLoginUserStore()
const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

const avatarInput = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)
const avatarPreview = ref('')
const avatarPreviewKey = ref('')
const avatarPreviewUser = ref<API.LoginUserVO['id']>()

// 新头像保存的是对象键；旧数据可能已经是完整 URL。
const avatarUrl = computed(() => {
  const avatar = loginUserStore.loginUser.userAvatar
  if (avatarPreview.value && avatar === avatarPreviewKey.value &&
      loginUserStore.loginUser.id === avatarPreviewUser.value) return avatarPreview.value
  if (!avatar) return undefined
  if (/^https?:\/\//i.test(avatar)) return avatar
  const baseUrl = import.meta.env.VITE_AVATAR_BASE_URL
  if (!baseUrl || !avatar.startsWith('avatar/')) return undefined
  return `${baseUrl.replace(/\/$/, '')}/${avatar.split('/').map(encodeURIComponent).join('/')}`
})

onUnmounted(() => {
  if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value)
})

const openAvatarPicker = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  // 用户取消选择时，不发送请求
  if (!file) {
    return
  }

  if (avatarUploading.value) return

  try {
    // 前端先给出提示；后端仍要再次校验
    if (file.size > 2 * 1024 * 1024) {
      message.error('图片不能超过 2 MB')
      return
    }

    avatarUploading.value = true
    const userId = loginUserStore.loginUser.id
    const res = await uploadAvatar(file)

    if (res.data.code !== 0) {
      message.error('上传失败，' + (res.data.message ?? '请重试'))
      return
    }

    if (res.data.data && loginUserStore.loginUser.id === userId) {
      const preview = URL.createObjectURL(file)
      if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value)
      avatarPreview.value = preview
      avatarPreviewKey.value = res.data.data
      avatarPreviewUser.value = userId
      loginUserStore.setLoginUser({ ...loginUserStore.loginUser, userAvatar: res.data.data })
    }
    message.success('头像上传成功')
    try {
      await loginUserStore.fetchLoginUser()
    } catch {
      message.warning('头像已上传，用户信息刷新失败')
    }
  } catch {
    message.error('上传请求失败，请重试')
  } finally {
    avatarUploading.value = false
    // 清空后，即使再次选择同一文件也能触发 change
    input.value = ''
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 48px;
  width: 48px;
}

.site-title {
  margin: 0;
  font-size: 18px;
  color: #1890ff;
}

.ant-menu-horizontal {
  border-bottom: none !important;
}
</style>
