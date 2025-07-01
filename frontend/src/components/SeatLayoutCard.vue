<template>
  <div class="seat-layout-card" :style="cardStyle">
    <div class="seat-map" :style="mapStyle">
      <div
        v-for="(row, rowIndex) in seatStatus"
        :key="rowIndex"
        class="seat-row"
      >
        <div
          v-for="(seat, colIndex) in row"
          :key="colIndex"
          class="seat-cell"
        >
          <template v-if="seat && seat.type !== 'NULL'">
            <img
              :src="seatIcon(seat, rowIndex, colIndex)"
              :class="seatClass(seat, rowIndex, colIndex)"
              @click="handleSeatClick(rowIndex, colIndex, seat)"
              :alt="seat.type"
              draggable="false"
            />
          </template>
          <template v-else>
            <div class="seat-null"></div>
          </template>
        </div>
      </div>
    </div>
    <div class="seat-actions">
      <el-button type="primary" @click="confirmSelect" :disabled="selectedSeats.length === 0">确认选座</el-button>
      <el-button @click="$emit('close')">取消</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
const props = defineProps({
  seatStatus: {
    type: Array,
    required: true
  },
  selectedSeats: {
    type: Array,
    default: () => []
  }
});
const emit = defineEmits(['select', 'confirm', 'close']);
const selectedSeats = ref([]);

const ICONS = {
  NORMAL: '/单人座位.svg',
  NORMAL_SELECTED: '/单人座位——选座位.svg',
  VIP: '/SVIP座位.svg',
  VIP_SELECTED: '/SVIP选座座位-copy.svg',
  DISABLED: '/不可选座位.svg',
};

watch(
  () => props.selectedSeats,
  (val) => {
    selectedSeats.value = [...val];
  },
  { immediate: true }
);

const maxCols = computed(() => {
  return Math.max(...(props.seatStatus || []).map(row => row.length || 0));
});
const cardStyle = computed(() => ({
  minWidth: `${maxCols.value * 60 + 64}px`,
  maxWidth: `${maxCols.value * 60 + 64}px`,
  minHeight: '520px',
  background: '#fff',
  borderRadius: '14px',
  boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
}));
const mapStyle = computed(() => ({
  width: `${maxCols.value * 60}px`,
  overflow: 'auto',
  margin: '32px 0',
}));

function isSelected(row, col) {
  return selectedSeats.value.some(([r, c]) => r === row && c === col);
}

function isOccupied(seat) {
  return seat && (seat.status === 'OCCUPIED' || seat.status === 'LOCKED');
}

function seatIcon(seat, row, col) {
  if (isOccupied(seat)) return ICONS.DISABLED;
  if (isSelected(row, col)) {
    if (seat.type === 'VIP') return ICONS.VIP_SELECTED;
    if (seat.type === 'SVIP') return ICONS.SVIP_SELECTED;
    return ICONS.NORMAL_SELECTED;
  }
  if (seat.type === 'VIP') return ICONS.VIP;
  if (seat.type === 'SVIP') return ICONS.SVIP;
  return ICONS.NORMAL;
}

function seatClass(seat, row, col) {
  let cls = ['seat-img'];
  if (isOccupied(seat)) cls.push('seat-occupied');
  else if (isSelected(row, col)) cls.push('seat-selected');
  else if (seat && seat.type !== 'NULL') cls.push('seat-available');
  if (seat && seat.type === 'VIP') cls.push('seat-vip');
  if (seat && seat.type === 'SVIP') cls.push('seat-svip');
  return cls.join(' ');
}

function handleSeatClick(row, col, seat) {
  if (!seat || seat.type === 'NULL' || isOccupied(seat)) return;
  const idx = selectedSeats.value.findIndex(([r, c]) => r === row && c === col);
  if (idx !== -1) {
    selectedSeats.value.splice(idx, 1);
  } else {
    selectedSeats.value.push([row, col]);
  }
  emit('select', [...selectedSeats.value]);
}

function confirmSelect() {
  emit('confirm', [...selectedSeats.value]);
}
</script>

<style scoped>
.seat-layout-card {
  /* 样式由cardStyle动态控制 */
}
.seat-map {
  /* 样式由mapStyle动态控制 */
  display: flex;
  flex-direction: column;
  align-items: center;
}
.seat-row {
  display: flex;
}
.seat-cell {
  width: 60px;
  height: 60px;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.seat-img {
  width: 48px;
  height: 48px;
  user-select: none;
  transition: transform 0.15s;
}
.seat-img.seat-selected {
  transform: scale(1.08);
  z-index: 2;
}
.seat-img.seat-occupied {
  opacity: 0.5;
  cursor: not-allowed;
}
.seat-null {
  width: 48px;
  height: 48px;
  background: transparent;
}
.seat-actions {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 32px;
}
</style> 