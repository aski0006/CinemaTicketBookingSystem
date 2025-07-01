<template>
  <div class="arco-dashboard">
    <!-- Top Navigation Bar -->
    <header class="top-nav">
      <div class="nav-logo">Arco Pro Dashboard</div>
      <nav class="nav-menu">
        <span v-for="item in topNavItems" :key="item" :class="['nav-item', {active: item === activeTopNav}]" @click="activeTopNav = item">{{ item }}</span>
      </nav>
      <div class="nav-user">Admin</div>
    </header>
    <div class="dashboard-body">
      <!-- Left Sidebar -->
      <AdminSidebar
        :items="sidebarItems"
        :activeSidebar="activeSidebar"
        :collapsed="sidebarCollapsed"
        @toggle-collapse="sidebarCollapsed = !sidebarCollapsed"
        @sidebar-click="handleSidebarClick"
      />
      <!-- Main Content Grid -->
      <main class="main-content">
        <div class="dashboard-grid">
          <!-- Metrics Cards -->
          <div class="metrics-row">
            <div class="metrics-card">
              <div class="metrics-icon" style="color:#52c41a"><i class="el-icon-user"></i></div>
              <div class="metrics-value">{{ userCount }}</div>
              <div class="metrics-label">用户总数</div>
            </div>
            <div class="metrics-card">
              <div class="metrics-icon" style="color:#1890ff"><i class="el-icon-money"></i></div>
              <div class="metrics-value">¥{{ orderAmount }}</div>
              <div class="metrics-label">总订单金额</div>
            </div>
            <div class="metrics-card">
              <div class="metrics-icon" style="color:#FF7D00"><i class="el-icon-tickets"></i></div>
              <div class="metrics-value">{{ orderCount }}</div>
              <div class="metrics-label">订单总数</div>
            </div>
            <div class="metrics-card">
              <div class="metrics-icon" style="color:#f5222d"><i class="el-icon-warning"></i></div>
              <div class="metrics-value">{{ refundedCount }}</div>
              <div class="metrics-label">已退款订单</div>
            </div>
          </div>
          <!-- Chart Cards -->
          <div class="chart-row">
            <div class="chart-card">
              <div class="chart-header">
                <span>用户与订单趋势</span>
              </div>
              <div class="chart-body">
                <div id="dashboard-main-chart" style="height:320px;width:100%"></div>
              </div>
            </div>
            <div class="chart-card">
              <div class="chart-header">
                <span>订单类型占比</span>
              </div>
              <div class="chart-body">
                <div id="dashboard-pie-chart" style="height:320px;width:100%"></div>
              </div>
            </div>
          </div>
          <!-- Table Card -->
          <div class="table-card">
            <div class="table-header">热度排行</div>
            <el-table :data="tableData" size="small" stripe border>
              <el-table-column prop="rank" label="排名" width="60" />
              <el-table-column prop="title" label="内容标题" />
              <el-table-column prop="views" label="点击量" width="100" />
              <el-table-column prop="growth" label="日涨量" width="100" />
            </el-table>
          </div>
        </div>
      </main>
      <!-- Right Utility Panel -->
      <aside class="right-panel">
        
        <div class="right-panel-section">
          <div class="section-header">系统状态</div>
          <div class="status-item"><span class="dot online"></span>服务正常</div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import AdminSidebar from '../../components/AdminSidebar.vue';
import { handleAdminSidebarNav, sidebarItems } from '@/utils/adminSidebarNav';
import * as echarts from 'echarts';
import service from '../../api/request';

const topNavItems = ["仪表盘"];
const activeTopNav = ref(topNavItems[0]);
const activeSidebar = ref('总览');
const sidebarCollapsed = ref(false);
const router = useRouter();

const userCount = ref(0);
const orderCount = ref(0);
const orderAmount = ref(0);
const refundedCount = ref(0);
const orderTypePie = ref({ ticket: 0, membership: 0 });
const trendDays = ref([]);
const userTrend = ref([]);
const orderTrend = ref([]);
const tableData = ref([]);

function handleSidebarClick(item) {
  handleAdminSidebarNav(router, item, v => activeSidebar.value = v);
}

