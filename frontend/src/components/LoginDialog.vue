<template>
  <el-dialog
    v-model="visible"
    title="登录"
    width="400px"
    :close-on-click-modal="false"
    :show-close="true"
    class="custom-dialog"
    @closed="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
      <el-form-item prop="identifier">
        <el-input
          v-model="form.identifier"
          placeholder="用户名/手机/邮箱"
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
      <el-form-item>
        <el-button
          type="primary"
          size="large"
          style="width: 100%"
          :loading="loading"
          @click="onSubmit"
        >登录</el-button>
      </el-form-item>
      <el-form-item>
        <div class="switch-tip">
          没有账号？
          <el-link type="primary" @click="switchToRegister">去注册</el-link>
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
import { ref, watch, defineEmits, defineProps } from 'vue'
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
  identifier: '',
  password: ''
})
const rules = {
  identifier: [
    { required: true, message: '请输入用户名/手机/邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}
const loading = ref(false)
const errorMsg = ref('')

function onSubmit() {
  errorMsg.value = ''
  formRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    console.log('[Login] 提交表单:', form.value)
    try {
      const data = await service.post('/api/users/login', {
        identifier: form.value.identifier,
        password: form.value.password
      })
      console.log('[Login] 登录响应:', data)
      if (!data.token) {
        // 只要没有token就认为是失败
        throw { response: { status: data.status || 401, data } }
      }
      localStorage.setItem('token', data.token)
      ElMessage.success('登录成功')
      visible.value = false
      emit('close')
    } catch (e) {
      console.error('[Login] 登录失败:', e)
      if (e.response) {
        console.error('[Login] 错误响应内容:', e.response)
      }
      if (e.response && e.response.status === 401) {
        errorMsg.value = e.response.data.error || '账号或密码错误'
      } else {
        errorMsg.value = '登录失败，请重试'
      }
    } finally {
      loading.value = false
    }
  })
}
function handleClose() {
  console.log('[Login] 关闭弹窗')
  emit('close')
  errorMsg.value = ''
  form.value.identifier = ''
  form.value.password = ''
}
function switchToRegister() {
  console.log('[Login] 切换到注册弹窗')
  visible.value = false
  emit('switch', 'register')
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
</style> 