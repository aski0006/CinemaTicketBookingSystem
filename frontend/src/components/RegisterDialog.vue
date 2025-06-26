<template>
  <el-dialog
    v-model="visible"
    title="注册"
    width="400px"
    :close-on-click-modal="false"
    :show-close="true"
    class="custom-dialog"
    @closed="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
      <el-form-item prop="username">
        <el-input
          v-model="form.username"
          placeholder="用户名"
          size="large"
          clearable
        />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          placeholder="密码"
          type="password"
          size="large"
          show-password
          clearable
        />
      </el-form-item>
      <el-form-item prop="phone">
        <el-input
          v-model="form.phone"
          placeholder="手机号"
          size="large"
          clearable
        />
        <el-button size="small" style="margin-top:6px;" @click="generatePhone">自动生成手机号</el-button>
        <div v-if="form.phone" :style="{color: phoneLengthColor, fontSize: '12px', marginTop: '2px'}">
          已输入 {{ form.phone.length }}/11 位
        </div>
      </el-form-item>
      <el-form-item prop="email">
        <el-input
          v-model="form.email"
          placeholder="邮箱"
          size="large"
          clearable
          @input="onEmailInput"
          @focus="onEmailFocus"
          @blur="onEmailBlur"
          ref="emailInputRef"
        />
        <ul v-if="showEmailSuggestions && filteredEmailSuggestions.length" class="email-suggestion-list">
          <li v-for="item in filteredEmailSuggestions" :key="item" @mousedown.prevent="selectEmailSuggestion(item)">{{ item }}</li>
        </ul>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          size="large"
          style="width: 100%"
          :loading="loading"
          @click="onSubmit"
        >注册</el-button>
      </el-form-item>
      <el-form-item>
        <div class="switch-tip">
          已有账号？
          <el-link type="primary" @click="switchToLogin">去登录</el-link>
        </div>
      </el-form-item>
    </el-form>
    <el-alert
      v-if="errorMsg"
      :title="errorMsg"
      type="error"
      show-icon
      class="error-alert"
      :closable="false"
    />
  </el-dialog>
</template>

<script setup>
import { ref, watch, defineEmits, defineProps, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import service from '@/api/request'

const props = defineProps({
  modelValue: Boolean
})
const emit = defineEmits(['update:modelValue', 'close', 'switch'])

const visible = ref(props.modelValue)
watch(() => props.modelValue, v => visible.value = v)
watch(visible, v => emit('update:modelValue', v))

const formRef = ref()
const form = ref({
  username: '',
  password: '',
  phone: '',
  email: ''
})
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}
const loading = ref(false)
const errorMsg = ref('')

const emailInputRef = ref()
const showEmailSuggestions = ref(false)
const emailSuggestions = [
  '@qq.com',
  '@163.com',
  '@gmail.com',
  '@outlook.com',
  '@126.com',
  '@sina.com',
  '@hotmail.com',
  '@foxmail.com'
]
const filteredEmailSuggestions = ref([])

function onEmailInput(val) {
  const atIdx = val.indexOf('@')
  if (atIdx === -1) {
    showEmailSuggestions.value = false
    filteredEmailSuggestions.value = []
    return
  }
  const prefix = val.slice(0, atIdx)
  const suffix = val.slice(atIdx)
  filteredEmailSuggestions.value = emailSuggestions
    .filter(s => s.startsWith(suffix) && s !== suffix)
    .map(s => prefix + s)
  showEmailSuggestions.value = filteredEmailSuggestions.value.length > 0
}
function selectEmailSuggestion(suggestion) {
  form.value.email = suggestion
  showEmailSuggestions.value = false
  nextTick(() => {
    emailInputRef.value.blur()
    emailInputRef.value.focus()
  })
}
function onEmailFocus() {
  if (form.value.email.includes('@')) {
    onEmailInput(form.value.email)
  }
}
function onEmailBlur() {
  setTimeout(() => { showEmailSuggestions.value = false }, 100)
}
const phoneLengthColor = computed(() => {
  if (form.value.phone.length === 11) return '#67c23a' // green
  if (form.value.phone.length > 11) return '#f56c6c' // red
  return '#909399' // gray
})

function onSubmit() {
  errorMsg.value = ''
  formRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    console.log('[Register] 提交表单:', form.value)
    try {
      const data = await service.post('/api/users/register', {
        username: form.value.username,
        password: form.value.password,
        phone: form.value.phone,
        email: form.value.email
      })
      console.log('[Register] 注册响应:', data)
      if (!data.id) {
        // 只要没有id就认为是失败
        throw { response: { status: data.status || 409, data } }
      }
      ElMessage.success('注册成功，请登录')
      visible.value = false
      emit('switch', 'login')
    } catch (e) {
      console.error('[Register] 注册失败:', e)
      if (e.response) {
        console.error('[Register] 错误响应内容:', e.response)
      }
      if (e.response && e.response.status === 409) {
        errorMsg.value = e.response.data.error || '用户名、手机号或邮箱已被注册'
      } else if (e.response && e.response.status === 400) {
        errorMsg.value = '注册信息不合法'
      } else {
        errorMsg.value = '注册失败，请重试'
      }
    } finally {
      loading.value = false
    }
  })
}
function handleClose() {
  console.log('[Register] 关闭弹窗')
  emit('close')
  errorMsg.value = ''
  form.value.username = ''
  form.value.password = ''
  form.value.phone = ''
  form.value.email = ''
}
function switchToLogin() {
  console.log('[Register] 切换到登录弹窗')
  visible.value = false
  emit('switch', 'login')
}
function generatePhone() {
  // 中国大陆手机号：1[3-9]开头，11位
  const prefix = '1' + Math.floor(Math.random() * 7 + 3);
  let phone = prefix;
  for (let i = 0; i < 9; i++) {
    phone += Math.floor(Math.random() * 10);
  }
  form.value.phone = phone;
}
</script>

<style scoped>
.custom-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}
.switch-tip {
  text-align: right;
  width: 100%;
  font-size: 13px;
}
.error-alert {
  margin-top: 10px;
}
.email-suggestion-list {
  list-style: none;
  margin: 0;
  padding: 0 0 0 4px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  position: absolute;
  z-index: 10;
  width: 95%;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.email-suggestion-list li {
  padding: 4px 8px;
  cursor: pointer;
  font-size: 13px;
}
.email-suggestion-list li:hover {
  background: #f5f7fa;
}
</style> 