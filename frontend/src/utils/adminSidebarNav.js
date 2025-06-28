// adminSidebarNav.js
// 用于统一后台侧边栏点击跳转逻辑

export function handleAdminSidebarNav(router, item, setActiveSidebar) {
    if (setActiveSidebar) setActiveSidebar(item.label);
    if (item.label === '总览') {
        router.push('/admin/dashboard');
    } else if (item.label === '电影管理') {
        router.push('/admin/movies');
    } else if (item.label === '场次管理') {
        router.push('/admin/sessions');
    } else if (item.label === '订单管理') {
        // 可扩展
    } else if (item.label === '用户管理') {
        // 可扩展
    } else if (item.label === '系统设置') {
        // 可扩展
    } else if (item.label === '影厅管理') {
        router.push('/admin/halls');
    }
}

export const sidebarItems = [
    { label: "总览", icon: "el-icon-data-analysis" },
    { label: "电影管理", icon: "el-icon-film" },
    { label: "影厅管理", icon: "el-icon-video-camera-solid" },
    { label: "场次管理", icon: "el-icon-date" },
    { label: "订单管理", icon: "el-icon-tickets" },
    { label: "用户管理", icon: "el-icon-user" },
    { label: "系统设置", icon: "el-icon-setting" }
]; 