import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import MovieBrowseView from '../views/MovieBrowseView.vue';

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
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router; 