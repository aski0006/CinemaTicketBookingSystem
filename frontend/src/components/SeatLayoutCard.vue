<template>
  <div class="seat-layout-card">
    <div class="seat-map">
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
            <div
              :class="seatClass(seat, rowIndex, colIndex)"
              @click="handleSeatClick(rowIndex, colIndex, seat)"
            >
              <span v-if="isSelected(rowIndex, colIndex)">✔</span>
            </div>
          </template>
          <template v-else>
            <div class="seat-null"></div>
          </template>
        </div>
      </div>
    </div>
    <div class="seat-legend">
      <div class="legend-item"><span class="seat seat-available"></span>可选</div>
      <div class="legend-item"><span class="seat seat-selected"></span>已选</div>
      <div class="legend-item"><span class="seat seat-occupied"></span>已售</div>
      <div class="legend-item"><span class="seat seat-vip"></span>VIP</div>
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

watch(
  () => props.selectedSeats,
  (val) => {
    selectedSeats.value = [...val];
  },
  { immediate: true }
);

function isSelected(row, col) {
  return selectedSeats.value.some(([r, c]) => r === row && c === col);
}

function isOccupied(seat) {
  return seat && (seat.status === 'OCCUPIED' || seat.status === 'LOCKED');
}

function seatClass(seat, row, col) {
  let cls = ['seat'];
  if (seat && seat.type === 'VIP') cls.push('seat-vip');
  if (isOccupied(seat)) cls.push('seat-occupied');
  else if (isSelected(row, col)) cls.push('seat-selected');
  else if (seat && seat.type !== 'NULL') cls.push('seat-available');
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
  padding: 20px 0;
}
.seat-map {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}
.seat-row {
  display: flex;
}
.seat-cell {
  width: 36px;
  height: 36px;
  margin: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.seat {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: 2px solid #d9d9d9;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s;
}
.seat-available {
  border-color: #1890ff;
  background: #e6f7ff;
}
.seat-selected {
  border-color: #52c41a;
  background: #f6ffed;
  color: #52c41a;
}
.seat-occupied {
  border-color: #aaa;
  background: #eee;
  color: #aaa;
  cursor: not-allowed;
}
.seat-vip {
  border-color: #faad14;
  background: #fffbe6;
}
.seat-null {
  width: 32px;
  height: 32px;
  margin: 4px;
  background: transparent;
}
.seat-legend {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  justify-content: center;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}
.seat-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
}
</style> 