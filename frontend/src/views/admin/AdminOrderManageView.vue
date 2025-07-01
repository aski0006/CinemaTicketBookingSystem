<template>
  <div class="order-manage-layout">
    <AdminSidebar
      :items="sidebarItems"
      :activeSidebar="activeSidebar"
      :collapsed="sidebarCollapsed"
      @toggle-collapse="sidebarCollapsed = !sidebarCollapsed"
      @sidebar-click="handleSidebarClick"
    />
    <div class="order-manage-root">
      <header class="order-header">
        <h2>订单管理</h2>
        <div class="order-header-actions">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="margin-right:12px;" />
          <el-select v-model="statusFilter" placeholder="订单状态" style="width:120px;margin-right:12px;">
            <el-option label="全部" value="" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已退款" value="REFUNDED" />
            <el-option label="待支付" value="PENDING_PAYMENT" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
          <el-button type="primary" @click="fetchAll">筛选</el-button>
          <el-button @click="exportOrders" style="margin-left:8px;">导出</el-button>
        </div>
      </header>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="购票订单" name="ticket">
          <el-table :data="filteredTicketOrders" style="width: 100%" stripe border>
            <el-table-column prop="id" label="订单ID" width="80" />
            <el-table-column prop="orderNo" label="订单号" width="180" />
            <el-table-column prop="userId" label="用户ID" width="80" />
            <el-table-column prop="sessionId" label="场次ID" width="80" />
            <el-table-column prop="totalAmount" label="金额" width="100" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="paymentMethod" label="支付方式" width="100" />
            <el-table-column prop="createTime" label="下单时间" width="160">
              <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button v-if="scope.row.status==='COMPLETED'" size="small" type="danger" @click="refundOrder(scope.row)">退款</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="会员订单" name="membership">
          <el-table :data="filteredMembershipOrders" style="width: 100%" stripe border>
            <el-table-column prop="id" label="订单ID" width="80" />
            <el-table-column prop="userId" label="用户ID" width="80" />
            <el-table-column prop="membershipType" label="类型" width="80" />
            <el-table-column prop="duration" label="时长" width="80" />
            <el-table-column prop="amount" label="金额" width="100" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="paymentTime" label="支付时间" width="160">
              <template #default="scope">{{ formatTime(scope.row.paymentTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button v-if="scope.row.status==='COMPLETED'" size="small" type="danger" @click="refundMembership(scope.row)">退款</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <div class="order-stats">
        <h3>订单统计</h3>
        <div>总订单数：{{ ticketOrders.length + membershipOrders.length }}</div>
        <div>购票订单：{{ ticketOrders.length }}，会员订单：{{ membershipOrders.length }}</div>
        <div>总金额：¥{{ totalAmount }}</div>
        <div>已退款订单：{{ refundedCount }}</div>
      </div>
      <div class="order-charts">
        <h3>订单趋势图</h3>
        <div id="order-chart" style="height:320px;"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import service from '../../api/request';
import AdminSidebar from '../../components/AdminSidebar.vue';
import { sidebarItems, handleAdminSidebarNav } from '@/utils/adminSidebarNav';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';

const activeSidebar = ref('订单管理');
const sidebarCollapsed = ref(false);
const router = useRouter();
const activeTab = ref('ticket');
const ticketOrders = ref([]);
const membershipOrders = ref([]);
const dateRange = ref([]);
const statusFilter = ref('');
const memberPage = ref(0);
const memberPageSize = ref(50);

const filteredTicketOrders = computed(() => {
  return ticketOrders.value.filter(o => {
    const matchStatus = !statusFilter.value || o.status === statusFilter.value;
    const matchDate = !dateRange.value.length || (
      new Date(o.createTime) >= new Date(dateRange.value[0]) && new Date(o.createTime) <= new Date(dateRange.value[1])
    );
    return matchStatus && matchDate;
  });
});
const filteredMembershipOrders = computed(() => {
  return membershipOrders.value.filter(o => {
    const matchStatus = !statusFilter.value || o.status === statusFilter.value;
    const matchDate = !dateRange.value.length || (
      new Date(o.paymentTime) >= new Date(dateRange.value[0]) && new Date(o.paymentTime) <= new Date(dateRange.value[1])
    );
    return matchStatus && matchDate;
  });
});

const totalAmount = computed(() => {
  return filteredTicketOrders.value.reduce((sum, o) => sum + (o.totalAmount || 0), 0) +
    filteredMembershipOrders.value.reduce((sum, o) => sum + (o.amount || 0), 0);
});
const refundedCount = computed(() => {
  return filteredTicketOrders.value.filter(o => o.status === 'REFUNDED').length +
    filteredMembershipOrders.value.filter(o => o.status === 'REFUNDED').length;
});

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

