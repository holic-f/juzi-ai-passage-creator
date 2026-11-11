package com.juzi.aipassagecreator.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosConfig {

//    @Bean(destroyMethod = "shutdown")
    public COSClient cosClient() {
        String secretId = System.getenv("COS_SECRET_ID");
        String secretKey = System.getenv("COS_SECRET_KEY");

        if (secretId == null || secretId.isBlank()
                || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("请先配置 COS 访问凭证");
        }

        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);

        // 与你的存储桶地域保持一致：广州
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        clientConfig.setHttpProtocol(HttpProtocol.https);

        return new COSClient(credentials, clientConfig);
    }
}