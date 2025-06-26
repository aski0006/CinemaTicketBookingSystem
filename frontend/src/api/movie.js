import request from './request';

/**
 * 搜索和分页获取电影列表
 * @param {object} params - 查询参数
 * @param {string} [params.keyword] - 搜索关键词
 * @param {number} [params.page] - 页码
 * @param {number} [params.size] - 每页数量
 */
export function searchMovies(params) {
    return request({
        url: '/api/movies/search',
        method: 'get',
        params
    });
} 