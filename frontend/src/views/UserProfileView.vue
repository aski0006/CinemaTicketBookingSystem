<template>
  <div class="user-profile-root">
    <NavBar :is-logged-in="isLoggedIn" @show-login="showLogin = true" @show-register="showRegister = true" @logout="handleLogout" />
    <div v-if="showLogin || showRegister" class="modal-backdrop"></div>
    <LoginDialog v-model="showLogin" @close="showLogin = false" @switch="handleSwitchDialog" />
    <RegisterDialog v-model="showRegister" @close="showRegister = false" @switch="handleSwitchDialog" />

    <!-- 用户信息卡片 -->
    <div class="user-info-card" :class="memberLevelClass">
      <h2>
        个人信息
        <span v-if="user && user.status === 'ACTIVE'" class="active-dot"></span>
      </h2>
      <div v-if="user">
        <div class="avatar-row">
          <img :src="user.avatar || defaultAvatar" class="avatar-img" alt="头像" />
        </div>
        <div>
          <b>用户名：</b>{{ user.username }}
        </div>
        <div>
          <b>手机号：</b>{{ user.phone }}
        </div>
        <div>
          <b>邮箱：</b>{{ user.email }}
        </div>
        <div>
          <b>会员等级：</b>
          <span v-if="user.memberLevel === 0">普通</span>
          <span v-else-if="user.memberLevel === 1">VIP</span>
          <span v-else-if="user.memberLevel === 2">SVIP</span>
        </div>
        <div>
          <b>状态：</b>{{ user.status }}
        </div>
        <div>
          <b>注册时间：</b>{{ formatTime(user.createTime) }}
        </div>
        <el-button type="primary" size="small" @click="showEdit = true" style="margin-top: 10px;">修改信息</el-button>
      </div>
      <div v-else>加载中...</div>
    </div>

    <!-- 修改信息表单 -->
    <el-dialog v-model="showEdit" title="修改个人信息" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="头像">
          <el-input v-model="editForm.avatar" placeholder="图片URL" />
          <img v-if="editForm.avatar" :src="editForm.avatar" class="avatar-img" style="margin-top:8px;width:40px;height:40px;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 订单和场次卡片（原样） -->
    <div class="user-orders-card">
      <h2 style="display: flex; align-items: center; gap: 12px;">
        我的订单
        <el-button type="primary" size="small" @click="handleFetchOrders" :loading="ordersLoading" :disabled="ordersLoading" style="margin-left: 8px;">获取个人订单</el-button>
      </h2>
      <el-table :data="orders" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="220" />
        <el-table-column prop="movieTitle" label="影片" />
        <el-table-column label="场次时间" width="160">
          <template #default="scope">
            {{ formatTime(scope.row.sessionTime || scope.row.session_time) }}
          </template>
        </el-table-column>
        <el-table-column prop="seatCount" label="座位数" width="80" />
        <el-table-column prop="totalAmount" label="金额" width="100" />
        <el-table-column prop="status" label="状态" width="100" />
      </el-table>
    </div>
    <div class="user-sessions-card">
      <h2>可观看场次</h2>
      <el-table :data="sessions" style="width: 100%">
        <el-table-column prop="id" label="场次ID" width="80" />
        <el-table-column prop="movieTitle" label="影片" />
        <el-table-column label="开始时间" width="140">
          <template #default="scope">
            {{ formatTime(scope.row.startTime || scope.row.start_time) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="140">
          <template #default="scope">
            {{ formatTime(scope.row.endTime || scope.row.end_time) }}
          </template>
        </el-table-column>
        <el-table-column prop="hallName" label="影厅" />
        <el-table-column prop="price" label="票价" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import NavBar from '../components/NavBar.vue';
import LoginDialog from '../components/LoginDialog.vue';
import RegisterDialog from '../components/RegisterDialog.vue';
import service from '../api/request';
import { ElMessage } from 'element-plus';

const user = ref(null);
const orders = ref([]);
const sessions = ref([]);
const showLogin = ref(false);
const showRegister = ref(false);
const isLoggedIn = ref(!!localStorage.getItem('token'));
const showEdit = ref(false);
const editForm = ref({
  username: '',
  phone: '',
  email: '',
  avatar: ''
});
const defaultAvatar = '/vite.svg'; // public目录下的vite.svg
const ordersLoading = ref(false);

const memberLevelClass = computed(() => {
  if (!user.value) return '';
  switch (user.value.memberLevel) {
    case 1: return 'vip-bg';
    case 2: return 'svip-bg';
    default: return 'normal-bg';
  }
});

function handleSwitchDialog(type) {
  if (type === 'login') {
    showRegister.value = false;
    showLogin.value = true;
  } else {
    showLogin.value = false;
    showRegister.value = true;
  }
}
function handleLogout() {
  localStorage.removeItem('token');
  localStorage.removeItem('member_level');
  isLoggedIn.value = false;
  showLogin.value = false;
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

async function fetchUserInfo() {
  try {
    const res = await service.get('/api/users/me');
    // 字段兼容处理
    user.value = {
      ...res,
      memberLevel: res.memberLevel ?? res.member_level ?? 0,
      createTime: res.createTime ?? res.create_time ?? null
    };
    // 初始化编辑表单
    editForm.value = {
      username: user.value.username,
      phone: user.value.phone,
      email: user.value.email,
      avatar: user.value.avatar
    };
  } catch (e) {
    ElMessage.error('获取用户信息失败');
  }
}

async function fetchUserOrders() {
  try {
    ordersLoading.value = true;
    const res = await service.get('/api/orders/my');
    orders.value = (res || []).map(o => ({
      orderNo: o.orderNo || o.order_no,
      movieTitle: o.movieTitle || o.movie_title,
      sessionTime: o.sessionTime || o.session_time,
      seatCount: o.seatCount || o.seat_count,
      totalAmount: o.totalAmount || o.total_amount,
      status: o.status
    }));
  } catch (e) {
    ElMessage.error('获取订单信息失败');
  } finally {
    ordersLoading.value = false;
  }
}

function handleFetchOrders() {
  fetchUserOrders();
}

async function fetchUserSessions() {
  try {
    const res = await service.get('/api/users/my-today-sessions');
    sessions.value = (res || []).map(s => ({
      id: Math.abs(s.id),
      movieTitle: s.movieTitle || s.movie_title || '',
      startTime: s.startTime || s.start_time,
      endTime: s.endTime || s.end_time,
      hallName: s.hallName || s.hall_name || '',
      price: s.price
    }));
  } catch (e) {
    ElMessage.error('获取场次信息失败');
  }
}

// 提交修改
async function submitEdit() {
  try {
    const userId = user.value.id;
    const payload = {
      username: editForm.value.username,
      phone: editForm.value.phone,
      email: editForm.value.email,
      avatar: editForm.value.avatar
    };
    await service.put(`/api/users/${userId}`, payload);
    ElMessage.success('修改成功');
    showEdit.value = false;
    fetchUserInfo();
  } catch (e) {
    ElMessage.error('修改失败');
  }
}

onMounted(() => {
  fetchUserInfo();
  fetchUserSessions();
});
</script>

<style scoped>
.user-profile-root {
  max-width: 900px;
  margin: 0 auto;
  padding: 30px 0;
  display: flex;
  flex-direction: column;
  gap: 32px;
}
.user-info-card, .user-orders-card, .user-sessions-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  padding: 28px 32px 20px 32px;
}
.user-info-card h2 {
  margin-bottom: 18px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}
.active-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  background: #4caf50;
  border-radius: 50%;
  margin-left: 4px;
}
.avatar-row {
  margin-bottom: 12px;
  text-align: center;
}
.avatar-img {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
  background: #f5f5f5;
}
.normal-bg { background: #fff; }
.vip-bg { background: linear-gradient(90deg, #ffe082 0%, #ffd54f 100%); }
.svip-bg { background: linear-gradient(90deg, #b388ff 0%, #7c4dff 100%); }
</style>