<template>
  <div class="ticket-booking-root">
    <NavBar
      :is-logged-in="isLoggedIn"
      @show-login="showLogin = true"
      @show-register="showRegister = true"
      @logout="handleLogout"
    />
    <div v-if="showLogin || showRegister" class="modal-backdrop"></div>
    <LoginDialog v-model="showLogin" @close="showLogin = false" @switch="handleSwitchDialog" />
    <RegisterDialog v-model="showRegister" @close="showRegister = false" @switch="handleSwitchDialog" />
    
    <div class="booking-container">
      <div class="left-panel">
        <MovieDetailCard :movie="movie" @close="goBack" :close-button-text="'返回'" />
      </div>
      <div class="right-panel">
        <div class="sessions-header">
          <h2>选择场次</h2>
          <el-button @click="goBack" type="text">返回</el-button>
        </div>
        
        <div v-if="movie" class="movie-title">
          <h3>{{ movie.title }}</h3>
        </div>
        
        <div v-if="loading" class="loading-container">
          <el-loading-spinner />
          <p>加载场次信息中...</p>
        </div>
        
        <div v-else-if="sessions.length === 0" class="empty-sessions">
          <el-empty description="暂无场次信息" />
        </div>
        
        <div v-else class="sessions-list">
          <div 
            v-for="session in sessions" 
            :key="session.id" 
            class="session-item"
            :class="{ 
              'selected': selectedSession?.id === session.id,
              'disabled': session.status === 'FULL' || session.availableSeats === 0
            }"
            @click="selectSession(session)"
          >
            <div class="session-time">
              <div class="time">{{ formatTime(session.startTime) }}</div>
              <div class="duration">{{ session.movieDuration }}分钟</div>
            </div>
            <div class="session-info">
              <div class="hall-name">{{ session.hallName }}</div>
              <div class="price">¥{{ session.price }}</div>
              <div class="seats-info">剩余座位：{{ session.availableSeats }}/{{ session.totalSeats }}</div>
            </div>
            <div class="session-status">
              <el-tag 
                :type="getSessionStatusType(session.status)" 
                size="small"
              >
                {{ getSessionStatusText(session.status) }}
              </el-tag>
            </div>
          </div>
        </div>
        
      </div>
    </div>
    <el-dialog v-model="seatDialogVisible" title="选择座位" width="600px" :close-on-click-modal="false">
      <template v-if="seatDialogSession && seatDialogSession.seatStatus && seatDialogSession.seatStatus.length">
        <SeatLayoutCard
          :seat-status="seatDialogSession.seatStatus"
          :selected-seats="selectedSeats"
          @select="handleSeatSelect"
          @confirm="handleSeatConfirm"
          @close="handleSeatClose"
        />
      </template>
      <template v-else>
        <div style="text-align:center;color:#888;padding:40px 0;">暂无座位数据</div>
      </template>
    </el-dialog>
    <el-dialog v-model="showPayWaiting" title="等待支付完成" width="350px" :close-on-click-modal="false" :show-close="false">
      <div style="text-align:center;padding:24px 0;">
        <el-icon style="font-size:32px;color:#1e88e5;"><i class="el-icon-loading"></i></el-icon>
        <div style="margin-top:16px;">请在新窗口完成支付，支付成功后购票将自动完成。</div>
        <div style="margin-top:8px;color:#888;font-size:13px;">如已支付可关闭此窗口。</div>
        <el-button type="primary" style="margin-top:18px;" @click="manualCheckOrder">我已完成支付，手动确认</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import NavBar from '../components/NavBar.vue';
import LoginDialog from '../components/LoginDialog.vue';
import RegisterDialog from '../components/RegisterDialog.vue';
import MovieDetailCard from '../components/MovieDetailCard.vue';
import SeatLayoutCard from '../components/SeatLayoutCard.vue';
import { getMovieById } from '../api/movie';
import { getTodaySessions } from '../api/session';
import service from '../api/request';

const route = useRoute();
const router = useRouter();

