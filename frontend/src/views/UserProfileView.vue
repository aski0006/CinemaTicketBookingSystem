<template>
  <div class="user-profile-root">
    <NavBar :is-logged-in="isLoggedIn" @show-login="showLogin = true" @show-register="showRegister = true" @logout="handleLogout" />
    <div v-if="showLogin || showRegister" class="modal-backdrop"></div>
    <LoginDialog v-model="showLogin" @close="showLogin = false" @switch="handleSwitchDialog" />
    <RegisterDialog v-model="showRegister" @close="showRegister = false" @switch="handleSwitchDialog" />
    <div class="user-info-card">
      <h2>个人信息</h2>
      <div v-if="user">
        <div><b>用户名：</b>{{ user.username }}</div>
        <div><b>手机号：</b>{{ user.phone }}</div>
        <div><b>邮箱：</b>{{ user.email }}</div>
        <div><b>会员等级：</b>{{ user.memberLevel }}</div>
        <div><b>状态：</b>{{ user.status }}</div>
        <div><b>注册时间：</b>{{ formatTime(user.createTime) }}</div>
      </div>
      <div v-else>加载中...</div>
    </div>
    <div class="user-orders-card">
      <h2>我的订单</h2>
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
import { ref, onMounted } from 'vue';
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
  // 兼容LocalDateTime对象
  if (typeof val === 'object' && val.year && val.monthValue && val.dayOfMonth) {
    // 形如 {year:2025, monthValue:6, dayOfMonth:28, hour:18, minute:0, second:0, ...}
    const y = val.year, m = String(val.monthValue).padStart(2, '0'), d = String(val.dayOfMonth).padStart(2, '0');
    const h = String(val.hour).padStart(2, '0'), min = String(val.minute).padStart(2, '0');
    return `${y}-${m}-${d} ${h}:${min}`;
  }
  return String(val);
}

async function fetchUserInfo() {
  try {
    const res = await service.get('/api/users/me');
    user.value = res;
  } catch (e) {
    ElMessage.error('获取用户信息失败');
  }
}

async function fetchUserOrders() {
  try {
    const res = await service.get('/api/orders/my');
    orders.value = (res || []).map(o => ({
      orderNo: o.orderNo || o.order_no,
      movieTitle: o.movieTitle || o.movie_title,
      sessionTime: o.sessionTime || o.session_time,
      seatCount: o.seatCount || o.seat_count,
      totalAmount: o.totalAmount || o.total_amount,
      status: o.status
    }));
    console.log(orders.value);
  } catch (e) {
    ElMessage.error('获取订单信息失败');
  }
}

async function fetchUserSessions() {
  try {
    // 获取个人今日已购场次
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

onMounted(() => {
  fetchUserInfo();
  fetchUserOrders();
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
.user-info-card h2, .user-orders-card h2, .user-sessions-card h2 {
  margin-bottom: 18px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}
</style> 