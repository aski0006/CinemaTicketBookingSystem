<template>
  <div class="system-settings-layout">
    <AdminSidebar
      :items="sidebarItems"
      :activeSidebar="activeSidebar"
      :collapsed="sidebarCollapsed"
      @toggle-collapse="sidebarCollapsed = !sidebarCollapsed"
      @sidebar-click="handleSidebarClick"
    />
    <div class="settings-root">
      <header class="settings-header">
        <h2>系统设置</h2>
      </header>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="settings-form">
        <el-form-item label="页面标题" prop="pageTitle">
          <el-input v-model="form.pageTitle" placeholder="请输入页面标题" />
        </el-form-item>
        <el-form-item label="布局风格" prop="layout">
          <el-select v-model="form.layout" placeholder="请选择布局">
            <el-option label="经典布局" value="classic" />
            <el-option label="紧凑布局" value="compact" />
            <el-option label="宽屏布局" value="wide" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSubmit">保存设置</el-button>
          <el-button @click="onReset" style="margin-left:8px;">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import AdminSidebar from '../../components/AdminSidebar.vue';
import { sidebarItems, handleAdminSidebarNav } from '@/utils/adminSidebarNav';
import { useRouter } from 'vue-router';

const activeSidebar = ref('系统设置');
const sidebarCollapsed = ref(false);
const router = useRouter();
const formRef = ref();

const form = reactive({
  pageTitle: '',
  layout: ''
});

const rules = {
  pageTitle: [
    { required: true, message: '页面标题不能为空', trigger: 'blur' },
    { min: 2, max: 32, message: '标题长度2-32字符', trigger: 'blur' }
  ],
  layout: [
    { required: true, message: '请选择布局风格', trigger: 'change' }
  ]
};

function handleSidebarClick(item) {
  handleAdminSidebarNav(router, item, v => activeSidebar.value = v);
}

function loadSettings() {
  const saved = localStorage.getItem('systemSettings');
  if (saved) {
    try {
      const data = JSON.parse(saved);
      form.pageTitle = data.pageTitle || '';
      form.layout = data.layout || '';
    } catch {}
  }
}

function saveSettings() {
  localStorage.setItem('systemSettings', JSON.stringify({
    pageTitle: form.pageTitle,
    layout: form.layout
  }));
}

function onSubmit() {
  formRef.value.validate(valid => {
    if (valid) {
      saveSettings();
      ElMessage.success('设置已保存！');
    } else {
      ElMessage.error('请检查表单输入');
    }
  });
}

function onReset() {
  form.pageTitle = '';
  form.layout = '';
  saveSettings();
  ElMessage.info('设置已重置');
}

onMounted(() => {
  loadSettings();
});
</script>

<style scoped>
.system-settings-layout {
  display: flex;
  min-height: 100vh;
  background: #F5F7FA;
}
.settings-root {
  flex: 1;
  max-width: 600px;
  margin: 0 auto;
  padding: 48px 0 32px 32px;
}
.settings-header {
  margin-bottom: 24px;
}
.settings-form {
  background: #fff;
  border-radius: 8px;
  padding: 32px 32px 16px 32px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
</style> 