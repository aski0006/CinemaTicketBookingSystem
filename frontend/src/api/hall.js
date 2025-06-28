import request from './request';

/**
 * 获取所有影厅
 * @returns {Promise<Array>} 影厅列表
 */
export function getAllHalls() {
    return request({
        url: '/api/halls',
        method: 'get'
    });
}

/** 获取单个影厅 */
export function getHallById(id) {
    return request({ url: `/api/halls/${id}`, method: 'get' });
}

/** 创建影厅 */
export function createHall(data) {
    return request({ url: '/api/halls', method: 'post', data });
}

/** 更新影厅 */
export function updateHall(id, data) {
    return request({ url: `/api/halls/${id}`, method: 'put', data });
}

/** 删除影厅 */
export function deleteHall(id) {
    return request({ url: `/api/halls/${id}`, method: 'delete' });
} 