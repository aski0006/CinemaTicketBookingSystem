import request from './request';

/**
 * 创建场次
 * @param {Object} data 场次数据
 */
export function createSession(data) {
    return request({
        url: '/admin/sessions',
        method: 'post',
        data
    });
}

/**
 * 更新场次
 * @param {number} sessionId 场次ID
 * @param {Object} data 场次数据
 */
export function updateSession(sessionId, data) {
    return request({
        url: `/admin/sessions/${sessionId}`,
        method: 'put',
        data
    });
}

/**
 * 删除场次
 * @param {number} sessionId 场次ID
 */
export function deleteSession(sessionId) {
    return request({
        url: `/admin/sessions/${sessionId}`,
        method: 'delete'
    });
}

/**
 * 获取指定影片的场次信息
 * @param {number} movieId - 影片ID
 */
export function getMovieSessions(movieId) {
    return request({
        url: `/api/sessions/movie/${movieId}`,
        method: 'get'
    });
}

/**
 * 获取今日场次（优先查Redis）
 * @param {number} movieId - 影片ID
 */
export function getTodaySessions(movieId) {
    return request({
        url: '/api/sessions/today',
        method: 'get',
        params: { movieId }
    });
} 