const showLogin = ref(false);
const showRegister = ref(false);
const isLoggedIn = ref(!!localStorage.getItem('token'));
const loading = ref(true);
const movie = ref(null);
const sessions = ref([]);
const selectedSession = ref(null);
const seatDialogVisible = ref(false);
const seatDialogSession = ref(null);
const selectedSeats = ref([]);
const occupiedSeats = ref([]);
const showPayWaiting = ref(false);
const payOrderNo = ref('');

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

function goBack() {
  router.go(-1);
}

function selectSession(session) {
  if (session.status === 'FULL' || session.availableSeats === 0) {
    ElMessage.warning('该场次已满座');
    return;
  }
  selectedSession.value = session;
  // 优先使用seatStatus字段
  let seatStatus = session.seatStatus;
  if (!seatStatus && session.seatLayout) {
    try {
      seatStatus = JSON.parse(session.seatLayout);
    } catch {
      seatStatus = [];
    }
  }
  seatDialogSession.value = {
    ...session,
    seatStatus: seatStatus || []
  };
  selectedSeats.value = [];
  seatDialogVisible.value = true;
}

async function fetchOccupiedSeats(sessionId) {
  // 获取座位状态二维数组
  const res = await fetch(`/api/sessions/${sessionId}/seats`).then(r => r.json());
  const occ = [];
  res.forEach((row, i) => {
    row.forEach((cell, j) => {
      if (cell && cell.status === 'OCCUPIED') occ.push([i, j]);
    });
  });
  occupiedSeats.value = occ;
}

function handleSeatSelect(seats) {
  selectedSeats.value = seats;
}

async function handleSeatConfirm(seats) {
  if (!selectedSession.value || !seats.length) {
    ElMessage.warning('请先选择座位');
    return;
  }
  const orderRequest = {
    sessionId: selectedSession.value.id,
    seatPositions: seats,
    paymentMethod: 'ALIPAY'
  };
  try {
    const res = await service.post('/api/orders', orderRequest);
    if (res && res.payUrl) {
      // 在新标签页打开支付宝支付表单
      const newWindow = window.open('', '_blank');
      if (newWindow) {
        newWindow.document.write(res.payUrl);
        newWindow.document.close();
      } else {
        // 如果被浏览器拦截，降级为当前页跳转
        const div = document.createElement('div');
        div.innerHTML = res.payUrl;
        document.body.appendChild(div);
        div.querySelector('form')?.submit();
        setTimeout(() => document.body.removeChild(div), 3000);
      }
      // 弹出支付等待弹窗并轮询订单状态
      payOrderNo.value = res.order_no;
      showPayWaiting.value = true;
      pollOrderStatus(res.order_no);
    } else if (res.error || res.errorMessage) {
      ElMessage.error(res.error || res.errorMessage);
    } else {
      if (res && res.order_no) {
        ElMessage.success('下单成功');
        seatDialogVisible.value = false;
      } else {
        ElMessage.error('下单失败');
      }
    }
  } catch (e) {
    ElMessage.error('下单失败');
  }
}

function pollOrderStatus(orderNo) {
  let timer = setInterval(async () => {
    try {
      const res = await service.get(`/api/payments/query/${orderNo}`);
      if (res && (res.status === 'FINISH' || res.status === 'COMPLETED' || res.status === 'TRADE_SUCCESS')) {
        clearInterval(timer);
        showPayWaiting.value = false;
        ElMessage.success('支付成功，购票完成！');
        seatDialogVisible.value = false;
        // 可选：刷新订单/座位等数据
        fetchMovieData();
      } else if (res && res.status === 'FAILED') {
        clearInterval(timer);
        showPayWaiting.value = false;
        ElMessage.error('支付失败，请重新下单');
      }
    } catch (e) {
      // ignore
    }
  }, 5000);
}

function manualCheckOrder() {
  if (!payOrderNo.value) return;
  service.get(`/api/payments/query/${payOrderNo.value}`).then(res => {
    if (res && (res.status === 'FINISH' || res.status === 'COMPLETED' || res.status === 'TRADE_SUCCESS')) {
      showPayWaiting.value = false;
      ElMessage.success('支付成功，购票完成！');
      seatDialogVisible.value = false;
      fetchMovieData();
    } else {
      ElMessage.warning('暂未检测到支付完成，请稍后再试或等待自动检测。');
    }
  }).catch(() => {
    ElMessage.error('订单状态查询失败，请稍后重试');
  });
}

