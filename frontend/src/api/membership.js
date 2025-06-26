import service from './request'

/**
 * 购买会员
 * @param {Object} params { userId, membershipType, duration, paymentMethod }
 * @returns Promise<{ paymentUrl: string, ... }>
 */
export function purchaseMembership(params) {
    return service.post('/api/memberships/purchase', params)
} 