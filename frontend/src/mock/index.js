import Mock from 'mockjs'

// 登录
Mock.mock('/api/users/login', 'post', (options) => {
    const body = JSON.parse(options.body)
    // 支持多种账号，返回不同member_level
    if (
        (body.identifier === 'admin' || body.identifier === 'admin@example.com' || body.identifier === '13800000000') &&
        body.password === '123456'
    ) {
        return {
            token: 'mock-jwt-token-123456',
            user: { id: 1, member_level: 1 }
        }
    } else if (
        (body.identifier === 'svip' || body.identifier === 'svip@example.com' || body.identifier === '13900000000') &&
        body.password === '123456'
    ) {
        return {
            token: 'mock-jwt-token-654321',
            user: { id: 2, member_level: 2 }
        }
    } else if (
        (body.identifier === 'user' || body.identifier === 'user@example.com' || body.identifier === '13700000000') &&
        body.password === '123456'
    ) {
        return {
            token: 'mock-jwt-token-000111',
            user: { id: 3, member_level: 0 }
        }
    } else {
        return {
            status: 401,
            error: '账号或密码错误'
        }
    }
})
// 注册
Mock.mock('/api/users/register', 'post', (options) => {
    const body = JSON.parse(options.body)
    if (
        body.username === 'admin' ||
        body.phone === '13800000000' ||
        body.email === 'admin@example.com'
    ) {
        return {
            status: 409,
            error: '用户名、手机号或邮箱已被注册'
        }
    } else {
        return {
            id: 2,
            create_time: new Date().toISOString()
        }
    }
}) 