async function fetchDashboardStats() {
  // 获取用户数
  const userRes = await service.get('/admin/users', { params: { page: 1, size: 1 } });
  userCount.value = Number(userRes.total) || (userRes.users || userRes.content || []).length;
  // 获取购票订单统计
  const orderRes = await service.get('/admin/orders');
  const ticketOrders = (orderRes.orders || []);
  orderCount.value = ticketOrders.length;
  orderAmount.value = ticketOrders.reduce((sum, o) => sum + (o.totalAmount || o.total_amount || 0), 0);
  refundedCount.value = ticketOrders.filter(o => o.status === 'REFUNDED').length;
  orderTypePie.value.ticket = ticketOrders.length;
  // 获取会员订单（修正：统一用管理员专用接口，兼容新字段）
  const memberRes = await service.get('/api/memberships/admin/all', { params: { page: 0, size: 1000 } });
  const memberOrders = (memberRes.content || []).map(o => ({
    ...o,
    id: o.id || o.orderId,
    userId: o.userId,
    amount: o.amount || o.totalAmount,
    paymentTime: o.paymentTime || o.payment_time,
    status: o.status
  }));
  orderTypePie.value.membership = memberOrders.length;
  orderCount.value += memberOrders.length;
  orderAmount.value += memberOrders.reduce((sum, o) => sum + (o.amount || 0), 0);
  refundedCount.value += memberOrders.filter(o => o.status === 'REFUNDED').length;
  // 统计趋势
  const days = Array.from({length: 30}, (_, i) => {
    const d = new Date(); d.setDate(d.getDate() - (29-i));
    return d.toISOString().slice(0,10);
  });
  trendDays.value = days;
  userTrend.value = Array(30).fill(userCount.value); // 简单处理，实际可按天统计注册数
  const ticketTrend = days.map(day => ticketOrders.filter(o => (o.createTime||o.create_time||'').slice(0,10) === day).length);
  const memberTrend = days.map(day => memberOrders.filter(o => (o.paymentTime||'').slice(0,10) === day).length);
  orderTrend.value = ticketTrend.map((v,i) => v + memberTrend[i]);
  // 获取热度排行
  try {
    const hotRes = await service.get('/admin/movies/hot');
    tableData.value = (hotRes || []).map(item => ({
      rank: item.rank,
      title: item.title,
      views: item.views,
      growth: '' // 暂无日涨量数据
    }));
  } catch (e) {
    tableData.value = [];
  }
  setTimeout(drawCharts, 200);
}

function drawCharts() {
  // 主趋势图
  const mainChart = echarts.init(document.getElementById('dashboard-main-chart'));
  mainChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['用户数','订单数'] },
    xAxis: { type: 'category', data: trendDays.value },
    yAxis: { type: 'value' },
    series: [
      { name: '用户数', type: 'line', data: userTrend.value },
      { name: '订单数', type: 'bar', data: orderTrend.value }
    ]
  });
  // 饼图
  const pieChart = echarts.init(document.getElementById('dashboard-pie-chart'));
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { top: '5%', left: 'center' },
    series: [
      {
        name: '订单类型',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, position: 'outside' },
        emphasis: { label: { show: true, fontSize: 18, fontWeight: 'bold' } },
        labelLine: { show: true },
        data: [
          { value: orderTypePie.value.ticket, name: '购票订单' },
          { value: orderTypePie.value.membership, name: '会员订单' }
        ]
      }
    ]
  });
}

onMounted(() => {
  fetchDashboardStats();
  // 监听用户管理页面的用户数变更
  window.addEventListener('user-count-update', e => {
    userCount.value = e.detail.total;
    drawCharts();
  });
});
</script>

<style scoped>
.arco-dashboard {
  font-family: 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  background: #F5F7FA;
  min-height: 100vh;
  color: #1D2129;
}
.top-nav {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom: 1px solid #e5e6eb;
}
.nav-logo {
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
}
.nav-menu {
  display: flex;
  gap: 32px;
}
.nav-item {
  font-size: 15px;
  color: #1D2129;
  cursor: pointer;
  padding: 0 8px;
  position: relative;
}
.nav-item.active {
  color: #1890ff;
  font-weight: 600;
}
.nav-item.active::after {
  content: '';
  display: block;
  height: 2px;
  background: #1890ff;
  border-radius: 2px;
  width: 60%;
  margin: 0 auto;
  margin-top: 4px;
}
.nav-user {
  color: #86909C;
  font-size: 14px;
}
.dashboard-body {
  display: flex;
  min-height: calc(100vh - 56px);
}
.sidebar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e5e6eb;
  transition: width 0.2s;
  padding-top: 16px;
  position: relative;
}
.sidebar.collapsed {
  width: 56px;
}
.sidebar-toggle {
  position: absolute;
  top: 8px;
  right: -18px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
}
.sidebar-menu {
  list-style: none;
  padding: 0;
  margin: 0;
}
.sidebar-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  font-size: 15px;
  color: #1D2129;
  cursor: pointer;
  border-radius: 6px;
  margin: 4px 8px;
  transition: background 0.2s, color 0.2s;
}
.sidebar-menu-item.active, .sidebar-menu-item:hover {
  background: #e8f3ff;
  color: #1890ff;
}
.sidebar.collapsed .sidebar-menu-item span {
  display: none;
}
.main-content {
  flex: 1;
  padding: 32px 24px 24px 24px;
}
.dashboard-grid {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.metrics-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.metrics-card {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.metrics-icon {
  font-size: 24px;
  margin-bottom: 4px;
}
.metrics-value {
  font-size: 24px;
  font-weight: 600;
}
.metrics-label {
  font-size: 14px;
  color: #86909C;
}
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.chart-card {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
}
.chart-action {
  color: #FF7D00;
  font-weight: 500;
  font-size: 14px;
}
.chart-body {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chart-placeholder {
  color: #86909C;
  font-size: 16px;
  font-style: italic;
}
.table-card {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 16px 24px;
}
.table-header {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}
.right-panel {
  width: 240px;
  background: #F5F7FA;
  border-left: 1px solid #e5e6eb;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.right-panel-section {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 16px 16px 12px 16px;
  margin-bottom: 8px;
}
.section-header {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
}
.ghost-btn {
  color: #FF7D00;
  background: none;
  border: none;
  font-weight: 500;
  font-size: 15px;
  margin-bottom: 8px;
}
.status-item {
  font-size: 14px;
  color: #52c41a;
  display: flex;
  align-items: center;
  gap: 8px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.dot.online {
  background: #52c41a;
}
</style> 