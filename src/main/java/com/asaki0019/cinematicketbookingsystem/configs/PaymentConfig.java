package com.asaki0019.cinematicketbookingsystem.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付配置类
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {

    /**
     * 支付回调URL
     */
    private String callbackUrl = "http://localhost:8080/api/payments/callback";

    /**
     * 支付返回URL
     */
    private String returnUrl = "http://localhost:8080/return_url";

    /**
     * 支付宝配置
     */
    private AlipayConfig alipay = new AlipayConfig();

    /**
     * 支付宝配置内部类
     */
    public static class AlipayConfig {
        /**
         * 支付宝网关地址
         */
        private String gateway = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

        /**
         * 支付宝应用ID
         */
        private String appId = "9021000149688252";

        /**
         * 支付宝应用私钥
         */
        private String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCctgKX0oBzXcHVyrwsgru56WeJZkbi8MA+EiGrsz+5CZ6lECLi2yGmAeUB2Dxew4i9FOaSlTq6OIA0/XvFdHPPjrdAAvymja83XSSR0cr+73KATpyWYOKKni5bkC7cHN9YwOXE65nI7C7FuH9tPIKzy+T+KSxhqDuvMkUN8iueIITcNUq8k+apWPTUYHB36z6hQrusmM9pQoJOWsrrryCRKSfzOtssWAmvaWhAsv53UnT8DRBJwzFCpmmmZU26iE35xQu3IIokmjcWtDEBmbzcbQAkeV3G6VGCH7YZx4/yil/2ixa0PS7aKpjDpfO4NvzkFxzOQ1ZRP4vTuJ0l+rEDAgMBAAECggEAWgwT7V1dxezH5qs5+XGdoTRG9CK69MhWDtSoWFsFcLPgXwSopyY3bYaFKswv1FwTBGMwImZxenPg55IIPuutaESfc77Dooijd/Kjgs7EED1S0/tX6uj9A53lEWQGKSDGDd+5p6+hjFx6e86KGiC9EcdFa+4IvIzIzuDZ4SuYeKFYq3HifeEa9P4ehu39AqAdlZTk2/hhRfw0EeBv77gCf5SKr1bZADUPuRNZA2eSfC1KURJ3kKvl4MtpA36afWUK+IWfWdJDyxZnzChX6L8vXrdt0Lfn0tp2k2znNPrDw5v6m+YiR+zjeHjsAkjam+DYUnHq7yXB4aW4f4gPD6akEQKBgQDx4b+oeCn70M7brSYzL7xXjUDol3gN8W8wrccRRdv0WMYUF2+dfeUeM/ABtn4k0nGu8k0aRefmu8iWdEjKmOKS0axzbzxX5ActmNQG3R5l9fzRCbQFII6Ub9dgSHwopy7kMwAk4SrLtGPavAaTbbGNi6irmuz2/SdosKqQdS9VTQKBgQCl256xwTbwqg+S7Qhsgq3qXtYmqkgikY2AtbcoYvJAd7JeMGDLkAi8NTrRPI4wyGfxMIHBNsARbqudt2HoFQUT4yx1pFHnyoS92mHX4QY8Y3ZI9pf8dqTawJndzlgqRdNUxW10HXKmw9rcdo957aMBRtKpss8qkPdsln7BvNC3jwKBgQCOl8Hv5B8D933rHTE23b5PctNACwNYXOtqrBd9xEw9yRPEhmhjVPN8Eaw8pkPJG1KviuIPSgTDDhLbN2QuI2D2oqriRkIxohjlNYJRJYulGhXXebvphd7n/OLgPPsM0DohhztfgmpDOm3fZhcOVI1mX12pBKULmgPggL2ceajUxQKBgQCH+OTBFYXVB5Z8/ZZKX1f1LIqkaDV7IZjATDk8AuJXt8mjLkYsnIiMw5bUsrBfjeyo7vtxS1Fq+S/4vL6nZZQkGbn5OB1tybnJa5+LPA/Asknmx0MS1rGQJRX/oYrmHRiEBtcUuo+j6C0lUI2PiCJ6iXVragws+WveugJxEjqKQQKBgAnqdSG9HlCOpZy7xU0LPijSM2IUTjMZCKtGuJOlZE/SUtTFgLGX7Y/XOdDu5Pmb53wGJC6wU++Ccxd/BvwYiWhdT5GJHOE6E8Pb8LV3wctsAtafL2P7aOaiIybDu6VmamD5czRfQPClp4AYMnGx3EWTmkr1+jwTkNxI41pFhcqi";

        /**
         * 支付宝公钥
         */
        private String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoCB4TS+HUYajZ6LQorC+IIjNL8G6ovb6sWfKU/gToIVl1qII0R6yIJZJd2Tp/STfuFBvVOi/72+/xDRSQp310A9yvyI/07UkotHx8w44r+7b5AbSIxm8cbBKGeP3kLSsfxTiXWRqJuzzLrkv6xRCIO5uEc+TvKeX/Rr/kyKkoipJSGiwhYGzEZ8FGJ4IJ0gE+25zLPbzNzB879TgIm0qkQ8xAEhAVAq4GjoUTmMr7Q0nAOM7oxsxHx0Y3YdI2XNOFW/c4wswjOlcGJ146cVzwZCR2UzDg5R+mWehWQlOxDiRcZ7pF7WhLkpIS4nLtHR10yZE4W0fnk4iss9w2BfgSQIDAQAB";

        /**
         * 字符编码
         */
        private String charset = "UTF-8";

        /**
         * 数据格式
         */
        private String format = "json";

        /**
         * 签名类型
         */
        private String signType = "RSA2";

        // Getters and Setters
        public String getGateway() {
            return gateway;
        }

        public void setGateway(String gateway) {
            this.gateway = gateway;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getCharset() {
            return charset;
        }

        public void setCharset(String charset) {
            this.charset = charset;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getSignType() {
            return signType;
        }

        public void setSignType(String signType) {
            this.signType = signType;
        }
    }

    // Getters and Setters
    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public AlipayConfig getAlipay() {
        return alipay;
    }

    public void setAlipay(AlipayConfig alipay) {
        this.alipay = alipay;
    }
}