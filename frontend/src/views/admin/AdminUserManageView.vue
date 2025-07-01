<template>
  <div class="admin-layout">
    <AdminSidebar
      :items="sidebarItems"
      :activeSidebar="activeSidebar"
      :collapsed="sidebarCollapsed"
      @toggle-collapse="sidebarCollapsed = !sidebarCollapsed"
      @sidebar-click="handleSidebarClick"
    />
    <div class="admin-main">
      <el-card class="main-card" shadow="never">
        <div class="page-header">
          <span class="page-title">用户管理</span>
          <el-input v-model="searchKeyword" placeholder="搜索用户名/手机号/邮箱" class="search-input" clearable @input="fetchUsers(1)" />
          <el-button type="primary" @click="fetchUsers(1)">搜索</el-button>
          <el-button @click="exportUsers" style="margin-left:8px;">导出</el-button>
        </div>
        <el-table :data="users" border stripe size="small" class="user-table" @selection-change="handleSelectionChange" ref="userTable">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="phone" label="手机号" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="memberLevel" label="会员等级" />
          <el-table-column prop="status" label="状态">
            <template #default="scope">
              <el-tag v-if="scope.row.status==='ACTIVE'" type="success">ACTIVE</el-tag>
              <el-tag v-else type="danger">LOCKED</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间">
            <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="320">
            <template #default="scope">
              <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteUser(scope.row.id)">删除</el-button>
              <el-button size="small" type="warning" v-if="scope.row.status==='ACTIVE'" @click="lockUser(scope.row.id)">锁定</el-button>
              <el-button size="small" type="success" v-if="scope.row.status==='LOCKED'" @click="unlockUser(scope.row.id)">解锁</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="batch-bar">
          <el-button size="small" type="danger" :disabled="!selectedIds.length" @click="batchDelete">批量删除</el-button>
          <el-button size="small" type="warning" :disabled="!selectedIds.length" @click="batchLock">批量锁定</el-button>
          <el-button size="small" type="success" :disabled="!selectedIds.length" @click="batchUnlock">批量解锁</el-button>
        </div>
        <div class="pagination-bar">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="fetchUsers"
          />
        </div>
        <!-- 编辑用户弹窗 -->
        <el-dialog v-model="showEditDialog" title="编辑用户" width="400px">
          <el-form :model="editForm" label-width="80px" ref="editFormRef">
            <el-form-item label="用户名">
              <el-input v-model="editForm.username" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" />
            </el-form-item>
            <el-form-item label="会员等级">
              <el-select v-model="editForm.memberLevel">
                <el-option label="普通" :value="0" />
                <el-option label="VIP" :value="1" />
                <el-option label="SVIP" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="editForm.status">
                <el-option label="ACTIVE" value="ACTIVE" />
                <el-option label="LOCKED" value="LOCKED" />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showEditDialog = false">取消</el-button>
            <el-button type="primary" @click="submitEdit">保存</el-button>
          </template>
        </el-dialog>
      </el-card>
      <el-card class="footer-card" shadow="never">
        <div class="footer-text">© 2024 Cinema Admin 后台管理系统</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import AdminSidebar from '../../components/AdminSidebar.vue';
import { useRouter } from 'vue-router';
import { handleAdminSidebarNav, sidebarItems } from '@/utils/adminSidebarNav';
import service from '@/api/request';

const activeSidebar = ref('用户管理');
const sidebarCollapsed = ref(false);
const router = useRouter();

const users = ref([]);
const total = ref(0);
const pageSize = ref(10);
const currentPage = ref(1);
const searchKeyword = ref('');
const selectedIds = ref([]);
const userTable = ref();

const showEditDialog = ref(false);
const editForm = reactive({ id: null, username: '', phone: '', email: '', memberLevel: 0, status: 'ACTIVE' });
const editFormRef = ref();

function handleSidebarClick(item) {
  handleAdminSidebarNav(router, item, v => activeSidebar.value = v);
}

