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
            <div class="metrics-card" v-for="metric in metrics" :key="metric.label">
              <div class="metrics-icon" :style="{color: metric.color}"><i :class="metric.icon"></i></div>
              <div class="metrics-value">{{ metric.value }}</div>
              <div class="metrics-label">{{ metric.label }}</div>
            </div>
          </div>
          <!-- Chart Cards -->
          <div class="chart-row">
            <div class="chart-card">
              <div class="chart-header">
                <span>票房趋势</span>
                <el-button type="text" class="chart-action">查看更多<i class="el-icon-arrow-right"></i></el-button>
              </div>
              <div class="chart-body">
                <!-- 这里可集成ECharts等图表库，暂用占位 -->
                <div class="chart-placeholder">[Line Chart]</div>
              </div>
            </div>
            <div class="chart-card">
              <div class="chart-header">
                <span>类型占比</span>
                <el-button type="text" class="chart-action">查看更多<i class="el-icon-arrow-right"></i></el-button>
              </div>
              <div class="chart-body">
                <div class="chart-placeholder">[Pie Chart]</div>
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
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import AdminSidebar from '../../components/AdminSidebar.vue';
import { handleAdminSidebarNav, sidebarItems } from '@/utils/adminSidebarNav';
const topNavItems = ["仪表盘", "数据可视化", "异常页", "个人中心"];
const activeTopNav = ref(topNavItems[0]);
const activeSidebar = ref('总览');
const sidebarCollapsed = ref(false);
const router = useRouter();
const metrics = [
  { label: "今日票房", value: "¥12,800", icon: "el-icon-money", color: "#1890ff" },
  { label: "今日订单", value: "320", icon: "el-icon-tickets", color: "#FF7D00" },
  { label: "新增用户", value: "56", icon: "el-icon-user", color: "#52c41a" },
  { label: "待处理退款", value: "3", icon: "el-icon-warning", color: "#f5222d" }
];
const tableData = [
  { rank: 1, title: "封神第一部", views: 12000, growth: 320 },
  { rank: 2, title: "消失的她", views: 9800, growth: 210 },
  { rank: 3, title: "八角笼中", views: 8700, growth: 180 },
  { rank: 4, title: "孤注一掷", views: 7600, growth: 150 }
];
function handleSidebarClick(item) {
  handleAdminSidebarNav(router, item, v => activeSidebar.value = v);
}
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