function handleSeatClose() {
  seatDialogVisible.value = false;
}

function formatTime(timeStr) {
  if (!timeStr) return '--';
  // 如果是LocalDateTime对象，提取时间部分
  if (typeof timeStr === 'string') {
    // 如果是ISO格式的时间字符串，提取HH:mm部分
    if (timeStr.includes('T')) {
      return timeStr.substring(11, 16); // 提取 HH:mm
    }
    return timeStr.substring(0, 5); // 只显示 HH:mm
  }
  return '--';
}

function getSessionStatusType(status) {
  switch (status) {
    case 'AVAILABLE': return 'success';
    case 'FULL': return 'warning';
    case 'CANCELLED': return 'danger';
    default: return 'info';
  }
}

function getSessionStatusText(status) {
  switch (status) {
    case 'AVAILABLE': return '可预订';
    case 'FULL': return '已满座';
    case 'CANCELLED': return '已取消';
    default: return '未知';
  }
}


async function fetchMovieData() {
  try {
    const movieId = route.params.movieId;
    if (!movieId) {
      ElMessage.error('影片ID不存在');
      goBack();
      return;
    }
    
    // 获取影片详情
    const movieData = await getMovieById(movieId);
    if (!movieData) {
      ElMessage.error('影片不存在');
      goBack();
      return;
    }
    movie.value = movieData;
    
    // 获取场次信息
    const sessionsData = await getTodaySessions(movieId);
    // 处理场次数据，适配后端返回的结构
    sessions.value = (sessionsData || []).map(session => {
      // 自动判断状态
      let status = 'AVAILABLE';
      if (session.availableSeats === 0) status = 'FULL';
      return {
        id: session.id,
        startTime: session.startTime || session.start_time,
        endTime: session.endTime || session.end_time,
        price: session.price,
        hallName: session.hall?.name || session.hallName || '',
        movieDuration: movieData.duration || 120,
        status,
        availableSeats: session.availableSeats ?? 0,
        totalSeats: session.totalSeats ?? 0,
        seatLayout: session.seatLayout || ''
      };
    });
    
  } catch (error) {
    console.error('获取数据失败:', error);
    ElMessage.error('获取数据失败，请稍后重试');
    goBack();
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchMovieData();
});
</script>

<style scoped>
.ticket-booking-root {
  min-height: 100vh;
  background: #f5f7fa;
}

.booking-container {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  gap: 30px;
  min-height: calc(100vh - 80px);
}

.left-panel {
  flex: 1;
  max-width: 500px;
  display: flex;
  align-items: flex-start;
}

.left-panel .movie-detail-card {
  margin: 0;
  max-width: none;
  width: 100%;
}

.right-panel {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.sessions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.sessions-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.movie-title {
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.movie-title h3 {
  margin: 0;
  color: #333;
  font-size: 20px;
  font-weight: 600;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #666;
}

.empty-sessions {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.sessions-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 30px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fafafa;
}

.session-item:hover {
  border-color: #1890ff;
  background: #f0f8ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
}

.session-item.selected {
  border-color: #1890ff;
  background: #e6f7ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.session-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f5f5;
}

.session-item.disabled:hover {
  border-color: #f0f0f0;
  background: #f5f5f5;
  transform: none;
  box-shadow: none;
}

.session-time {
  flex: 1;
  text-align: center;
}

.time {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.duration {
  font-size: 12px;
  color: #666;
}

.session-info {
  flex: 2;
  text-align: center;
}

.hall-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
}

.price {
  font-size: 18px;
  font-weight: 600;
  color: #ff4d4f;
}

.seats-info {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.session-status {
  flex: 1;
  text-align: center;
}

.booking-actions {
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 2px solid #f0f0f0;
}

.booking-actions .el-button {
  padding: 12px 40px;
  font-size: 16px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .booking-container {
    flex-direction: column;
    padding: 10px;
  }
  
  .left-panel {
    max-width: none;
  }
  
  .right-panel {
    padding: 20px;
  }
  
  .session-item {
    flex-direction: column;
    gap: 10px;
    text-align: center;
  }
  
  .session-time,
  .session-info,
  .session-status {
    flex: none;
  }
}
</style> 