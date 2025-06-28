<template>
  <aside class="sidebar" :class="{collapsed}">
    <div class="sidebar-toggle" @click="$emit('toggle-collapse')">
      <i :class="collapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
    </div>
    <ul class="sidebar-menu">
      <li v-for="item in items" :key="item.label" :class="['sidebar-menu-item', {active: item.label === activeSidebar}]" @click="$emit('sidebar-click', item)">
        <i :class="item.icon"></i>
        <span v-if="!collapsed">{{ item.label }}</span>
      </li>
    </ul>
  </aside>
</template>
<script setup>
const props = defineProps({
  items: Array,
  activeSidebar: String,
  collapsed: Boolean
});
</script>
<style scoped>
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
</style> 