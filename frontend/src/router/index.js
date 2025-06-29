import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import MovieBrowseView from '../views/MovieBrowseView.vue';
import TodayMoviesView from '../views/TodayMoviesView.vue';
import TicketBookingView from '../views/TicketBookingView.vue';
import AdminDashboardView from '../views/admin/AdminDashboardView.vue';
import AdminMovieManageView from '../views/admin/AdminMovieManageView.vue';
import AdminSessionManageView from '../views/admin/AdminSessionManageView.vue';
import AdminHallManageView from '../views/admin/AdminHallManageView.vue';
import UserProfileView from '../views/UserProfileView.vue';

const routes = [
    {
        path: '/',
        name: 'Home',
        component: HomeView,
    },
    {
        path: '/movies',
        name: 'MovieBrowse',
        component: MovieBrowseView,
    },
    {
        path: '/today',
        name: 'TodayMovies',
        component: TodayMoviesView,
    },
    {
        path: '/booking/:movieId',
        name: 'TicketBooking',
        component: TicketBookingView,
    },
    {
        path: '/admin/dashboard',
        name: 'AdminDashboard',
        component: AdminDashboardView,
    },
    {
        path: '/admin/movies',
        name: 'AdminMovieManage',
        component: AdminMovieManageView,
    },
    {
        path: '/admin/sessions',
        name: 'AdminSessionManage',
        component: AdminSessionManageView,
    },
    {
        path: '/admin/halls',
        name: 'AdminHallManage',
        component: AdminHallManageView,
    },
    {
        path: '/user/profile',
        name: 'UserProfile',
        component: UserProfileView,
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router; 