function formatTime(val) {
  if (!val) return '--';
  if (typeof val === 'string' && val.includes('T')) {
    return val.replace('T', ' ').substring(0, 16);
  }
  if (typeof val === 'object' && val.year && val.monthValue && val.dayOfMonth) {
    const y = val.year, m = String(val.monthValue).padStart(2, '0'), d = String(val.dayOfMonth).padStart(2, '0');
    const h = String(val.hour).padStart(2, '0'), min = String(val.minute).padStart(2, '0');
    return `${y}-${m}-${d} ${h}:${min}`;
  }
  return String(val);
}

async function fetchUsers(page = 1) {
  currentPage.value = page;
  const params = { page, size: pageSize.value };
  if (searchKeyword.value) params.keyword = searchKeyword.value;
  const res = await service.get('/admin/users', { params });
  users.value = (res.users || res.content || []).map(u => ({
    ...u,
    memberLevel: u.memberLevel ?? u.member_level ?? 0,
    createTime: u.createTime ?? u.create_time ?? ''
  }));
  total.value = Number(res.total) || users.value.length;
  // 联动Dashboard统计
  window.dispatchEvent(new CustomEvent('user-count-update', { detail: { total: total.value } }));
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id);
}

function openEditDialog(row) {
  Object.assign(editForm, row);
  showEditDialog.value = true;
}

async function submitEdit() {
  await service.put(`/admin/users/${editForm.id}`, editForm);
  ElMessage.success('修改成功');
  showEditDialog.value = false;
  fetchUsers(currentPage.value);
}

async function deleteUser(id) {
  await service.delete(`/admin/users/${id}`);
  ElMessage.success('删除成功');
  fetchUsers(currentPage.value);
}

async function lockUser(id) {
  await service.post(`/admin/users/${id}/lock`);
  ElMessage.success('已锁定');
  fetchUsers(currentPage.value);
}

async function unlockUser(id) {
  await service.post(`/admin/users/${id}/unlock`);
  ElMessage.success('已解锁');
  fetchUsers(currentPage.value);
}

async function batchDelete() {
  await Promise.all(selectedIds.value.map(id => service.delete(`/admin/users/${id}`)));
  ElMessage.success('批量删除成功');
  fetchUsers(currentPage.value);
}
async function batchLock() {
  await Promise.all(selectedIds.value.map(id => service.post(`/admin/users/${id}/lock`)));
  ElMessage.success('批量锁定成功');
  fetchUsers(currentPage.value);
}
async function batchUnlock() {
  await Promise.all(selectedIds.value.map(id => service.post(`/admin/users/${id}/unlock`)));
  ElMessage.success('批量解锁成功');
  fetchUsers(currentPage.value);
}

function exportUsers() {
  const rows = [
    ['ID','用户名','手机号','邮箱','会员等级','状态','注册时间'],
    ...users.value.map(u => [u.id, u.username, u.phone, u.email, u.memberLevel, u.status, formatTime(u.createTime)])
  ];
  const csv = rows.map(r => r.join(',')).join('\n');
  const blob = new Blob([csv], {type: 'text/csv'});
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'users.csv';
  a.click();
  URL.revokeObjectURL(url);
}

onMounted(() => {
  fetchUsers();
});
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #F5F7FA;
}
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.main-card {
  margin: 32px 24px 0 24px;
  border-radius: 8px;
  background: #fff;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.footer-card {
  margin: 16px 24px 24px 24px;
  background: #f5f7fa;
  border-radius: 8px;
  text-align: center;
  color: #86909C;
  font-size: 14px;
  box-shadow: none;
}
.footer-text {
  padding: 12px 0;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
}
.user-table {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  margin-bottom: 24px;
}
.batch-bar {
  margin: 12px 0 0 0;
  display: flex;
  gap: 12px;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 24px;
}
.search-input {
  width: 220px;
  margin: 0 12px;
}
</style> 