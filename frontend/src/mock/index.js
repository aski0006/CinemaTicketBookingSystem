import Mock from 'mockjs'

// 登录
Mock.mock('/api/users/login', 'post', (options) => {
    const body = JSON.parse(options.body)
    if (
        (body.identifier === 'admin' || body.identifier === 'admin@example.com' || body.identifier === '13800000000') &&
        body.password === '123456'
    ) {
        return {
            token: 'mock-jwt-token-123456',
            user: { id: 1, member_level: 1 }
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