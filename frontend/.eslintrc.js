module.exports = {
    root: true,
    parser: 'vue-eslint-parser',
    parserOptions: {
        parser: '@babel/eslint-parser',
        ecmaVersion: 2020,
        sourceType: 'module',
    },
    extends: [
        'plugin:vue/vue3-recommended',
        'eslint:recommended',
    ],
    rules: {
        'vue/no-v-model-argument': 'off',
        // 你可以在这里添加其他自定义规则
    },
}; 