function handleSidebarClick(item) {
  handleAdminSidebarNav(router, item, v => activeSidebar.value = v);
}

async function fetchTicketOrders() {
  const res = await service.get('/admin/orders');
  ticketOrders.value = (res.orders || []).map(o => ({
    ...o,
    createTime: o.createTime || o.create_time,
    orderNo: o.orderNo || o.order_no,
    paymentMethod: o.paymentMethod || o.payment_method,
    userId: o.userId || o.user_id,
    sessionId: o.sessionId || o.session_id,
    totalAmount: o.totalAmount || o.total_amount,
    status: o.status
  }));
}
async function fetchMembershipOrders() {
  const res = await service.get('/api/memberships/admin/all', { params: { page: memberPage.value, size: memberPageSize.value } });
  membershipOrders.value = (res.content || []).map(o => ({
    ...o,
    id: o.id || o.orderId,
    userId: o.userId,
    amount: o.amount || o.totalAmount,
    paymentTime: o.paymentTime || o.payment_time,
    status: o.status
  }));
}
async function fetchAll() {
  await fetchTicketOrders();
  await fetchMembershipOrders();
  setTimeout(drawChart, 200); // 等待数据渲染后再画图
}

async function refundOrder(order) {
  ElMessageBox.confirm('确定要对该订单退款吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await service.post(`/admin/orders/${order.id}/refund`, { refundAmount: order.totalAmount, reason: '管理员操作' });
        ElMessage.success('退款成功');
        fetchTicketOrders();
        setTimeout(drawChart, 200);
      } catch (e) {
        ElMessage.error('退款失败');
      }
    });
}
async function refundMembership(order) {
  ElMessageBox.confirm('确定要对该会员订单退款吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await service.post(`/api/memberships/refund/${order.id}`, { refundAmount: order.amount, reason: '管理员操作' });
        ElMessage.success('退款成功');
        fetchMembershipOrders();
        setTimeout(drawChart, 200);
      } catch (e) {
        ElMessage.error('退款失败');
      }
    });
}

function exportOrders() {
  const rows = [
    ['类型','订单ID','订单号','用户ID','金额','状态','下单/支付时间'],
    ...filteredTicketOrders.value.map(o => ['购票', o.id, o.orderNo, o.userId, o.totalAmount, o.status, formatTime(o.createTime)]),
    ...filteredMembershipOrders.value.map(o => ['会员', o.id, '', o.userId, o.amount, o.status, formatTime(o.paymentTime)])
  ];
  const csv = rows.map(r => r.join(',')).join('\n');
  const blob = new Blob([csv], {type: 'text/csv'});
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'orders.csv';
  a.click();
  URL.revokeObjectURL(url);
}

function drawChart() {
  const chartDom = document.getElementById('order-chart');
  if (!chartDom) return;
  const chart = echarts.init(chartDom);
  // 统计近30天订单数和金额
  const days = Array.from({length: 30}, (_, i) => {
    const d = new Date(); d.setDate(d.getDate() - (29-i));
    return d.toISOString().slice(0,10);
  });
  const ticketData = days.map(day => filteredTicketOrders.value.filter(o => (o.createTime||'').slice(0,10) === day).length);
  const memberData = days.map(day => filteredMembershipOrders.value.filter(o => (o.paymentTime||'').slice(0,10) === day).length);
  const ticketAmount = days.map(day => filteredTicketOrders.value.filter(o => (o.createTime||'').slice(0,10) === day).reduce((sum,o)=>sum+(o.totalAmount||0),0));
  const memberAmount = days.map(day => filteredMembershipOrders.value.filter(o => (o.paymentTime||'').slice(0,10) === day).reduce((sum,o)=>sum+(o.amount||0),0));
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['购票订单数','会员订单数','购票金额','会员金额'] },
    xAxis: { type: 'category', data: days },
    yAxis: { type: 'value' },
    series: [
      { name: '购票订单数', type: 'bar', data: ticketData },
      { name: '会员订单数', type: 'bar', data: memberData },
      { name: '购票金额', type: 'line', data: ticketAmount },
      { name: '会员金额', type: 'line', data: memberAmount }
    ]
  });
}

onMounted(() => {
  fetchAll();
});
watch([filteredTicketOrders, filteredMembershipOrders], drawChart);
</script>

<style scoped>
.order-manage-layout {
  display: flex;
  min-height: 100vh;
  background: #F5F7FA;
}
.order-manage-root {
  flex: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 0 32px 32px;
}
.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.order-header-actions {
  display: flex;
  align-items: center;
}
.order-stats {
  margin-top: 32px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 18px 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.order-charts {
  margin-top: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  padding: 18px 24px;
}
</style> 