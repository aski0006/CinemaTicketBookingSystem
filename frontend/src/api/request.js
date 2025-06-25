import axios from 'axios';

// 创建 Axios 实例
const service = axios.create({
    baseURL: 'http://localhost:8080', // API 的 base_url
    timeout: 5000 // 请求超时时间
});

// 请求拦截器
service.interceptors.request.use(
    config => {
        // 在发送请求之前做些什么，比如添加 token
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    error => {
        // 对请求错误做些什么
        console.log(error); // for debug
        return Promise.reject(error);
    }
);

// 响应拦截器
service.interceptors.response.use(
    response => {
        // 对响应数据做点什么
        const res = response.data;
        // 如果是二进制数据（如文件下载），则直接返回
        if (response.request.responseType === 'blob' || response.request.responseType === 'arraybuffer') {
            return response;
        }
        // 这里可以根据后端的 code 进行统一的错误处理
        // if (res.code !== 200) { ... }
        return res;
    },
    error => {
        console.log('err' + error); // for debug
        // 可以在这里统一处理网络错误，比如弹出一个 Message 提示
        return Promise.reject(error);
    }
);

export default service; 